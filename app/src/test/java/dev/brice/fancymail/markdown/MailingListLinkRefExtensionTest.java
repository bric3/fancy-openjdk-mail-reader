/*
 * fancy-mail-openjdk-reader
 *
 * Copyright (c) 2026 - Brice Dutheil
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package dev.brice.fancymail.markdown;

import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the MailingListLinkRefExtension Flexmark extension.
 */
class MailingListLinkRefExtensionTest {

    static {
        // Enable DEBUG logging for tests
        System.setProperty("org.slf4j.simpleLogger.log.dev.brice.fancymail.markdown", "DEBUG");
    }

    private Parser parser;
    private HtmlRenderer renderer;

    @BeforeEach
    void setUp() {
        MutableDataSet options = new MutableDataSet();
        options.set(Parser.EXTENSIONS, List.of(MailingListLinkRefExtension.create()));

        parser = Parser.builder(options).build();
        renderer = HtmlRenderer.builder(options).build();
    }

    private String render(String markdown) {
        Node document = parser.parse(markdown);
        return renderer.render(document);
    }

    @Nested
    class NoReferencesTest {

        @Test
        void render_withNoReferences_returnsNormalHtml() {
            String markdown = "This is some text without any references.";

            String html = render(markdown);

            assertThat(html).contains("<p>This is some text without any references.</p>");
            assertThat(html).doesNotContain("link-references");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\n\n"})
        void render_withBlankInput_returnsEmpty(String input) {
            String html = render(input != null ? input : "");
            // Empty or whitespace input should not crash
            assertThat(html).isNotNull();
        }
    }

    @Nested
    class SingleReferenceTest {

        @Test
        void render_withSingleReference_createsSuperscriptLink() {
            String markdown = """
                    See the docs[1].

                    [1] https://example.com/docs
                    """;

            String html = render(markdown);
            System.out.println("=== HTML OUTPUT ===");
            System.out.println(html);
            System.out.println("===================");

            assertThat(html)
                    .contains("<sup><a href=\"https://example.com/docs\">[1]</a></sup>")
                    .contains("<div class=\"link-references\">")
                    .contains("<li><a href=\"https://example.com/docs\">https://example.com/docs</a></li>");
        }

        @Test
        void render_referenceDefinitionIsRemoved() {
            String markdown = """
                    Text[1].

                    [1] https://example.com
                    """;

            String html = render(markdown);

            // The definition line should not appear as a paragraph
            assertThat(html).doesNotContain("<p>[1] https://example.com</p>");
        }
    }

    @Nested
    class MultipleReferencesTest {

        @Test
        void render_withMultipleReferences_replacesAll() {
            String markdown = """
                    See docs[1] and example[2].

                    [1] https://example.com/docs
                    [2] https://example.com/example
                    """;

            String html = render(markdown);

            assertThat(html)
                    .contains("<sup><a href=\"https://example.com/docs\">[1]</a></sup>")
                    .contains("<sup><a href=\"https://example.com/example\">[2]</a></sup>");
        }

        @Test
        void render_footnotesSectionSortedByNumber() {
            String markdown = """
                    See [3] and [1] and [2].

                    [3] https://three.com
                    [1] https://one.com
                    [2] https://two.com
                    """;

            String html = render(markdown);

            // References should be sorted in the footnotes section
            int indexOne = html.indexOf("https://one.com</a></li>");
            int indexTwo = html.indexOf("https://two.com</a></li>");
            int indexThree = html.indexOf("https://three.com</a></li>");

            assertThat(indexOne).isLessThan(indexTwo);
            assertThat(indexTwo).isLessThan(indexThree);
        }

        @Test
        void render_withBlankLinesBetweenDefinitions_handlesCorrectly() {
            // Note: Using "Text[1] and [2]." instead of "Text[1][2]." because
            // [1][2] is valid Markdown reference link syntax and would be parsed differently
            String markdown = """
                    Text[1] and [2].

                    [1] https://one.com

                    [2] https://two.com
                    """;

            String html = render(markdown);

            assertThat(html)
                    .contains("<sup><a href=\"https://one.com\">[1]</a></sup>")
                    .contains("<sup><a href=\"https://two.com\">[2]</a></sup>");
        }
    }

    @Nested
    class UndefinedReferencesTest {

        @Test
        void render_withUndefinedReference_keepsOriginal() {
            String markdown = """
                    See [1] and [99].

                    [1] https://example.com
                    """;

            String html = render(markdown);

            assertThat(html)
                    .contains("<sup><a href=\"https://example.com\">[1]</a></sup>")
                    .contains("[99]")
                    .doesNotContain("href=\"\">[99]");
        }
    }

    @Nested
    class ReferenceInMiddleTest {

        @Test
        void render_definitionInMiddle_notExtracted() {
            String markdown = """
                    Some text.

                    [1] https://example.com

                    More text after.
                    """;

            String html = render(markdown);

            // Definition in middle should remain as a paragraph, not be extracted
            // The [1] in the paragraph should be autolinked or left as-is
            assertThat(html).doesNotContain("<div class=\"link-references\">");
        }
    }

    @Nested
    class SpecialCharactersTest {

        @Test
        void render_escapesSpecialCharsInUrl() {
            String markdown = """
                    See [1].

                    [1] https://example.com/page?a=1&b=2
                    """;

            String html = render(markdown);

            assertThat(html)
                    .contains("href=\"https://example.com/page?a=1&amp;b=2\"");
        }
    }

    @Nested
    class FootnotesSectionStructureTest {

        @Test
        void render_containsExpectedHtmlStructure() {
            String markdown = """
                    Text[1].

                    [1] https://example.com
                    """;

            String html = render(markdown);

            assertThat(html)
                    .contains("<hr />")
                    .contains("<div class=\"link-references\">")
                    .contains("<strong>References:</strong>")
                    .contains("<ol>")
                    .contains("<li>")
                    .contains("</ol>")
                    .contains("</div>");
        }
    }

    @Nested
    class RealWorldExamplesTest {

        @Test
        void render_valhallaStyleEmail() {
            String markdown = """
                    Value objects combine some properties of both primitive values
                    and objects. They are class instances whose identity is based
                    on state rather than object identity[1].

                    This enables the JVM to optimize by replacing the identity-based
                    object with direct use of its primitive components (flattening
                    and scalarization)[2].

                    [1] https://openjdk.org/projects/valhalla/value-objects
                    [2] https://openjdk.org/jeps/401
                    """;

            String html = render(markdown);

            assertThat(html)
                    .contains("identity<sup><a href=\"https://openjdk.org/projects/valhalla/value-objects\">[1]</a></sup>")
                    .contains("scalarization)<sup><a href=\"https://openjdk.org/jeps/401\">[2]</a></sup>")
                    .contains("<div class=\"link-references\">")
                    .doesNotContain("[1] https://openjdk.org")
                    .doesNotContain("[2] https://openjdk.org");
        }

        @Test
        void render_withCodeBlock_doesNotModifyArrayAccess() {
            String markdown = """
                    Some code:

                    ```
                    int x = array[0];
                    ```

                    See docs[1].

                    [1] https://docs.example.com
                    """;

            String html = render(markdown);

            // array[0] should not be treated as a reference
            assertThat(html).contains("array[0]");

            // But [1] should be processed
            assertThat(html).contains("<sup><a href=\"https://docs.example.com\">[1]</a></sup>");
        }
    }

    @Nested
    class IntegrationWithMarkdownConverterTest {

        @Test
        void markdownConverter_usesExtension() {
            // Test that the extension works through MarkdownConverter
            var converter = new dev.brice.fancymail.service.MarkdownConverter();

            String markdown = """
                    See the documentation[1].

                    [1] https://example.com/docs
                    """;

            String html = converter.toHtml(markdown);

            assertThat(html)
                    .contains("<sup><a href=\"https://example.com/docs\">[1]</a></sup>")
                    .contains("<div class=\"link-references\">");
        }
    }
}
