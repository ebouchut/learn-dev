package com.ericbouchut.learndev.course;

import com.ericbouchut.learndev.common.config.CacheConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Boots a minimal context (just the cache config and the renderer, no
 * database) so {@code @Cacheable} runs through the real Spring proxy.
 */
@SpringBootTest(classes = {CacheConfig.class, MarkdownRenderer.class})
class MarkdownRendererTest {

    @Autowired
    MarkdownRenderer markdownRenderer;

    @Test
    void renders_markdown_constructs_to_html() {
        // Arrange (Given): a lesson snippet with a heading, emphasis, and code
        String markdown = "# Indexes\n\nSome **bold** text.\n\n```java\nint x = 1;\n```";

        // Act (When)
        String html = markdownRenderer.render(markdown);

        // Assert (Then): structural HTML with the language hint preserved
        assertThat(html).contains("<h1>Indexes</h1>");
        assertThat(html).contains("<strong>bold</strong>");
        assertThat(html).contains("<code class=\"language-java\">");
    }

    @Test
    void strips_scripts_and_event_handlers_from_hostile_markdown() {
        // Arrange (Given): raw HTML in Markdown passes through CommonMark,
        // so only the sanitizer stands between this and the browser
        String hostile = "Hello <script>alert('xss')</script>\n\n"
                + "<img src=\"x\" onerror=\"alert(1)\">\n\n"
                + "<a href=\"javascript:alert(2)\">click</a>";

        // Act (When)
        String html = markdownRenderer.render(hostile);

        // Assert (Then): nothing executable survives the allowlist
        assertThat(html).doesNotContain("<script");
        assertThat(html).doesNotContain("onerror");
        assertThat(html).doesNotContain("javascript:");
        assertThat(html).contains("Hello");
    }

    @Test
    void same_content_is_rendered_once_and_served_from_the_cache() {
        // Arrange (Given): one Markdown source rendered twice
        String markdown = "Cache **me** once.";

        // Act (When)
        String first = markdownRenderer.render(markdown);
        String second = markdownRenderer.render(markdown);

        // Assert (Then): the second call returns the cached instance itself,
        // proving the renderer did not run again
        assertThat(second).isSameAs(first);
        assertThat(markdownRenderer.render("Different content.")).isNotSameAs(first);
    }

    @Test
    void blank_content_renders_to_an_empty_string() {
        assertThat(markdownRenderer.render("")).isEmpty();
        assertThat(markdownRenderer.render("   ")).isEmpty();
    }
}
