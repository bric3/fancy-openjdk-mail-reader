/*
 * fancy-mail-openjdk-reader
 *
 * Copyright (c) 2026 - Brice Dutheil
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package dev.brice.fancymail.service;

import dev.brice.fancymail.model.MailPath;
import dev.brice.fancymail.model.ParsedMail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.*;

class MailParserTest {

    private MailParser parser;
    private MarkdownConverter markdownConverter;
    private LinkRewriter linkRewriter;

    @BeforeEach
    void setUp() {
        markdownConverter = new MarkdownConverter();
        linkRewriter = new LinkRewriter();
        parser = new MailParser(markdownConverter, linkRewriter);
    }

    private String loadFixture(String filename) throws IOException {
        try (InputStream is = getClass().getResourceAsStream("/fixtures/" + filename)) {
            if (is == null) {
                throw new IOException("Fixture not found: " + filename);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    void parse_fixture004306_extractsSubject() throws IOException {
        String html = loadFixture("004306.html");
        MailPath mailPath = new MailPath("amber-spec-experts", "2026-January", "004306");

        ParsedMail parsed = parser.parse(html, mailPath);

        assertThat(parsed.subject()).isEqualTo("Amber features 2026");
    }

    @Test
    void parse_fixture004306_extractsDate() throws IOException {
        String html = loadFixture("004306.html");
        MailPath mailPath = new MailPath("amber-spec-experts", "2026-January", "004306");

        ParsedMail parsed = parser.parse(html, mailPath);

        // Note: JSoup normalizes multiple spaces to single space
        assertThat(parsed.date()).isEqualTo("Fri Jan 9 23:08:05 UTC 2026");
    }

    @Test
    void parse_fixture004306_extractsEmail() throws IOException {
        String html = loadFixture("004306.html");
        MailPath mailPath = new MailPath("amber-spec-experts", "2026-January", "004306");

        ParsedMail parsed = parser.parse(html, mailPath);

        assertThat(parsed.email()).isEqualTo("gavin.bierman@oracle.com");
    }

    @Test
    void parse_fixture004306_extractsList() throws IOException {
        String html = loadFixture("004306.html");
        MailPath mailPath = new MailPath("amber-spec-experts", "2026-January", "004306");

        ParsedMail parsed = parser.parse(html, mailPath);

        assertThat(parsed.list()).isEqualTo("amber-spec-experts");
    }

    @Test
    void parse_fixture004306_extractsOriginalUrl() throws IOException {
        String html = loadFixture("004306.html");
        MailPath mailPath = new MailPath("amber-spec-experts", "2026-January", "004306");

        ParsedMail parsed = parser.parse(html, mailPath);

        assertThat(parsed.originalUrl())
                .isEqualTo("https://mail.openjdk.org/pipermail/amber-spec-experts/2026-January/004306.html");
    }

    @Test
    void parse_fixture004306_bodyContainsContent() throws IOException {
        String html = loadFixture("004306.html");
        MailPath mailPath = new MailPath("amber-spec-experts", "2026-January", "004306");

        ParsedMail parsed = parser.parse(html, mailPath);

        assertThat(parsed.bodyMarkdown() + parsed.bodyHtml())
                .containsIgnoringCase("spec experts");
    }

    @Test
    void parse_fixture004307_extractsSubject() throws IOException {
        String html = loadFixture("004307.html");
        MailPath mailPath = new MailPath("amber-spec-experts", "2026-January", "004307");

        ParsedMail parsed = parser.parse(html, mailPath);

        assertThat(parsed.subject()).isEqualTo("Data Oriented Programming, Beyond Records");
    }

    @Test
    void parse_fixture004307_extractsDate() throws IOException {
        String html = loadFixture("004307.html");
        MailPath mailPath = new MailPath("amber-spec-experts", "2026-January", "004307");

        ParsedMail parsed = parser.parse(html, mailPath);

        assertThat(parsed.date()).isEqualTo("Tue Jan 13 21:52:47 UTC 2026");
    }

    @Test
    void parse_fixture004307_extractsEmail() throws IOException {
        String html = loadFixture("004307.html");
        MailPath mailPath = new MailPath("amber-spec-experts", "2026-January", "004307");

        ParsedMail parsed = parser.parse(html, mailPath);

        assertThat(parsed.email()).isEqualTo("brian.goetz@oracle.com");
    }

    @Test
    void parse_fixture004307_bodyContainsCarrierClasses() throws IOException {
        String html = loadFixture("004307.html");
        MailPath mailPath = new MailPath("amber-spec-experts", "2026-January", "004307");

        ParsedMail parsed = parser.parse(html, mailPath);

        // The body should contain discussion about carrier classes
        assertThat(parsed.bodyMarkdown().toLowerCase() + parsed.bodyHtml().toLowerCase())
                .contains("carrier");
    }

    @Test
    void parse_simpleHtml_extractsBasicInfo() {
        String html = """
                <!DOCTYPE HTML>
                <HTML>
                <HEAD><TITLE>Test Subject</TITLE></HEAD>
                <BODY>
                <H1>Test Subject</H1>
                <B>Test Author</B>
                test.author at example.com
                <BR>
                <I>Mon Jan 1 12:00:00 UTC 2026</I>
                <HR>
                <PRE>This is the email body content.</PRE>
                </BODY>
                </HTML>
                """;
        MailPath mailPath = new MailPath("test-list", "2026-January", "000001");

        ParsedMail parsed = parser.parse(html, mailPath);

        assertThat(parsed.subject()).isEqualTo("Test Subject");
        assertThat(parsed.from()).isEqualTo("Test Author");
        assertThat(parsed.date()).isEqualTo("Mon Jan 1 12:00:00 UTC 2026");
        assertThat(parsed.email()).isEqualTo("test.author@example.com");
    }

    @Test
    void parse_htmlWithNavigationLinks_removesNavigation() {
        String html = """
                <!DOCTYPE HTML>
                <HTML>
                <HEAD><TITLE>Test</TITLE></HEAD>
                <BODY>
                <UL>
                    <LI><a href="date.html">date</a></LI>
                    <LI><a href="thread.html">thread</a></LI>
                </UL>
                <PRE>Body content</PRE>
                <UL>
                    <LI><a href="date.html">date</a></LI>
                </UL>
                </BODY>
                </HTML>
                """;
        MailPath mailPath = new MailPath("test-list", "2026-January", "000001");

        ParsedMail parsed = parser.parse(html, mailPath);

        // Navigation links should not appear in the body
        assertThat(parsed.bodyHtml())
                .doesNotContain("date.html")
                .doesNotContain("thread.html");
    }

    @Test
    void parse_pipermailItalicTags_strippedFromBlockquotes() {
        // Pipermail uses <i>...</I> tags to italicize quoted content
        String html = """
                <!DOCTYPE HTML>
                <HTML>
                <HEAD><TITLE>Test</TITLE></HEAD>
                <BODY>
                <H1>Test</H1>
                <PRE>&gt;<i> This is quoted text
                </I>&gt;<i> More quoted text
                </I>
                Reply text here.
                </PRE>
                </BODY>
                </HTML>
                """;
        MailPath mailPath = new MailPath("test-list", "2026-January", "000001");

        ParsedMail parsed = parser.parse(html, mailPath);

        // Italic tags should be stripped, blockquote should render
        assertThat(parsed.bodyHtml()).contains("<blockquote>");
        assertThat(parsed.bodyHtml()).doesNotContain("<i>");
        assertThat(parsed.bodyHtml()).doesNotContain("</i>");
        assertThat(parsed.bodyMarkdown())
                .contains("> This is quoted text")
                .doesNotContain("<i>")
                .doesNotContain("</i>");
    }

    @Test
    void parse_codeBlockInsideBlockquote_rendersAsCodeBlock() {
        // Code blocks inside blockquotes need proper spacing: > + space + 4 spaces
        String html = """
                <!DOCTYPE HTML>
                <HTML>
                <HEAD><TITLE>Test</TITLE></HEAD>
                <BODY>
                <H1>Test</H1>
                <PRE>&gt;<i> Here is some code:
                </I>&gt;<i>
                </I>&gt;<i>     void example() {
                </I>&gt;<i>         return;
                </I>&gt;<i>     }
                </I>&gt;<i>
                </I>&gt;<i> End of code.
                </I></PRE>
                </BODY>
                </HTML>
                """;
        MailPath mailPath = new MailPath("test-list", "2026-January", "000001");

        ParsedMail parsed = parser.parse(html, mailPath);

        // Code block should render with <pre><code> inside blockquote
        assertThat(parsed.bodyHtml()).contains("<blockquote>");
        assertThat(parsed.bodyHtml()).contains("<pre><code>");
        assertThat(parsed.bodyHtml()).contains("void example()");
    }

    @Test
    void parse_lightlyIndentedCodeWithKeywords_rendersAsCodeBlock() {
        // Lines with 2-3 space indentation containing Java keywords should become code blocks
        // Using explicit \n and spaces to ensure indentation is preserved
        String html = "<!DOCTYPE HTML><HTML><HEAD><TITLE>Test</TITLE></HEAD><BODY>" +
                "<H1>Test</H1>" +
                "<PRE>Just a question, are you proposing that\n" +
                "  case Point(0, 0) -&gt; ...\n" +
                "\n" +
                "is semantically equivalent to\n" +
                "  case Point(var x, var y) when x == 0 -&gt; ...</PRE>" +
                "</BODY></HTML>";
        MailPath mailPath = new MailPath("test-list", "2026-January", "000001");

        ParsedMail parsed = parser.parse(html, mailPath);

        // The indented code lines should render as code blocks, not merged into paragraphs
        assertThat(parsed.bodyHtml()).contains("<pre><code>");
        assertThat(parsed.bodyHtml()).contains("case Point(0, 0)");
        // Should not be merged into the paragraph
        assertThat(parsed.bodyHtml()).doesNotContain("proposing that case");
    }

    @Test
    void parse_lightlyIndentedCodeWithOperators_rendersAsCodeBlock() {
        // Lines with operators like ->, ==, != should be detected as code
        String html = "<!DOCTYPE HTML><HTML><HEAD><TITLE>Test</TITLE></HEAD><BODY>" +
                "<H1>Test</H1>" +
                "<PRE>Consider this example:\n" +
                "  x == 0 &amp;&amp; y == 0\n" +
                "  result -&gt; process()</PRE>" +
                "</BODY></HTML>";
        MailPath mailPath = new MailPath("test-list", "2026-January", "000001");

        ParsedMail parsed = parser.parse(html, mailPath);

        assertThat(parsed.bodyHtml()).contains("<pre><code>");
        assertThat(parsed.bodyHtml()).contains("x == 0");
    }

    @Test
    void parse_lightlyIndentedNonCode_remainsAsParagraph() {
        // Lines with 2-3 space indentation but no code patterns should stay as text
        // Note: avoid Java keywords like "this", "new", "class" in the test text
        String html = "<!DOCTYPE HTML><HTML><HEAD><TITLE>Test</TITLE></HEAD><BODY>" +
                "<H1>Test</H1>" +
                "<PRE>Here is my thought:\n" +
                "  just some regular text\n" +
                "  nothing special here</PRE>" +
                "</BODY></HTML>";
        MailPath mailPath = new MailPath("test-list", "2026-January", "000001");

        ParsedMail parsed = parser.parse(html, mailPath);

        // Should be rendered as regular paragraph text, not code
        assertThat(parsed.bodyHtml()).doesNotContain("<pre><code>just some");
    }

    @Test
    void parse_shortLineWithPunctuation_preservesLineBreak() {
        // Short lines ending with punctuation should have line breaks preserved
        String html = "<!DOCTYPE HTML><HTML><HEAD><TITLE>Test</TITLE></HEAD><BODY>" +
                "<H1>Test</H1>" +
                "<PRE>Hello Gavin,\n" +
                "healing the differences between a case constant is something we should continue.</PRE>" +
                "</BODY></HTML>";
        MailPath mailPath = new MailPath("test-list", "2026-January", "000001");

        ParsedMail parsed = parser.parse(html, mailPath);

        // Should have a <br> between greeting and message, not merged into single line
        assertThat(parsed.bodyHtml()).contains("<br");
        assertThat(parsed.bodyHtml()).doesNotContain("Gavin, healing");
    }

    @Test
    void parse_signatureLines_preservesLineBreaks() {
        // Signature lines like "Best regards," followed by name should preserve breaks
        String html = "<!DOCTYPE HTML><HTML><HEAD><TITLE>Test</TITLE></HEAD><BODY>" +
                "<H1>Test</H1>" +
                "<PRE>Wishing you a happy and successful 2026!\n" +
                "Gavin</PRE>" +
                "</BODY></HTML>";
        MailPath mailPath = new MailPath("test-list", "2026-January", "000001");

        ParsedMail parsed = parser.parse(html, mailPath);

        // Should have line break between wish and name
        assertThat(parsed.bodyHtml()).contains("<br");
        assertThat(parsed.bodyHtml()).doesNotContain("2026! Gavin");
    }

    @Test
    void parse_longLine_noSoftBreak() {
        // Long lines should not get soft breaks even if they end with punctuation
        String html = "<!DOCTYPE HTML><HTML><HEAD><TITLE>Test</TITLE></HEAD><BODY>" +
                "<H1>Test</H1>" +
                "<PRE>This is a much longer line that exceeds the threshold for short lines and ends with punctuation.\n" +
                "This is the next line.</PRE>" +
                "</BODY></HTML>";
        MailPath mailPath = new MailPath("test-list", "2026-January", "000001");

        ParsedMail parsed = parser.parse(html, mailPath);

        // Long lines should be merged normally (no <br>)
        // The markdown will be rendered as a single paragraph
        assertThat(parsed.bodyMarkdown()).doesNotContain("  \n");
    }

    @Test
    void parse_extractsNavigationLinks() {
        // Navigation links should be extracted from pipermail HTML
        String html = "<!DOCTYPE HTML><HTML><HEAD><TITLE>Test</TITLE></HEAD><BODY>" +
                "<UL>" +
                "<LI>Previous message (by thread): <A HREF=\"004313.html\">Previous Subject</A></LI>" +
                "<LI>Next message (by thread): <A HREF=\"004307.html\">Next Subject</A></LI>" +
                "<LI><B>Messages sorted by:</B> " +
                "<a href=\"date.html#4309\">[ date ]</a> " +
                "<a href=\"thread.html#4309\">[ thread ]</a></LI>" +
                "</UL>" +
                "<PRE>Body content</PRE>" +
                "</BODY></HTML>";
        MailPath mailPath = new MailPath("test-list", "2026-January", "004309");

        ParsedMail parsed = parser.parse(html, mailPath);

        assertThat(parsed.navigation()).isNotNull();
        assertThat(parsed.navigation().prevMessage()).isNotNull();
        assertThat(parsed.navigation().prevMessage().url()).isEqualTo("/rendered/test-list/2026-January/004313.html");
        assertThat(parsed.navigation().prevMessage().title()).isEqualTo("Previous Subject");
        assertThat(parsed.navigation().nextMessage()).isNotNull();
        assertThat(parsed.navigation().nextMessage().url()).isEqualTo("/rendered/test-list/2026-January/004307.html");
        assertThat(parsed.navigation().dateIndexUrl()).contains("date.html");
        assertThat(parsed.navigation().threadIndexUrl()).contains("thread.html");
    }

    @Test
    void parse_originalMessageSeparator_rendersAsStyledSeparator() {
        // "----- Original Message -----" should be converted to styled separator
        String html = "<!DOCTYPE HTML><HTML><HEAD><TITLE>Test</TITLE></HEAD><BODY>" +
                "<H1>Test</H1>" +
                "<PRE>Here is my reply.\n" +
                "\n" +
                "----- Original Message -----\n" +
                "The original content here.</PRE>" +
                "</BODY></HTML>";
        MailPath mailPath = new MailPath("test-list", "2026-January", "000001");

        ParsedMail parsed = parser.parse(html, mailPath);

        // Should have bold "Original Message" with Unicode line characters
        assertThat(parsed.bodyHtml()).contains("<strong>───── Original Message ─────</strong>");
        // Should not have the dashes
        assertThat(parsed.bodyHtml()).doesNotContain("-----");
    }

    @Test
    void parse_originalMessageInNestedBlockquote_rendersAsStyledSeparator() {
        // "----- Original Message -----" inside blockquotes should also be styled
        String html = "<!DOCTYPE HTML><HTML><HEAD><TITLE>Test</TITLE></HEAD><BODY>" +
                "<H1>Test</H1>" +
                "<PRE>Here is my reply.\n" +
                "\n" +
                "&gt; Some quoted text\n" +
                "&gt;\n" +
                "&gt;&gt; ----- Original Message -----\n" +
                "&gt;&gt; The deeply nested original content.</PRE>" +
                "</BODY></HTML>";
        MailPath mailPath = new MailPath("test-list", "2026-January", "000001");

        ParsedMail parsed = parser.parse(html, mailPath);

        // Should have bold "Original Message" in blockquote context
        assertThat(parsed.bodyMarkdown()).contains("**───── Original Message ─────**");
        // Should not have the dashes in the output
        assertThat(parsed.bodyMarkdown()).doesNotContain("----- Original Message -----");
    }

    @Test
    void parse_indentedListItems_notConvertedToCodeBlock() {
        // List items with markdown links (containing parentheses) should not be treated as code
        // This tests the sample from fixture 004307
        String html = "<!DOCTYPE HTML><HTML><HEAD><TITLE>Test</TITLE></HEAD><BODY>" +
                "<H1>Test</H1>" +
                "<PRE>\n" +
                " &#160;- [Reconstruction](https://openjdk.org/jeps/468) of record instances, \n" +
                "allowing\n" +
                " &#160; &#160;the appearance of controlled mutation of record state.\n" +
                " &#160;- Automatic marshalling and unmarshalling of record instances.\n" +
                " &#160;- Instantiating or destructuring record instances identifying components\n" +
                " &#160; &#160;nominally rather than positionally.\n" +
                "</PRE>" +
                "</BODY></HTML>";
        MailPath mailPath = new MailPath("test-list", "2026-January", "000001");

        ParsedMail parsed = parser.parse(html, mailPath);

        // Should render as a list, not a code block
        assertThat(parsed.bodyHtml()).contains("<ul>");
        assertThat(parsed.bodyHtml()).contains("<li>");
        // Should NOT be in a code block
        assertThat(parsed.bodyHtml()).doesNotContain("<pre><code>- [Reconstruction]");
        // The orphan "allowing" line should be joined with the previous line
        assertThat(parsed.bodyMarkdown()).contains("instances, allowing");
        assertThat(parsed.bodyMarkdown()).doesNotContain("instances, \nallowing");
    }

    @Test
    void parse_orphanLineAfterCodeBlock_joinedWithPreviousLine() {
        // When email wrapping breaks a code line, the orphan continuation should be joined
        String html = "<!DOCTYPE HTML><HTML><HEAD><TITLE>Test</TITLE></HEAD><BODY>" +
                "<H1>Test</H1>" +
                "<PRE>\n" +
                "    record Rational(int num, int denom) {\n" +
                "        Rational {\n" +
                "            if (denom == 0)\n" +
                "                throw new IllegalArgumentException(&quot;denominator cannot \n" +
                "be zero&quot;);\n" +
                "        }\n" +
                "    }\n" +
                "</PRE>" +
                "</BODY></HTML>";
        MailPath mailPath = new MailPath("test-list", "2026-January", "000001");

        ParsedMail parsed = parser.parse(html, mailPath);

        // The orphan "be zero" should be joined with the previous line
        assertThat(parsed.bodyMarkdown()).contains("cannot be zero");
        assertThat(parsed.bodyMarkdown()).doesNotContain("cannot \nbe zero");
    }

    @Test
    void parse_indentedListItemsWithAsterisk_notConvertedToCodeBlock() {
        // List items using * should also not be treated as code
        String html = "<!DOCTYPE HTML><HTML><HEAD><TITLE>Test</TITLE></HEAD><BODY>" +
                "<H1>Test</H1>" +
                "<PRE>\n" +
                " &#160;* First item with some long text that wraps \n" +
                "to the next line.\n" +
                " &#160;* Second item.\n" +
                " &#160;* Third item.\n" +
                "</PRE>" +
                "</BODY></HTML>";
        MailPath mailPath = new MailPath("test-list", "2026-January", "000001");

        ParsedMail parsed = parser.parse(html, mailPath);

        // Should render as a list, not a code block
        assertThat(parsed.bodyHtml()).contains("<ul>");
        assertThat(parsed.bodyHtml()).contains("<li>");
        // The orphan line should be joined
        assertThat(parsed.bodyMarkdown()).contains("wraps to the next");
    }

    @Test
    void parse_numberedList_notConvertedToCodeBlock() {
        // Numbered lists should not be treated as code
        String html = "<!DOCTYPE HTML><HTML><HEAD><TITLE>Test</TITLE></HEAD><BODY>" +
                "<H1>Test</H1>" +
                "<PRE>\n" +
                " &#160;1. First numbered item with long text that \n" +
                "continues here.\n" +
                " &#160;2. Second numbered item.\n" +
                " &#160;3. Third numbered item.\n" +
                "</PRE>" +
                "</BODY></HTML>";
        MailPath mailPath = new MailPath("test-list", "2026-January", "000001");

        ParsedMail parsed = parser.parse(html, mailPath);

        // Should render as an ordered list, not a code block
        assertThat(parsed.bodyHtml()).contains("<ol>");
        assertThat(parsed.bodyHtml()).contains("<li>");
        // The orphan line should be joined
        assertThat(parsed.bodyMarkdown()).contains("that continues here");
    }

    @Test
    void parse_pipermailWrappedLines_joinsOrphanFragments() {
        // Pipermail wraps at ~72 chars, pushing short fragments to the next line
        // These orphan fragments should be joined back
        String html = "<!DOCTYPE HTML><HTML><HEAD><TITLE>Test</TITLE></HEAD><BODY>" +
                "<H1>Test</H1>" +
                "<PRE>\n" +
                "One might think that we would need some marking on the `x` and `y` \n" +
                "components of\n" +
                "`Point3d` to indicate that they map to the corresponding components of \n" +
                "`Point`,\n" +
                "as we did for associating component fields with their corresponding \n" +
                "components.\n" +
                "But in this case, we need no such marking, because there is no way that \n" +
                "an `int\n" +
                "x` component of `Point` and an `int x` component of its subclass could \n" +
                "possibly\n" +
                "refer to different things -- since they both are tied to the same `int x()`\n" +
                "accessor methods.  So we can safely infer which subclass components are \n" +
                "managed\n" +
                "by superclasses, just by matching up their names and types.\n" +
                "</PRE>" +
                "</BODY></HTML>";
        MailPath mailPath = new MailPath("test-list", "2026-January", "000001");

        ParsedMail parsed = parser.parse(html, mailPath);

        // Orphan fragments should be joined with their previous lines
        assertThat(parsed.bodyMarkdown())
                .contains("`y` components of")
                .contains("components of `Point`,")
                .contains("corresponding components.")
                .contains("way that an `int")
                .contains("could possibly")
                .contains("are managed");
        // Should NOT have orphans on their own lines
        assertThat(parsed.bodyMarkdown())
                .doesNotContain("`y` \ncomponents")
                .doesNotContain("of \n`Point`");
    }

    @Test
    void parse_pipermailWrappedLines_doesNotJoinSignatures() {
        // Signature patterns should not be joined
        String html = "<!DOCTYPE HTML><HTML><HEAD><TITLE>Test</TITLE></HEAD><BODY>" +
                "<H1>Test</H1>" +
                "<PRE>\n" +
                "This is some message content that explains something important.\n" +
                "\n" +
                "regards,\n" +
                "Rémi\n" +
                "</PRE>" +
                "</BODY></HTML>";
        MailPath mailPath = new MailPath("test-list", "2026-January", "000001");

        ParsedMail parsed = parser.parse(html, mailPath);

        // Signature should remain on separate lines
        assertThat(parsed.bodyMarkdown()).contains("regards,");
        assertThat(parsed.bodyMarkdown()).doesNotContain("regards, Rémi");
    }

    @Test
    void parse_pipermailWrappedLines_doesNotJoinParagraphFlow() {
        // Multiple consecutive long lines (paragraph flow) should not be joined
        String html = "<!DOCTYPE HTML><HTML><HEAD><TITLE>Test</TITLE></HEAD><BODY>" +
                "<H1>Test</H1>" +
                "<PRE>\n" +
                "starting values, the block can mutate those variables as desired, and upon\n" +
                "normal completion of the block, those variables are passed to a canonical\n" +
                "constructor to produce the final result.  The main difference is where the\n" +
                "starting values come from.\n" +
                "</PRE>" +
                "</BODY></HTML>";
        MailPath mailPath = new MailPath("test-list", "2026-January", "000001");

        ParsedMail parsed = parser.parse(html, mailPath);

        // Lines should remain separate (they're all long, intentional paragraph flow)
        // The markdown renderer will merge them into a paragraph anyway
        assertThat(parsed.bodyMarkdown()).contains("and upon\nnormal");
        assertThat(parsed.bodyMarkdown()).contains("canonical\nconstructor");
    }

    @Test
    void parse_pipermailWrappedLines_doesNotJoinFencedCodeBlockMarkers() {
        // Fenced code block markers (```) should not be joined with previous line
        String html = "<!DOCTYPE HTML><HTML><HEAD><TITLE>Test</TITLE></HEAD><BODY>" +
                "<H1>Test</H1>" +
                "<PRE>\n" +
                "```\n" +
                "public sealed interface Pair&lt;T,U&gt;(T first, U second) { }\n" +
                "\n" +
                "private record PairImpl&lt;T, U&gt;(T first, U second) implements Pair&lt;T, U&gt; { }\n" +
                "```\n" +
                "</PRE>" +
                "</BODY></HTML>";
        MailPath mailPath = new MailPath("test-list", "2026-January", "000001");

        ParsedMail parsed = parser.parse(html, mailPath);

        // The closing ``` should be on its own line, not joined
        assertThat(parsed.bodyMarkdown()).contains("{ }\n```");
        assertThat(parsed.bodyMarkdown()).doesNotContain("{ } ```");
    }

    @Test
    void parse_indentedCodeBlock_convertedToFencedCodeBlock() {
        // Indented code blocks (4 spaces) should be converted to fenced code blocks
        String html = "<!DOCTYPE HTML><HTML><HEAD><TITLE>Test</TITLE></HEAD><BODY>" +
                "<H1>Test</H1>" +
                "<PRE>\n" +
                "Here is some code:\n" +
                "\n" +
                "    record Point(int x, int y) { }\n" +
                "    record Point3d(int x, int y, int z) { }\n" +
                "\n" +
                "That was the code.\n" +
                "</PRE>" +
                "</BODY></HTML>";
        MailPath mailPath = new MailPath("test-list", "2026-January", "000001");

        ParsedMail parsed = parser.parse(html, mailPath);

        // Should have fenced code block, not indented
        assertThat(parsed.bodyMarkdown()).contains("```\nrecord Point(int x, int y)");
        assertThat(parsed.bodyMarkdown()).contains("{ }\n```");
        // Should NOT have 4-space indented code
        assertThat(parsed.bodyMarkdown()).doesNotContain("    record Point");
    }

    @Test
    void parse_codeBlockInBlockquote_convertedToFencedWithPrefix() {
        // Code blocks inside blockquotes should keep the > prefix
        String html = "<!DOCTYPE HTML><HTML><HEAD><TITLE>Test</TITLE></HEAD><BODY>" +
                "<H1>Test</H1>" +
                "<PRE>\n" +
                "&gt; Here is quoted code:\n" +
                "&gt;\n" +
                "&gt;     void example() {\n" +
                "&gt;         return;\n" +
                "&gt;     }\n" +
                "&gt;\n" +
                "&gt; End of quote.\n" +
                "</PRE>" +
                "</BODY></HTML>";
        MailPath mailPath = new MailPath("test-list", "2026-January", "000001");

        ParsedMail parsed = parser.parse(html, mailPath);

        // Should have fenced code block with > prefix
        assertThat(parsed.bodyMarkdown()).contains("> ```\n> void example()");
        assertThat(parsed.bodyMarkdown()).contains("> }\n> ```");
    }

    @Test
    void parse_existingFencedCodeBlock_notModified() {
        // Content inside existing fenced code blocks should not be modified
        String html = "<!DOCTYPE HTML><HTML><HEAD><TITLE>Test</TITLE></HEAD><BODY>" +
                "<H1>Test</H1>" +
                "<PRE>\n" +
                "```\n" +
                "class AlmostRecord(int x,\n" +
                "                    int y,\n" +
                "                    Optional&lt;String&gt; s) {\n" +
                "\n" +
                "     private final component int x;\n" +
                "     private final component int y;\n" +
                "     private final String s;\n" +
                "}\n" +
                "```\n" +
                "</PRE>" +
                "</BODY></HTML>";
        MailPath mailPath = new MailPath("test-list", "2026-January", "000001");

        ParsedMail parsed = parser.parse(html, mailPath);

        // Should have exactly one opening and one closing fence
        String markdown = parsed.bodyMarkdown();
        int openCount = markdown.split("```", -1).length - 1;
        assertThat(openCount).isEqualTo(2); // One open, one close

        // Content should be preserved with indentation
        assertThat(markdown).contains("class AlmostRecord(int x,");
        assertThat(markdown).contains("                    int y,");
        assertThat(markdown).contains("     private final component int x;");
    }

    @Test
    void parse_listContinuationLines_notConvertedToCodeBlock() {
        // Indented continuation lines inside list items should not become code blocks
        String html = "<!DOCTYPE HTML><HTML><HEAD><TITLE>Test</TITLE></HEAD><BODY>" +
                "<H1>Test</H1>" +
                "<PRE>\n" +
                "  - A carrier class extends a non-carrier class;\n" +
                "  - A non-carrier class extends a carrier class;\n" +
                "  - A carrier class extends another carrier class, where all of the superclass\n" +
                "    components are subsumed by the subclass state description;\n" +
                "  - A carrier class extends another carrier class, but there are one or more\n" +
                "    superclass components that are not subsumed by the subclass state\n" +
                "    description.\n" +
                "</PRE>" +
                "</BODY></HTML>";
        MailPath mailPath = new MailPath("test-list", "2026-January", "000001");

        ParsedMail parsed = parser.parse(html, mailPath);

        // Should NOT have code blocks for list continuations
        assertThat(parsed.bodyMarkdown()).doesNotContain("```\ncomponents are subsumed");
        assertThat(parsed.bodyMarkdown()).doesNotContain("```\nsuperclass components");
        // List items should be preserved
        assertThat(parsed.bodyMarkdown()).contains("- A carrier class extends a non-carrier class;");
        assertThat(parsed.bodyMarkdown()).contains("components are subsumed");
    }

    @Test
    void parse_codeInsideListItem_detectedAndFenced() {
        // Code inside list items should be detected using looksLikeCode heuristic
        String html = "<!DOCTYPE HTML><HTML><HEAD><TITLE>Test</TITLE></HEAD><BODY>" +
                "<H1>Test</H1>" +
                "<PRE>\n" +
                "- you mutate variables when you reduce accumulators in a loop\n" +
                "  var v1 = ...\n" +
                "  var v2 = ...\n" +
                "  for(...) {\n" +
                "    (v1, v2) = f(v1, v2);\n" +
                "  }\n" +
                "\n" +
                "- you mutate variables after a condition\n" +
                "  var v1 = ...\n" +
                "  if (...) {\n" +
                "    (v1, v2) = f(...);\n" +
                "  }\n" +
                "</PRE>" +
                "</BODY></HTML>";
        MailPath mailPath = new MailPath("test-list", "2026-January", "000001");

        ParsedMail parsed = parser.parse(html, mailPath);

        // Code should be in fenced blocks
        assertThat(parsed.bodyMarkdown()).contains("```");
        assertThat(parsed.bodyMarkdown()).contains("var v1 = ...");
        assertThat(parsed.bodyMarkdown()).contains("for(...) {");
        // List items should be preserved
        assertThat(parsed.bodyMarkdown()).contains("- you mutate variables when you reduce");
        assertThat(parsed.bodyMarkdown()).contains("- you mutate variables after a condition");

        // Consecutive code lines should be in a SINGLE code block, not multiple blocks
        // Count the number of ``` occurrences - should be 4 (2 list items x 2 fences each = 4)
        long fenceCount = parsed.bodyMarkdown().split("```", -1).length - 1;
        assertThat(fenceCount)
                .as("Expected 2 code blocks (4 fence markers), but got %d fence markers", fenceCount)
                .isEqualTo(4);
    }

    @Test
    void parse_fixture004317_codeInsideListItems() throws IOException {
        // Real fixture from https://mail.openjdk.org/pipermail/amber-spec-experts/2026-January/004317.html
        String html = loadFixture("004317.html");
        MailPath mailPath = new MailPath("amber-spec-experts", "2026-January", "004317");

        ParsedMail parsed = parser.parse(html, mailPath);

        // List items with code examples should be properly formatted
        assertThat(parsed.bodyMarkdown())
                .contains("- you mutate variables when you reduce accumulators in a loop")
                .contains("- you mutate variables after a condition")
                .contains("- you mutate variables when you transfer values in between scopes");

        // Code inside list items should be in fenced blocks
        assertThat(parsed.bodyMarkdown()).contains("```");
        assertThat(parsed.bodyMarkdown()).contains("var v1 = ...");
        assertThat(parsed.bodyMarkdown()).contains("for(...) {");
        assertThat(parsed.bodyMarkdown()).contains("if (...) {");
        assertThat(parsed.bodyMarkdown()).contains("try(...) {");

        // HTML should also render correctly with code blocks
        assertThat(parsed.bodyHtml()).contains("<ul>");
        assertThat(parsed.bodyHtml()).contains("<li>");
    }

    @Test
    void parse_proseEndingWithSemicolon_notTreatedAsCode() {
        // Prose that ends with semicolons should NOT be treated as code
        // This tests the fix for lines like "components are subsumed by the subclass state description;"
        String html = "<!DOCTYPE HTML><HTML><HEAD><TITLE>Test</TITLE></HEAD><BODY>" +
                "<H1>Test</H1>" +
                "<PRE>\n" +
                "There are four cases to consider:\n" +
                "\n" +
                "  - A carrier class extends a non-carrier class;\n" +
                "  - A non-carrier class extends a carrier class;\n" +
                "  - A carrier class extends another carrier class, where all of the superclass\n" +
                "    components are subsumed by the subclass state description;\n" +
                "  - A carrier class extends another carrier class, but there are one or more\n" +
                "    superclass components that are not subsumed by the subclass state\n" +
                "    description.\n" +
                "</PRE>" +
                "</BODY></HTML>";
        MailPath mailPath = new MailPath("test-list", "2026-January", "000001");

        ParsedMail parsed = parser.parse(html, mailPath);

        // List items should be preserved
        assertThat(parsed.bodyMarkdown())
                .contains("- A carrier class extends a non-carrier class;")
                .contains("- A non-carrier class extends a carrier class;")
                .contains("- A carrier class extends another carrier class");

        // Prose continuation lines should NOT be in code blocks
        // The word "components" should appear in the text, not in a code fence
        assertThat(parsed.bodyMarkdown())
                .contains("components are subsumed")
                .doesNotContain("```\ncomponents")
                .doesNotContain("```\n  components");

        // There should be NO code blocks at all (no fenced blocks)
        long fenceCount = parsed.bodyMarkdown().split("```", -1).length - 1;
        assertThat(fenceCount)
                .as("Expected no code blocks in prose-only list, but got %d fence markers", fenceCount)
                .isEqualTo(0);
    }

    @Test
    void parse_proseWithJavaKeywords_notTreatedAsCode() {
        // Prose that mentions Java keywords like "record", "class", "case" should NOT
        // be treated as code just because the keywords appear. This is common in
        // discussions about Java features on mailing lists.
        String html = "<!DOCTYPE HTML><HTML><HEAD><TITLE>Test</TITLE></HEAD><BODY>" +
                "<H1>Test</H1>" +
                "<PRE>\n" +
                "Properties of records:\n" +
                "\n" +
                "  - The components are nominal; their names are a committed part of the\n" +
                "    record's API.\n" +
                "  - The class is final and cannot extend any other class.\n" +
                "  - A record component can hold a reference to its containing instance.\n" +
                "</PRE>" +
                "</BODY></HTML>";
        MailPath mailPath = new MailPath("test-list", "2026-January", "000001");

        ParsedMail parsed = parser.parse(html, mailPath);

        // The prose should be preserved as text, not wrapped in code blocks
        assertThat(parsed.bodyMarkdown())
                .contains("record's API")
                .contains("The class is final")
                .doesNotContain("```\nrecord")
                .doesNotContain("```\n  record");

        // There should be NO code blocks (keywords alone should not trigger code detection)
        long fenceCount = parsed.bodyMarkdown().split("```", -1).length - 1;
        assertThat(fenceCount)
                .as("Expected no code blocks in prose with Java keywords, but got %d fence markers", fenceCount)
                .isEqualTo(0);
    }

    @Test
    void parse_codeInNestedBlockquotes_detectedAsFencedBlock() {
        // Code blocks inside nested blockquotes should be detected and fenced
        // This tests deeply nested blockquotes like "> > >" with indented code
        String html = "<!DOCTYPE HTML><HTML><HEAD><TITLE>Test</TITLE></HEAD><BODY>" +
                "<H1>Test</H1>" +
                "<PRE>\n" +
                "&gt; &gt; &gt; disassemble a value, for example:\n" +
                "&gt; &gt; &gt;\n" +
                "&gt; &gt; &gt;    record ColorPoint(int x, int y, RGB color) {}\n" +
                "&gt; &gt; &gt;\n" +
                "&gt; &gt; &gt;    void somethingImportant(ColorPoint cp) {\n" +
                "&gt; &gt; &gt;        if (cp instanceof ColorPoint(var x, var y, var c)) {\n" +
                "&gt; &gt; &gt;            // important code\n" +
                "&gt; &gt; &gt;        }\n" +
                "&gt; &gt; &gt;    }\n" +
                "&gt; &gt; &gt;\n" +
                "&gt; &gt; &gt; The use of pattern matching is great.\n" +
                "</PRE>" +
                "</BODY></HTML>";
        MailPath mailPath = new MailPath("test-list", "2026-January", "000001");

        ParsedMail parsed = parser.parse(html, mailPath);

        // Code should be in fenced blocks within the blockquote
        assertThat(parsed.bodyMarkdown())
                .contains("```")
                .contains("record ColorPoint")
                .contains("void somethingImportant");

        // The prose should NOT be in code blocks
        assertThat(parsed.bodyMarkdown())
                .contains("disassemble a value")
                .contains("pattern matching is great");
    }
}
