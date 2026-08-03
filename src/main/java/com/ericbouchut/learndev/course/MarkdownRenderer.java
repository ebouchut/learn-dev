package com.ericbouchut.learndev.course;

import com.ericbouchut.learndev.common.config.CacheConfig;
import org.commonmark.Extension;
import org.commonmark.ext.gfm.alerts.AlertsExtension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.Heading;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.NodeRenderer;
import org.commonmark.renderer.html.HtmlNodeRendererContext;
import org.commonmark.renderer.html.HtmlRenderer;
import org.commonmark.renderer.html.HtmlWriter;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Safelist;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    // (```java fences) available to a future syntax highlighter. The div/p
    // class and data-alert-type attributes exist only for alerts, and
    // stripUnknownAlertClasses() rejects every value the alert renderer
    // does not emit, so raw HTML in a lesson cannot borrow site classes.
    private static final Safelist SAFELIST = Safelist.relaxed()
            .addAttributes("code", "class")
            .addAttributes("div", "class", "data-alert-type")
            .addAttributes("p", "class");

    private static final Pattern ALERT_DIV_CLASS =
            Pattern.compile("^markdown-alert markdown-alert-[a-z]+$");
    private static final Pattern ALERT_TYPE_VALUE = Pattern.compile("^[a-z]+$");

    // A level-1 ATX heading per CommonMark: up to 3 leading spaces, one #,
    // whitespace, the text, then an optional closing run of #s that only
    // counts when preceded by whitespace.
    private static final Pattern LEADING_ATX_TITLE =
            Pattern.compile("^ {0,3}#\\s+(.*?)(?:\\s+#+)?\\s*$");
    private static final Pattern SETEXT_H1_UNDERLINE =
            Pattern.compile("^ {0,3}=+\\s*$");

    /**
     * Alert types accepted in lessons: the five GFM types keep their GitHub
     * identity (IMPORTANT and CAUTION stay standalone), and the rest of the
     * Obsidian callout set joins them, aliases included, each with its
     * default title.
     */
    private static final Map<String, String> ALERT_TYPES = Map.ofEntries(
            Map.entry("NOTE", "Note"),
            Map.entry("TIP", "Tip"),
            Map.entry("IMPORTANT", "Important"),
            Map.entry("WARNING", "Warning"),
            Map.entry("CAUTION", "Caution"),
            Map.entry("ABSTRACT", "Abstract"),
            Map.entry("SUMMARY", "Summary"),
            Map.entry("TLDR", "TL;DR"),
            Map.entry("INFO", "Info"),
            Map.entry("TODO", "Todo"),
            Map.entry("HINT", "Hint"),
            Map.entry("SUCCESS", "Success"),
            Map.entry("CHECK", "Check"),
            Map.entry("DONE", "Done"),
            Map.entry("QUESTION", "Question"),
            Map.entry("HELP", "Help"),
            Map.entry("FAQ", "FAQ"),
            Map.entry("ATTENTION", "Attention"),
            Map.entry("FAILURE", "Failure"),
            Map.entry("FAIL", "Fail"),
            Map.entry("MISSING", "Missing"),
            Map.entry("DANGER", "Danger"),
            Map.entry("ERROR", "Error"),
            Map.entry("BUG", "Bug"),
            Map.entry("EXAMPLE", "Example"),
            Map.entry("QUOTE", "Quote"),
            Map.entry("CITE", "Cite"));

    // GFM pipe tables and alerts, added on demand as ADR-0013 planned; the
    // sanitizer allowlist lets table markup through (Safelist.relaxed) and
    // is extended above for the alert markup.
    private static final List<Extension> EXTENSIONS = List.of(
            TablesExtension.create(),
            AlertsExtension.builder()
                    .setAllowedTypes(ALERT_TYPES)
                    .allowCustomTitles(true)
                    .allowNestedAlerts(true)
                    .build());

    private final Parser parser = Parser.builder()
            .extensions(EXTENSIONS)
            .build();
    private final HtmlRenderer renderer = HtmlRenderer.builder()
            .extensions(EXTENSIONS)
            .nodeRendererFactory(DemotedHeadingRenderer::new)
            .build();

    /** Sanitized lesson HTML plus its table of contents (ADR-0018). */
    public record RenderedMarkdown(String html, List<TocEntry> toc) { }

    /** One TOC entry: minted anchor id, heading text, rendered level. */
    public record TocEntry(String id, String text, int level) { }

    @Cacheable(cacheNames = CacheConfig.RENDERED_MARKDOWN_CACHE,
            key = "T(com.ericbouchut.learndev.course.MarkdownRenderer).cacheKey(#markdown)")
    public RenderedMarkdown render(String markdown) {
        if (markdown == null || markdown.isBlank()) {
            return new RenderedMarkdown("", List.of());
        }
        String html = renderer.render(parser.parse(markdown));
        return postProcess(Jsoup.clean(html, SAFELIST));
    }

    /**
     * Post-sanitization pass, one parse for both jobs: narrow the class
     * allowlist to the exact alert values, then mint heading anchors and
     * collect the table of contents. Ids are stamped here, AFTER the
     * sanitizer stripped any author-supplied id, so raw HTML can never
     * clobber page anchors like the {@code #main} skip-link target
     * (ADR-0018); the allowlist itself stays id-free.
     */
    private static RenderedMarkdown postProcess(String html) {
        Document doc = Jsoup.parseBodyFragment(html);
        doc.outputSettings().prettyPrint(false);
        stripUnknownAlertClasses(doc);
        List<TocEntry> toc = mintHeadingAnchors(doc);
        return new RenderedMarkdown(doc.body().html(), List.copyOf(toc));
    }

    /**
     * Second sanitization pass: the allowlist admits {@code class} on
     * {@code div}/{@code p} so alerts stay styleable, but only the exact
     * values the alert renderer emits may survive. Anything else (for
     * example raw HTML trying to wear a site class like {@code alert} or
     * {@code site-header}) is stripped.
     */
    private static void stripUnknownAlertClasses(Document doc) {
        for (Element div : doc.select("div[class], div[data-alert-type]")) {
            if (!ALERT_DIV_CLASS.matcher(div.className()).matches()) {
                div.removeAttr("class");
            }
            if (!ALERT_TYPE_VALUE.matcher(div.attr("data-alert-type")).matches()) {
                div.removeAttr("data-alert-type");
            }
        }
        for (Element p : doc.select("p[class]")) {
            if (!"markdown-alert-title".equals(p.className())) {
                p.removeAttr("class");
            }
        }
    }

    /**
     * Stamps a slug id on every rendered {@code h2..h4} and returns the
     * matching TOC entries. Slugs fold accents, lowercase, hyphenate, and
     * deduplicate with numeric suffixes so anchors stay unique within a
     * lesson.
     */
    private static List<TocEntry> mintHeadingAnchors(Document doc) {
        List<TocEntry> toc = new ArrayList<>();
        Map<String, Integer> seen = new HashMap<>();
        for (Element heading : doc.select("h2, h3, h4")) {
            String text = heading.text();
            String slug = slugify(text);
            int count = seen.merge(slug, 1, Integer::sum);
            String id = count == 1 ? slug : slug + "-" + count;
            heading.attr("id", id);
            toc.add(new TocEntry(id, text, heading.tagName().charAt(1) - '0'));
        }
        return toc;
    }

    private static String slugify(String text) {
        String folded = Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        String slug = folded.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+)|(-+$)", "");
        return slug.isEmpty() ? "section" : slug;
    }

    /**
     * Drops a leading level-1 heading whose text repeats the lesson title,
     * so the common habit of starting a document with its title does not
     * render as an {@code h2} duplicating the page {@code h1} (see
     * ADR-0014). Handles the ATX form ({@code # Title}, closing {@code #}s
     * tolerated) and the setext form ({@code Title} underlined with
     * {@code =}); the match is trimmed and case-insensitive. Callers apply
     * this BEFORE {@link #render(String)} so the render cache stays keyed
     * by the exact rendered input; the stored Markdown is never modified.
     */
    public static String stripLeadingTitleHeading(String markdown, String title) {
        if (markdown == null || title == null || title.isBlank()) {
            return markdown;
        }
        String[] lines = markdown.split("\n", -1);
        int first = 0;
        while (first < lines.length && lines[first].isBlank()) {
            first++;
        }
        if (first == lines.length) {
            return markdown;
        }
        String wanted = title.strip();
        Matcher atx = LEADING_ATX_TITLE.matcher(lines[first]);
        if (atx.matches() && atx.group(1).strip().equalsIgnoreCase(wanted)) {
            return joinFrom(lines, first + 1);
        }
        if (first + 1 < lines.length
                && lines[first].strip().equalsIgnoreCase(wanted)
                && SETEXT_H1_UNDERLINE.matcher(lines[first + 1]).matches()) {
            return joinFrom(lines, first + 2);
        }
        return markdown;
    }

    private static String joinFrom(String[] lines, int from) {
        return String.join("\n", Arrays.asList(lines).subList(from, lines.length));
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
