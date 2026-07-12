package com.ericbouchut.learndev.course;

import com.ericbouchut.learndev.common.config.CacheConfig;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Converts lesson Markdown to HTML that is safe to serve.
 * <p>Lesson content is instructor input and CommonMark passes raw HTML
 * through, so the rendered output is sanitized against an allowlist: no
 * script, event handler, or frame survives, whatever the Markdown contains.
 * Results are cached by the SHA-256 of the source, so a lesson is rendered
 * once per content version (see ADR-0013 in docs/adr/).
 */
@Service
public class MarkdownRenderer {

    // The class attribute on code keeps CommonMark's language-* hints
    // (```java fences) available to a future syntax highlighter.
    private static final Safelist SAFELIST = Safelist.relaxed()
            .addAttributes("code", "class");

    private final Parser parser = Parser.builder().build();
    private final HtmlRenderer renderer = HtmlRenderer.builder().build();

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
}
