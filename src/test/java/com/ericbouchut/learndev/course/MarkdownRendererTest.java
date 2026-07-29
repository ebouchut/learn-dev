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
    void renders_gfm_pipe_tables_as_html_tables() {
        // Arrange (Given): a GFM pipe table (extension, not core CommonMark)
        String markdown = "| Concept | Example |\n|---------|---------|\n| Array | int[] |";

        // Act (When)
        String html = markdownRenderer.render(markdown);

        // Assert (Then): a real table survives rendering and sanitization
        assertThat(html).contains("<table>");
        assertThat(html).contains("<th>Concept</th>");
        assertThat(html).contains("<td>Array</td>");
    }

    @Test
    void renders_gfm_alerts_with_type_class_and_default_title() {
        // Arrange (Given): a GFM alert, uppercase marker as GitHub writes it
        String markdown = "> [!NOTE]\n> Take notes.";

        // Act (When)
        String html = markdownRenderer.render(markdown);

        // Assert (Then): the typed div and the default title survive
        // rendering and both sanitization passes
        assertThat(html).contains("markdown-alert markdown-alert-note");
        assertThat(html).contains("<p class=\"markdown-alert-title\">Note</p>");
        assertThat(html).contains("Take notes.");
    }

    @Test
    void renders_obsidian_callout_types_and_aliases_case_insensitively() {
        // Arrange (Given): an Obsidian-only type and a lowercase alias whose
        // default title is not just the capitalized marker
        String markdown = "> [!bug]\n> Off by one.\n\n> [!tldr]\n> Short version.";

        // Act (When)
        String html = markdownRenderer.render(markdown);

        // Assert (Then): both resolve to their registered type and title
        assertThat(html).contains("markdown-alert markdown-alert-bug");
        assertThat(html).contains("<p class=\"markdown-alert-title\">Bug</p>");
        assertThat(html).contains("markdown-alert markdown-alert-tldr");
        assertThat(html).contains("<p class=\"markdown-alert-title\">TL;DR</p>");
    }

    @Test
    void renders_custom_alert_titles_with_inline_formatting() {
        // Arrange (Given): a title after the marker (allowCustomTitles)
        String markdown = "> [!tip] Use **bold** wisely\n> Body text.";

        // Act (When)
        String html = markdownRenderer.render(markdown);

        // Assert (Then): the custom title replaces the default and its
        // inline Markdown is rendered
        assertThat(html).contains("markdown-alert markdown-alert-tip");
        assertThat(html).contains("Use <strong>bold</strong> wisely");
        assertThat(html).doesNotContain(">Tip</p>");
    }

    @Test
    void renders_alerts_nested_inside_alerts() {
        // Arrange (Given): a warning inside a note (allowNestedAlerts);
        // without nesting the inner marker would degrade to literal text
        String markdown = "> [!note]\n> Outer.\n> > [!warning]\n> > Inner.";

        // Act (When)
        String html = markdownRenderer.render(markdown);

        // Assert (Then): both alerts materialize as typed divs
        assertThat(html).contains("markdown-alert markdown-alert-note");
        assertThat(html).contains("markdown-alert markdown-alert-warning");
        assertThat(html).doesNotContain("[!warning]");
    }

    @Test
    void strips_class_spoofing_while_alert_classes_survive() {
        // Arrange (Given): raw HTML trying to wear site classes next to a
        // genuine alert; the allowlist admits class on div/p, so only the
        // second sanitization pass stands between this and the stylesheet
        String markdown = "<div class=\"site-header\">spoof</div>\n\n"
                + "<p class=\"alert alert--error\">spoof</p>\n\n"
                + "<div data-alert-type=\"NOT_A_TYPE\">spoof</div>\n\n"
                + "> [!note]\n> Legit.";

        // Act (When)
        String html = markdownRenderer.render(markdown);

        // Assert (Then): spoofed values are stripped, alert markup is kept
        assertThat(html).doesNotContain("site-header");
        assertThat(html).doesNotContain("alert--error");
        assertThat(html).doesNotContain("NOT_A_TYPE");
        assertThat(html).contains("markdown-alert markdown-alert-note");
        assertThat(html).contains("<p class=\"markdown-alert-title\">Note</p>");
    }

    @Test
    void preserves_mermaid_fences_for_client_side_rendering() {
        // Arrange (Given): a mermaid fence; the server never renders
        // diagrams, the browser does (ADR-0016), so the language class and
        // the source must survive sanitization and no SVG may appear here
        String markdown = "```mermaid\nflowchart LR\n  accTitle: Flow\n  A --> B\n```";

        // Act (When)
        String html = markdownRenderer.render(markdown);

        // Assert (Then): the client-side contract holds
        assertThat(html).contains("<code class=\"language-mermaid\">");
        assertThat(html).contains("flowchart LR");
        assertThat(html).contains("accTitle: Flow");
        assertThat(html).doesNotContain("<svg");
    }

    @Test
    void blank_content_renders_to_an_empty_string() {
        assertThat(markdownRenderer.render("")).isEmpty();
        assertThat(markdownRenderer.render("   ")).isEmpty();
    }

    @Test
    void strips_a_leading_atx_heading_matching_the_lesson_title() {
        // Arrange (Given): the common habit of repeating the title on top,
        // with leading blank lines, odd spacing, and a closing hash run
        String markdown = "\n\n#  Java Basics  ##\n\nFirst paragraph.";

        // Act (When)
        String stripped = MarkdownRenderer.stripLeadingTitleHeading(markdown, "Java Basics");

        // Assert (Then): only the duplicate heading is gone
        assertThat(stripped).doesNotContain("Java Basics");
        assertThat(stripped).contains("First paragraph.");
    }

    @Test
    void strips_a_matching_title_heading_case_insensitively() {
        String markdown = "# JAVA BASICS\nBody.";

        String stripped = MarkdownRenderer.stripLeadingTitleHeading(markdown, "Java Basics");

        assertThat(stripped).isEqualTo("Body.");
    }

    @Test
    void strips_a_leading_setext_heading_matching_the_lesson_title() {
        // Arrange (Given): the setext form, title underlined with equals
        String markdown = "Java Basics\n====\n\nBody.";

        // Act (When)
        String stripped = MarkdownRenderer.stripLeadingTitleHeading(markdown, "Java Basics");

        // Assert (Then)
        assertThat(stripped).isEqualTo("\nBody.");
    }

    @Test
    void keeps_a_leading_heading_with_a_different_text() {
        // Arrange (Given): a real first heading, not a title duplicate
        String markdown = "# Introduction\nBody.";

        // Act (When)
        String stripped = MarkdownRenderer.stripLeadingTitleHeading(markdown, "Java Basics");

        // Assert (Then): untouched, the author meant this heading
        assertThat(stripped).isEqualTo(markdown);
    }

    @Test
    void keeps_deeper_headings_even_when_they_match_the_title() {
        // Arrange (Given): only a level-1 heading duplicates the page h1
        String markdown = "## Java Basics\nBody.";

        // Act (When)
        String stripped = MarkdownRenderer.stripLeadingTitleHeading(markdown, "Java Basics");

        // Assert (Then)
        assertThat(stripped).isEqualTo(markdown);
    }

    @Test
    void strips_the_title_heading_despite_windows_line_endings() {
        // Arrange (Given): textarea submissions carry \r\n line endings,
        // so this is what stored lesson content actually looks like
        String markdown = "# Java Basics\r\n\r\nBody.";

        // Act (When)
        String stripped = MarkdownRenderer.stripLeadingTitleHeading(markdown, "Java Basics");

        // Assert (Then): the heading goes, the \r\n rhythm stays
        assertThat(stripped).isEqualTo("\r\nBody.");
    }

    @Test
    void title_stripping_tolerates_null_and_blank_input() {
        assertThat(MarkdownRenderer.stripLeadingTitleHeading(null, "T")).isNull();
        assertThat(MarkdownRenderer.stripLeadingTitleHeading("# T\nBody.", null))
                .isEqualTo("# T\nBody.");
        assertThat(MarkdownRenderer.stripLeadingTitleHeading("# T\nBody.", "  "))
                .isEqualTo("# T\nBody.");
    }
}
