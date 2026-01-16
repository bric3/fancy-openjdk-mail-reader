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

import dev.brice.fancymail.model.MailNavigation;
import dev.brice.fancymail.model.MailPath;
import dev.brice.fancymail.model.ParsedMail;
import jakarta.inject.Singleton;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.parser.Parser;

/**
 * Service to parse HTML content from OpenJDK mailing list emails.
 */
@Singleton
public class MailParser {

    private static final Logger LOG = LoggerFactory.getLogger(MailParser.class);

    // Pattern to extract email from "name at domain" format
    private static final Pattern EMAIL_PATTERN = Pattern.compile("([\\w.-]+)\\s+at\\s+([\\w.-]+)");

    // Pattern to match date format like "Tue Jan 13 21:52:47 UTC 2026"
    private static final Pattern DATE_PATTERN = Pattern.compile(
            "(Mon|Tue|Wed|Thu|Fri|Sat|Sun)\\s+" +
            "(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)\\s+" +
            "\\d{1,2}\\s+\\d{2}:\\d{2}:\\d{2}\\s+\\w+\\s+\\d{4}"
    );

    private final MarkdownConverter markdownConverter;
    private final LinkRewriter linkRewriter;

    public MailParser(MarkdownConverter markdownConverter, LinkRewriter linkRewriter) {
        this.markdownConverter = markdownConverter;
        this.linkRewriter = linkRewriter;
    }

    /**
     * Parse the HTML content and extract email information.
     *
     * @param html     the raw HTML content
     * @param mailPath the mail path for context
     * @return a ParsedMail record containing extracted information
     */
    public ParsedMail parse(String html, MailPath mailPath) {
        Document doc = Jsoup.parse(html);

        // Extract subject from title tag - pipermail format is "[list] subject"
        String subject = extractSubject(doc);

        // Extract author info from strong tag
        String from = extractFrom(doc);
        String email = extractEmail(doc);

        // Extract date from em tag or text
        String date = extractDate(doc);

        // Extract body content - the main message
        // Pipermail emails have the body in a PRE tag which is already markdown-formatted
        Element preElement = doc.body().selectFirst("pre");
        String bodyMarkdown;
        String bodyHtml;

        if (preElement != null) {
            // PRE content is already markdown-like - extract and clean it
            bodyMarkdown = extractPreContentAsMarkdown(preElement, mailPath);
            // For HTML view, wrap cleaned markdown in a pre-like div
            bodyHtml = "<div class=\"email-body\">" + markdownToSimpleHtml(bodyMarkdown) + "</div>";
        } else {
            // Fallback for non-standard format
            Element body = extractBodyElement(doc);
            if (body != null) {
                linkRewriter.rewriteLinks(body);
            }
            bodyMarkdown = body != null ? markdownConverter.toMarkdown(body) : "";
            bodyHtml = body != null ? body.html() : "";
        }

        // Extract navigation links
        MailNavigation navigation = extractNavigation(doc, mailPath);

        LOG.debug("Parsed mail: subject='{}', from='{}', email='{}', date='{}'",
                subject, from, email, date);

        return new ParsedMail(
                subject,
                from,
                email,
                date,
                mailPath.list(),
                bodyMarkdown,
                bodyHtml,
                mailPath.toOriginalUrl(),
                navigation
        );
    }

    private String extractSubject(Document doc) {
        // Try the title tag first - pipermail format is "[list] subject"
        String title = doc.title();
        if (title != null && !title.isBlank()) {
            // Often the title is formatted like "[list] Subject"
            int bracketEnd = title.indexOf(']');
            if (bracketEnd > 0 && bracketEnd < title.length() - 1) {
                return title.substring(bracketEnd + 1).trim();
            }
            return title.trim();
        }

        // Fallback: look for h1 or the first text block
        Element h1 = doc.selectFirst("h1");
        if (h1 != null) {
            return h1.text();
        }

        return "Unknown Subject";
    }

    private String extractFrom(Document doc) {
        // Look for the author name - pipermail uses <B> tag (not <strong>)
        // Try <B> first (pipermail format), then <strong> as fallback
        Element authorTag = doc.body().selectFirst("b");
        if (authorTag == null) {
            authorTag = doc.body().selectFirst("strong");
        }
        if (authorTag != null) {
            String text = authorTag.text();
            if (!text.isBlank()) {
                return text.trim();
            }
        }

        return "Unknown Author";
    }

    private String extractEmail(Document doc) {
        // Look for email pattern in the text following the author tag
        // Pipermail uses <B> for author, followed by email in <A> tag or text
        Element authorTag = doc.body().selectFirst("b");
        if (authorTag == null) {
            authorTag = doc.body().selectFirst("strong");
        }
        if (authorTag != null) {
            // Get the text node following the author tag
            Node next = authorTag.nextSibling();
            while (next != null) {
                if (next instanceof TextNode) {
                    String text = ((TextNode) next).text();
                    Matcher matcher = EMAIL_PATTERN.matcher(text);
                    if (matcher.find()) {
                        return matcher.group(1) + "@" + matcher.group(2);
                    }
                } else if (next instanceof Element) {
                    Element elem = (Element) next;
                    // Check if it's a link containing email
                    if (elem.tagName().equalsIgnoreCase("a")) {
                        String text = elem.text();
                        Matcher matcher = EMAIL_PATTERN.matcher(text);
                        if (matcher.find()) {
                            return matcher.group(1) + "@" + matcher.group(2);
                        }
                    }
                    // Don't go past the date (em/i tag)
                    if (elem.tagName().equalsIgnoreCase("em") || elem.tagName().equalsIgnoreCase("i")) {
                        break;
                    }
                }
                next = next.nextSibling();
            }
        }

        // Also try looking at the whole body text
        String bodyText = doc.body().text();
        Matcher matcher = EMAIL_PATTERN.matcher(bodyText);
        if (matcher.find()) {
            return matcher.group(1) + "@" + matcher.group(2);
        }

        return null;
    }

    private String extractDate(Document doc) {
        // Look for date - pipermail uses <I> tag (not <em>)
        // Try <I> first, then <em> as fallback
        Element dateTag = doc.body().selectFirst("i");
        if (dateTag == null) {
            dateTag = doc.body().selectFirst("em");
        }
        if (dateTag != null) {
            String text = dateTag.text();
            if (!text.isBlank()) {
                return text.trim();
            }
        }

        // Try to find date pattern in the body text
        String bodyText = doc.body().text();
        Matcher matcher = DATE_PATTERN.matcher(bodyText);
        if (matcher.find()) {
            return matcher.group();
        }

        return "Unknown Date";
    }

    private Element extractBodyElement(Document doc) {
        // Clone the body to avoid modifying the original
        Element body = doc.body().clone();

        // Remove navigation lists
        body.select("ul").stream()
                .filter(this::isNavigationList)
                .forEach(Element::remove);

        // Remove header elements: first strong, following text, em, br elements
        Element firstStrong = body.selectFirst("strong");
        if (firstStrong != null) {
            removeHeaderElements(firstStrong);
        }

        // Remove horizontal rules
        body.select("hr").forEach(Element::remove);

        // Find the main content - typically starts after the header
        // Look for the first pre tag or significant content
        Element pre = body.selectFirst("pre");
        if (pre != null) {
            return pre;
        }

        // If no pre tag, return the cleaned body
        return body;
    }

    private void removeHeaderElements(Element firstStrong) {
        // Remove the strong tag and surrounding header content
        Element parent = firstStrong.parent();
        if (parent == null) return;

        // Find the em tag (date) which marks the end of header
        Element em = null;
        Node current = firstStrong;
        while (current != null) {
            if (current instanceof Element) {
                Element elem = (Element) current;
                if (elem.tagName().equals("em")) {
                    em = elem;
                    break;
                }
            }
            current = current.nextSibling();
        }

        // Remove elements from strong to em (inclusive)
        current = firstStrong;
        while (current != null) {
            Node next = current.nextSibling();
            current.remove();
            if (current == em) break;
            current = next;
        }

        // Also remove br tags at the start
        while (parent.childNodeSize() > 0) {
            Node first = parent.childNode(0);
            if (first instanceof Element && ((Element) first).tagName().equals("br")) {
                first.remove();
            } else if (first instanceof TextNode && ((TextNode) first).isBlank()) {
                first.remove();
            } else {
                break;
            }
        }
    }

    private boolean isNavigationList(Element ul) {
        // Navigation lists contain links to date.html, thread.html, etc.
        Elements links = ul.select("a");
        for (Element link : links) {
            String href = link.attr("href");
            if (href.contains("date.html") || href.contains("thread.html") ||
                href.contains("subject.html") || href.contains("author.html")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Extract navigation links from pipermail HTML.
     * Pipermail has navigation at the top and bottom with prev/next message links
     * and links to sorted indexes (by date, thread, subject, author).
     */
    private MailNavigation extractNavigation(Document doc, MailPath mailPath) {
        MailNavigation.NavLink prevMessage = null;
        MailNavigation.NavLink nextMessage = null;
        String dateIndexUrl = null;
        String threadIndexUrl = null;
        String subjectIndexUrl = null;
        String authorIndexUrl = null;

        // Find navigation UL elements
        Elements uls = doc.select("ul");
        for (Element ul : uls) {
            if (!isNavigationList(ul)) {
                continue;
            }

            // Extract prev/next message links
            Elements lis = ul.select("li");
            for (Element li : lis) {
                String text = li.text().toLowerCase();
                Element link = li.selectFirst("a");

                if (link != null) {
                    String href = link.attr("href");
                    String linkText = link.text().trim();

                    if (text.contains("previous message")) {
                        // Rewrite to local server URL
                        String localUrl = rewriteNavLink(href, mailPath);
                        prevMessage = new MailNavigation.NavLink(localUrl, linkText);
                    } else if (text.contains("next message")) {
                        String localUrl = rewriteNavLink(href, mailPath);
                        nextMessage = new MailNavigation.NavLink(localUrl, linkText);
                    }
                }
            }

            // Extract index links (keep pointing to pipermail)
            Elements indexLinks = ul.select("a");
            String baseUrl = "https://mail.openjdk.org/pipermail/" + mailPath.list() + "/" + mailPath.yearMonth() + "/";
            for (Element link : indexLinks) {
                String href = link.attr("href");
                if (href.contains("date.html")) {
                    dateIndexUrl = baseUrl + href;
                } else if (href.contains("thread.html")) {
                    threadIndexUrl = baseUrl + href;
                } else if (href.contains("subject.html")) {
                    subjectIndexUrl = baseUrl + href;
                } else if (href.contains("author.html")) {
                    authorIndexUrl = baseUrl + href;
                }
            }

            // Only need to process the first navigation list
            if (prevMessage != null || nextMessage != null) {
                break;
            }
        }

        return new MailNavigation(
                prevMessage,
                nextMessage,
                dateIndexUrl,
                threadIndexUrl,
                subjectIndexUrl,
                authorIndexUrl
        );
    }

    /**
     * Rewrite a navigation link (like "004313.html") to a local server URL.
     */
    private String rewriteNavLink(String href, MailPath mailPath) {
        if (href == null || href.isBlank()) {
            return href;
        }
        // Extract message ID from href like "004313.html"
        if (href.matches("\\d+\\.html")) {
            String messageId = href.replace(".html", "");
            return "/rendered/" + mailPath.list() + "/" + mailPath.yearMonth() + "/" + messageId + ".html";
        }
        return href;
    }

    /**
     * Extract PRE content as markdown, cleaning up HTML entities and links.
     * Pipermail PRE content is already markdown-formatted text.
     */
    private String extractPreContentAsMarkdown(Element preElement, MailPath mailPath) {
        // Get the inner HTML to preserve structure
        String content = preElement.html();

        // Convert HTML links to markdown links
        // Pattern: <A HREF="url">text</A> or <a href="url">text</a>
        // Note: Pipermail often has markdown links with HTML inside: [text](<A HREF="url">url</A>)
        // In that case, we just need to extract the URL, not create a new markdown link
        Pattern linkPattern = Pattern.compile(
                "<[Aa]\\s+[Hh][Rr][Ee][Ff]=\"([^\"]+)\"[^>]*>([^<]*)</[Aa]>",
                Pattern.CASE_INSENSITIVE);
        Matcher linkMatcher = linkPattern.matcher(content);
        StringBuffer sb = new StringBuffer();
        while (linkMatcher.find()) {
            String url = linkMatcher.group(1);
            String text = linkMatcher.group(2).trim();
            // Rewrite openjdk mail links to our rendered path
            String rewrittenUrl = linkRewriter.rewriteLink(url);

            // Check if this link is already inside a markdown link syntax
            // by looking at what comes before the match
            int matchStart = linkMatcher.start();
            boolean insideMarkdownLink = matchStart > 0 && content.charAt(matchStart - 1) == '(';

            String replacement;
            if (insideMarkdownLink || text.equals(url) || text.startsWith("http")) {
                // Just output the URL - either we're inside markdown syntax or the text is the URL
                replacement = rewrittenUrl;
            } else {
                // Create a full markdown link
                replacement = "[" + text + "](" + rewrittenUrl + ")";
            }
            linkMatcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        linkMatcher.appendTail(sb);
        content = sb.toString();

        // Remove pipermail italic tags used for quoted content
        // Pipermail wraps quoted lines in <i>...</I> tags (case varies)
        // These must be removed before markdown parsing or they break blockquote detection
        content = content.replaceAll("(?i)</?i>", "");

        // Decode HTML entities
        content = Parser.unescapeEntities(content, false);

        // Normalize blockquote lines: ensure > is followed by a space
        // This is required for proper markdown parsing, especially for code blocks
        // inside blockquotes which need ">     code" (> + space + 4 space indent)
        // Pipermail gives us ">    code" (> + 4 spaces) which isn't enough
        content = content.replaceAll("(?m)^>(\\S)", "> $1");    // >text -> > text
        content = content.replaceAll("(?m)^>( {2,})", "> $1");  // >  + spaces -> >   + spaces (adds one for code blocks)

        // Replace non-breaking spaces with regular spaces
        content = content.replace('\u00A0', ' ');

        // Remove attachment notices (lines starting with "-------------- next part")
        content = content.replaceAll("(?m)^-{10,} next part.*(?:\\r?\\n.*)*$", "");

        // Convert "----- Original Message -----" to a styled separator
        // Uses Unicode box-drawing characters for a clean titled border look
        content = content.replaceAll(
                "(?m)^-{3,}\\s*(Original Message|Forwarded Message)\\s*-{3,}$",
                "\n**── $1 ──**\n"
        );

        // Convert lightly-indented code lines (2-3 spaces) to proper code blocks (4 spaces)
        // This handles cases like "  case Point(0, 0) -> ..." which would otherwise
        // be merged into the paragraph above
        content = convertLightlyIndentedCodeToBlocks(content);

        // Fix orphan continuation lines from email wrapping
        // When email wrapping breaks a line mid-code-block, the continuation starts at column 0,
        // which prematurely terminates the code block. Re-indent single orphan lines.
        content = fixOrphanContinuationLines(content);

        // Trim trailing whitespace from each line but preserve line breaks
        content = content.lines()
                .map(String::stripTrailing)
                .reduce((a, b) -> a + "\n" + b)
                .orElse("");

        // Add soft line breaks (two trailing spaces) to short lines ending with punctuation
        // This preserves line breaks for greetings like "Hello Gavin," and signatures like "Gavin"
        content = addSoftBreaksToShortLines(content);

        // Remove excessive blank lines (more than 2 consecutive)
        content = content.replaceAll("\n{3,}", "\n\n");

        return content.trim();
    }

    // Pattern for detecting code-like content
    // Java keywords, operators, and common code patterns
    private static final Pattern CODE_PATTERN = Pattern.compile(
            "\\b(case|switch|if|else|for|while|do|return|break|continue|class|interface|enum|record|" +
            "void|int|long|double|float|boolean|char|byte|short|var|final|static|public|private|protected|" +
            "new|null|true|false|this|super|throws?|try|catch|finally|instanceof|extends|implements)\\b|" +
            "->|=>|==|!=|<=|>=|&&|\\|\\||\\{|\\}|\\(.*\\)|//|/\\*|\\*/|;$"
    );

    /**
     * Convert lightly-indented lines (2-3 spaces) that look like code into proper
     * markdown code blocks (4 space indentation).
     * <p>
     * This handles cases like:
     * <pre>
     * Just a question, are you proposing that
     *   case Point(0, 0) -> ...
     * </pre>
     * Where the "case" line would otherwise be merged into the paragraph.
     * <p>
     * Markdown requires a blank line before indented code blocks, so we add one
     * when the previous line is not blank and not already a code block.
     */
    private String convertLightlyIndentedCodeToBlocks(String content) {
        String[] lines = content.split("\n", -1);
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String prevLine = i > 0 ? lines[i - 1] : "";

            // Check for lines with 2-3 space indentation (not already 4+)
            if (line.matches("^ {2,3}\\S.*") && looksLikeCode(line.trim())) {
                // Add blank line before code block if previous line is not blank
                // and not already an indented code block
                if (!prevLine.isBlank() && !prevLine.startsWith("    ")) {
                    result.append("\n");
                }
                // Convert to 4-space indentation for proper code block
                result.append("    ").append(line.trim());
            } else {
                result.append(line);
            }

            if (i < lines.length - 1) {
                result.append("\n");
            }
        }

        return result.toString();
    }

    /**
     * Heuristic check if a line looks like code based on common patterns.
     */
    private boolean looksLikeCode(String line) {
        return CODE_PATTERN.matcher(line).find();
    }

    // Pattern to extract leading indentation (spaces or blockquote prefix "> " followed by spaces)
    private static final Pattern INDENT_PATTERN = Pattern.compile("^((?:> ?)?[ ]+)");

    /**
     * Fix orphan continuation lines caused by email line wrapping.
     * <p>
     * When email clients wrap long lines in code blocks, the continuation often starts
     * at column 0, which prematurely terminates the markdown code block. For example:
     * <pre>
     *     throw new IllegalArgumentException("denominator cannot
     * be zero");
     * </pre>
     * becomes:
     * <pre>
     *     throw new IllegalArgumentException("denominator cannot be zero");
     * </pre>
     * <p>
     * This method detects such orphan lines and joins them with the previous line.
     * It only fixes SINGLE orphan lines (where the next line resumes normal indentation
     * or is blank), to avoid incorrectly modifying intentionally unindented text.
     * <p>
     * Also handles blockquotes (lines starting with "> " followed by indentation).
     */
    private String fixOrphanContinuationLines(String content) {
        String[] lines = content.split("\n", -1);
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String prevLine = i > 0 ? lines[i - 1] : "";
            String nextLine = i < lines.length - 1 ? lines[i + 1] : "";

            // Check if this is an orphan continuation line:
            // 1. Current line starts at column 0 (no leading whitespace, not a blockquote)
            // 2. Previous line was indented (code block or blockquote with code)
            // 3. Current line is not blank
            // 4. Next line either resumes indentation or is blank (single orphan)
            boolean isOrphanLine = false;

            if (!line.isEmpty() && !Character.isWhitespace(line.charAt(0)) && !line.startsWith(">")) {
                Matcher prevIndentMatcher = INDENT_PATTERN.matcher(prevLine);
                if (prevIndentMatcher.find()) {
                    String prevIndent = prevIndentMatcher.group(1);
                    // Check if it's a code block indent (4+ spaces, or blockquote + 4+ spaces for code in quotes)
                    int spaceCount = prevIndent.length() - prevIndent.replace(" ", "").length();

                    if (spaceCount >= 4) {
                        // Check next line: should resume indentation or be blank
                        boolean nextLineResumes = nextLine.isBlank() ||
                                INDENT_PATTERN.matcher(nextLine).find() ||
                                nextLine.startsWith("```");

                        if (nextLineResumes) {
                            isOrphanLine = true;
                        }
                    }
                }
            }

            if (isOrphanLine) {
                // Join with previous line: remove trailing newline and add space
                if (result.length() > 0 && result.charAt(result.length() - 1) == '\n') {
                    result.setLength(result.length() - 1);
                    result.append(" ");
                }
                result.append(line);
            } else {
                result.append(line);
            }

            if (i < lines.length - 1) {
                result.append("\n");
            }
        }

        return result.toString();
    }

    // Maximum length for a "short line" that should preserve its line break
    private static final int SHORT_LINE_MAX_LENGTH = 60;

    /**
     * Add markdown soft line breaks (two trailing spaces) to short lines ending with punctuation.
     * <p>
     * In markdown, consecutive lines are merged into a paragraph. This causes issues for:
     * - Greetings: "Hello Gavin," followed by the message body
     * - Signatures: "Wishing you a happy 2026!" followed by "Gavin"
     * <p>
     * By adding two trailing spaces, we create a soft break ({@code <br>}) in HTML.
     */
    private String addSoftBreaksToShortLines(String content) {
        String[] lines = content.split("\n", -1);
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String nextLine = i < lines.length - 1 ? lines[i + 1] : "";

            // Check if this line should have a soft break:
            // - Short line (under threshold)
            // - Ends with punctuation (. ! ? ,)
            // - Next line is not blank (otherwise paragraph break is fine)
            // - Not inside a code block (doesn't start with 4 spaces)
            // - Not a blockquote marker line (just ">")
            String trimmed = line.trim();
            boolean isShortLine = trimmed.length() > 0 && trimmed.length() <= SHORT_LINE_MAX_LENGTH;
            boolean endsWithPunctuation = trimmed.matches(".*[.!?,]$");
            boolean nextLineNotBlank = !nextLine.isBlank();
            boolean notCodeBlock = !line.startsWith("    ");
            boolean notBlockquoteOnly = !trimmed.equals(">");

            if (isShortLine && endsWithPunctuation && nextLineNotBlank && notCodeBlock && notBlockquoteOnly) {
                result.append(line).append("  "); // Add two spaces for soft break
            } else {
                result.append(line);
            }

            if (i < lines.length - 1) {
                result.append("\n");
            }
        }

        return result.toString();
    }

    /**
     * Convert markdown to simple HTML for the HTML view.
     * This is a basic conversion for display purposes.
     */
    private String markdownToSimpleHtml(String markdown) {
        // Use the markdown converter to convert markdown to HTML
        return markdownConverter.toHtml(markdown);
    }
}
