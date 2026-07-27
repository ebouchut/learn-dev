package com.ericbouchut.learndev.course;

import com.ericbouchut.learndev.common.config.CacheConfig;
import org.commonmark.Extension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.Heading;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.NodeRenderer;
import org.commonmark.renderer.html.HtmlNodeRendererContext;
import org.commonmark.renderer.html.HtmlRenderer;
import org.commonmark.renderer.html.HtmlWriter;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Converts lesson Markdown to HTML that is safe to serve.
 * <p>Lesson content is instructor input and CommonMark passes raw HTML
 * through, so the rendered output is sanitized against an allowlist: no
 * script, event handler, or frame survives, whatever the Markdown contains.
 * Results are cached by the SHA-256 of the source, so a lesson is rendered
 * once per content version (see ADR-0013 in docs/adr/).
 * <p>Markdown headings are demoted one level ({@code #} becomes {@code h2},
 * capped at {@code h6}): the lesson page already provides the single
 * {@code h1} (the lesson title), and one {@code h1} per page with no skipped
 * level is an RGAA 9.1 commitment (see ADR-0014 and docs/rgaa.md).
 */
@Service
public class MarkdownRenderer {

    // The class attribute on code keeps CommonMark's language-* hints
    // (```java fences) available to a future syntax highlighter.
    private static final Safelist SAFELIST = Safelist.relaxed()
            .addAttributes("code", "class");

    // GFM pipe tables, added on demand as ADR-0013 planned; the sanitizer
    // allowlist already lets table markup through (Safelist.relaxed).
    private static final List<Extension> EXTENSIONS = List.of(TablesExtension.create());

    private final Parser parser = Parser.builder()
            .extensions(EXTENSIONS)
            .build();
    private final HtmlRenderer renderer = HtmlRenderer.builder()
            .extensions(EXTENSIONS)
            .nodeRendererFactory(DemotedHeadingRenderer::new)
            .build();

    @Cacheable(cacheNames = CacheConfig.RENDERED_MARKDOWN_CACHE,
            key = "T(com.ericbouchut.learndev.course.MarkdownRenderer).cacheKey(#markdown)")
    public String render(String markdown) {
        if (markdown == null || markdown.isBlank()) {
            return "";
        }
        String html = renderer.render(parser.parse(markdown));
        return Jsoup.clean(html, SAFELIST);
    }

    /**
     * Cache key: SHA-256 of the source. Content-addressed, so edited content
     * is a new key and stale entries cannot exist; the fixed-size key also
     * avoids holding whole lesson sources in the cache's key set.
     */
    public static String cacheKey(String markdown) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(
                    (markdown == null ? "" : markdown).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    /**
     * Renders Markdown headings one HTML level lower than authored, capped
     * at {@code h6}, so instructor content slots under the page's own
     * {@code h1} without breaking the heading hierarchy.
     */
    private static final class DemotedHeadingRenderer implements NodeRenderer {

        private final HtmlNodeRendererContext context;
        private final HtmlWriter html;

        private DemotedHeadingRenderer(HtmlNodeRendererContext context) {
            this.context = context;
            this.html = context.getWriter();
        }

        @Override
        public Set<Class<? extends Node>> getNodeTypes() {
            return Set.of(Heading.class);
        }

        @Override
        public void render(Node node) {
            Heading heading = (Heading) node;
            String tag = "h" + Math.min(heading.getLevel() + 1, 6);
            html.line();
            html.tag(tag, context.extendAttributes(heading, tag, Map.of()));
            for (Node child = heading.getFirstChild(); child != null; child = child.getNext()) {
                context.render(child);
            }
            html.tag("/" + tag);
            html.line();
        }
    }
}
