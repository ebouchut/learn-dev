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
        // (the heading is demoted one level, see the renderer Javadoc)
        assertThat(html).contains("<h2>Indexes</h2>");
        assertThat(html).contains("<strong>bold</strong>");
        assertThat(html).contains("<code class=\"language-java\">");
    }

    @Test
    void demotes_headings_one_level_capped_at_h6() {
        // Arrange (Given): authored levels 1 through 6; the lesson page owns
        // the only h1 (RGAA 9.1), so authored levels must shift down
        String markdown = "# One\n\n## Two\n\n##### Five\n\n###### Six";

        // Act (When)
        String html = markdownRenderer.render(markdown);

        // Assert (Then): every level is one deeper, h6 stays h6
        assertThat(html).contains("<h2>One</h2>");
        assertThat(html).contains("<h3>Two</h3>");
        assertThat(html).contains("<h6>Five</h6>");
        assertThat(html).contains("<h6>Six</h6>");
        assertThat(html).doesNotContain("<h1");
        assertThat(html).doesNotContain("<h7");
    }

    @Test
    void demoted_headings_keep_their_inline_formatting() {
        // Arrange (Given): a heading with inline emphasis and code
        String markdown = "# The `for` loop is **great**";

        // Act (When)
        String html = markdownRenderer.render(markdown);

        // Assert (Then): children render inside the demoted tag
        assertThat(html).contains(
                "<h2>The <code>for</code> loop is <strong>great</strong></h2>");
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
