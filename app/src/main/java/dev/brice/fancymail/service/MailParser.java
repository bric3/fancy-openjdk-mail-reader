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

    // Pattern to match OpenJDK mailing list URLs
    private static final Pattern OPENJDK_MAIL_URL_PATTERN = Pattern.compile(
            "https?://mail\\.openjdk\\.org/pipermail/([^/]+)/([^/]+)/(\\d+)\\.html"
    );

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
            // Keep original URLs in markdown for raw markdown output
            bodyMarkdown = extractPreContentAsMarkdown(preElement, mailPath, false);
            // For HTML view, extract with rewritten URLs, then convert to HTML
            // The MailingListLinkRefExtension handles [n] style references during markdown to HTML conversion
            String bodyMarkdownForHtml = extractPreContentAsMarkdown(preElement, mailPath, true);
            bodyHtml = "<div class=\"email-body\">" + markdownToSimpleHtml(bodyMarkdownForHtml) + "</div>";
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
     *
     * @param preElement the PRE element to extract content from
     * @param mailPath the mail path for context
     * @param rewriteLinks if true, rewrite OpenJDK mail links to local paths; if false, keep original URLs
     */
    private String extractPreContentAsMarkdown(Element preElement, MailPath mailPath, boolean rewriteLinks) {
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
            // Optionally rewrite openjdk mail links to our rendered path
            String finalUrl = rewriteLinks ? linkRewriter.rewriteLink(url) : url;

            // Check if this link is already inside a markdown link syntax
            // by looking at what comes before the match
            int matchStart = linkMatcher.start();
            boolean insideMarkdownLink = matchStart > 0 && content.charAt(matchStart - 1) == '(';

            // Check if URL is an OpenJDK mail link (for display text shortening)
            Matcher openjdkMatcher = OPENJDK_MAIL_URL_PATTERN.matcher(url);
            boolean isOpenjdkMailUrl = openjdkMatcher.matches();

            String replacement;
            if (insideMarkdownLink) {
                // Already inside markdown link syntax - just output the URL
                replacement = finalUrl;
            } else if (rewriteLinks && isOpenjdkMailUrl) {
                // For HTML render: create a proper link with shortened display text
                // e.g., "hotspot-gc-dev/2026-January/056951.html"
                String shortDisplay = openjdkMatcher.group(1) + "/" + openjdkMatcher.group(2) + "/" + openjdkMatcher.group(3) + ".html";
                replacement = "[" + shortDisplay + "](" + finalUrl + ")";
            } else if (text.equals(url) || text.startsWith("http")) {
                // Text is the URL itself - output as-is (for markdown, keep original URL)
                replacement = finalUrl;
            } else {
                // Create a full markdown link with original text
                replacement = "[" + text + "](" + finalUrl + ")";
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

        // Replace non-breaking spaces with regular spaces FIRST
        // This must happen before blockquote normalization so all spaces are treated uniformly
        content = content.replace('\u00A0', ' ');

        // Normalize blockquote lines: ensure > is followed by a space
        // This is required for proper markdown parsing
        content = content.replaceAll("(?m)^>(\\S)", "> $1");    // >text -> > text
        // Note: We don't add extra spaces for indentation anymore since we convert
        // to fenced code blocks which don't require the extra space

        // Remove attachment notices (lines starting with "-------------- next part")
        content = content.replaceAll("(?m)^-{10,} next part.*(?:\\r?\\n.*)*$", "");

        // Convert "----- Original Message -----" to a styled separator
        // Uses Unicode box-drawing characters for a clean titled border look
        // Also handles nested blockquotes like "> ----- Original Message -----"
        // Don't add leading newline for blockquotes as it breaks continuity
        Pattern originalMsgPattern = Pattern.compile(
                "(?m)^((?:> ?)*)-{3,}\\s*(Original Message|Forwarded Message)\\s*-{3,}$");
        content = originalMsgPattern.matcher(content).replaceAll(match -> {
            String prefix = match.group(1);
            String msgType = match.group(2);
            // Only add leading newline if not in a blockquote
            // Don't add trailing newline - the original newline is preserved ($ doesn't consume it)
            String leadingNewline = (prefix == null || prefix.isEmpty()) ? "\n" : "";
            return leadingNewline + (prefix != null ? prefix : "") + "**───── " + msgType + " ─────**";
        });

        // Strip extra indentation from email header blocks within blockquotes
        // Email clients often indent forwarded/quoted headers with 4+ spaces, which
        // markdown would interpret as code blocks. We strip this indentation.
        content = stripEmailHeaderIndentation(content);

        // Convert lightly-indented code lines (2-3 spaces) to proper code blocks (4 spaces)
        // This handles cases like "  case Point(0, 0) -> ..." which would otherwise
        // be merged into the paragraph above
        content = convertLightlyIndentedCodeToBlocks(content);

        // Insert blank lines when transitioning out of blockquotes
        // CommonMark uses "lazy continuation" - blockquotes continue until a blank line
        // Without this, text after a blockquote gets included in the blockquote
        content = addBlankLinesAfterBlockquotes(content);

        // Join lines that were artificially wrapped by pipermail's line length limit
        // This handles orphan short fragments that got pushed to the next line
        content = joinPipermailWrappedLines(content);

        // Fix orphan continuation lines from email wrapping in code blocks and lists
        // When email wrapping breaks a line mid-code-block, the continuation starts at column 0,
        // which prematurely terminates the code block. Re-indent single orphan lines.
        content = fixOrphanContinuationLines(content);

        // Strip trailing whitespace from each line
        content = content.lines()
                .map(String::stripTrailing)
                .reduce((a, b) -> a + "\n" + b)
                .orElse("");

        // Remove excessive blank lines (more than 2 consecutive)
        content = content.replaceAll("\n{3,}", "\n\n");

        // Convert indented code blocks to fenced code blocks
        content = convertIndentedToFencedCodeBlocks(content);

        // Detect and fence code inside list items (using looksLikeCode heuristic)
        content = convertListItemCodeToFenced(content);

        // Detect and fence code blocks at column 0 (no indentation)
        // This handles email-wrapped code that lost its indentation
        content = convertColumnZeroCodeToFenced(content);

        // Trim and ensure single trailing newline for consistency
        content = content.trim();
        if (!content.isEmpty()) {
            content = content + "\n";
        }
        return content;
    }

    /**
     * Convert indented email blocks within blockquotes to nested blockquotes.
     * <p>
     * Email clients often indent forwarded/quoted message headers AND body with 4+ spaces:
     * <pre>
     * >     *From: *"Viktor Klang"
     * >     *To: *"Someone"
     * >
     * >     Just a quick note...
     * >
     * > Hello Viktor, (response - less indent)
     * >
     * >     This is another quote (back to email indent level)
     * </pre>
     * The indentation level of the email header defines what belongs to that email.
     * Content at that indent level becomes nested blockquotes.
     * Content at lower indent is kept at outer level, but we continue tracking
     * to capture subsequent content at the email indent level.
     */
    private String stripEmailHeaderIndentation(String content) {
        String[] lines = content.split("\n", -1);
        StringBuilder result = new StringBuilder();
        boolean inEmailContext = false;     // Have we seen an email header at this blockquote level?
        String currentPrefix = "";
        int emailIndentLevel = 0;           // Indentation level that defines the email block

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];

            // Check if this is a blockquoted line
            if (line.startsWith(">")) {
                Matcher matcher = BLOCKQUOTE_PREFIX_PATTERN.matcher(line);
                if (matcher.find()) {
                    String prefix = matcher.group(1);
                    String rest = line.substring(prefix.length());

                    // Count leading spaces for indentation
                    int indentSpaces = 0;
                    for (char c : rest.toCharArray()) {
                        if (c == ' ') indentSpaces++;
                        else break;
                    }
                    String trimmedRest = rest.trim();

                    // Skip code-like lines entirely - they should pass through unchanged
                    // and be handled by later code block detection methods
                    if (looksLikeCode(trimmedRest) && !looksLikeEmailHeader(trimmedRest)) {
                        result.append(line);
                        if (i < lines.length - 1) {
                            result.append("\n");
                        }
                        continue;
                    }

                    // Check if this line starts an email header block (detected by header pattern)
                    if (!inEmailContext && indentSpaces >= 4 && looksLikeEmailHeader(trimmedRest)) {
                        inEmailContext = true;
                        currentPrefix = prefix;
                        emailIndentLevel = indentSpaces;
                        // Convert to nested blockquote
                        result.append(prefix).append(" > ").append(trimmedRest);
                        if (i < lines.length - 1) {
                            result.append("\n");
                        }
                        continue;
                    }

                    // If we have email context at this prefix level, handle indentation
                    if (inEmailContext && prefix.replace(" ", "").equals(currentPrefix.replace(" ", ""))) {
                        // Blank line - keep at current nesting
                        if (trimmedRest.isEmpty()) {
                            // Check if next non-blank line is at email indent level
                            // If so, this blank is part of nested content
                            if (hasMoreIndentedContent(lines, i + 1, prefix, emailIndentLevel)) {
                                result.append(prefix).append(" >");
                            } else {
                                result.append(prefix);
                            }
                            if (i < lines.length - 1) {
                                result.append("\n");
                            }
                            continue;
                        }

                        // Content at or deeper than email indent level = nested blockquote
                        if (indentSpaces >= emailIndentLevel) {
                            // Check if this looks like code - if so, preserve original line
                            // for later code block detection (don't convert to nested blockquote)
                            if (looksLikeCode(trimmedRest) && !looksLikeEmailHeader(trimmedRest)) {
                                // Keep original line - code block handling will process it later
                                result.append(line);
                                if (i < lines.length - 1) {
                                    result.append("\n");
                                }
                                continue;
                            }

                            // Non-code content: calculate nesting depth relative to email indent
                            int extraLevels = (indentSpaces - emailIndentLevel) / 4;
                            String nestedPrefix = " >".repeat(Math.max(0, extraLevels));
                            result.append(prefix).append(" >").append(nestedPrefix).append(" ").append(trimmedRest);
                            if (i < lines.length - 1) {
                                result.append("\n");
                            }
                            continue;
                        }
                        // Less indentation = outer level response, output as-is
                        // But keep email context active for subsequent indented content
                    }

                    // Different blockquote prefix = end email context
                    if (inEmailContext && !prefix.replace(" ", "").equals(currentPrefix.replace(" ", ""))) {
                        inEmailContext = false;
                    }
                }
            } else {
                // Non-blockquoted line ends email context
                inEmailContext = false;
            }

            // Default: output line as-is
            result.append(line);
            if (i < lines.length - 1) {
                result.append("\n");
            }
        }

        return result.toString();
    }

    /**
     * Check if there's more content at the email indent level in upcoming lines.
     */
    private boolean hasMoreIndentedContent(String[] lines, int startIndex, String expectedPrefix, int emailIndentLevel) {
        String normalizedExpected = expectedPrefix.replace(" ", "");
        for (int i = startIndex; i < lines.length && i < startIndex + 5; i++) {
            String line = lines[i];
            if (!line.startsWith(">")) {
                return false;
            }
            Matcher matcher = BLOCKQUOTE_PREFIX_PATTERN.matcher(line);
            if (!matcher.find()) {
                return false;
            }
            String prefix = matcher.group(1);
            if (!prefix.replace(" ", "").equals(normalizedExpected)) {
                return false;
            }
            String rest = line.substring(prefix.length());
            if (rest.trim().isEmpty()) {
                continue;
            }
            int indentSpaces = 0;
            for (char c : rest.toCharArray()) {
                if (c == ' ') indentSpaces++;
                else break;
            }
            return indentSpaces >= emailIndentLevel;
        }
        return false;
    }

    /**
     * Check if a line looks like a continuation of an email header.
     * This handles wrapped header values like email addresses on their own line.
     */
    private boolean looksLikeEmailHeaderContinuation(String line) {
        // Lines that look like email address continuations: <email@domain.com>
        if (line.matches("^<.*>$") || line.matches("^<.*@.*>$")) {
            return true;
        }
        // Lines with email-like content (contains @, angle brackets with content)
        if (line.contains(" at ") && (line.contains("<") || line.contains(">"))) {
            return true;
        }
        // Lines that look like quoted header content (quoted name + angle bracket email)
        if (line.matches("^\"[^\"]+\"\\s*<.*") || line.matches(".*>\\s*$")) {
            return true;
        }
        return false;
    }

    /**
     * Convert indented code blocks (4+ space indentation) to fenced code blocks (```).
     * <p>
     * This handles:
     * <ul>
     *   <li>Regular code blocks (4 space indent)</li>
     *   <li>Code blocks inside list items (preserves list indentation)</li>
     *   <li>Code blocks inside blockquotes (preserves > prefix)</li>
     * </ul>
     * <p>
     * Content inside existing fenced code blocks is left untouched.
     * Indented lines inside list items (continuations) are not converted.
     */
    private String convertIndentedToFencedCodeBlocks(String content) {
        String[] lines = content.split("\n", -1);
        StringBuilder result = new StringBuilder();
        StringBuilder codeBlock = new StringBuilder();
        String codeBlockPrefix = "";  // Prefix for list/blockquote context
        boolean inIndentedCodeBlock = false;
        boolean inExistingFencedBlock = false;
        boolean inListContext = false;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String trimmed = line.trim();

            // Track existing fenced code blocks - don't modify content inside them
            if (trimmed.startsWith("```")) {
                inExistingFencedBlock = !inExistingFencedBlock;
                // If we were building an indented code block, close it first
                if (inIndentedCodeBlock) {
                    result.append(codeBlock);
                    result.append(codeBlockPrefix).append("```\n");
                    codeBlock.setLength(0);
                    inIndentedCodeBlock = false;
                    codeBlockPrefix = "";
                }
                result.append(line);
                if (i < lines.length - 1) {
                    result.append("\n");
                }
                continue;
            }

            // If inside an existing fenced block, pass through unchanged
            if (inExistingFencedBlock) {
                result.append(line);
                if (i < lines.length - 1) {
                    result.append("\n");
                }
                continue;
            }

            // Track list context - list items start with - * or number.
            // We're in a list until we see a blank line or non-indented non-list line
            boolean isListItem = trimmed.matches("^([-*]|\\d+\\.)\\s.*");
            if (isListItem) {
                inListContext = true;
            } else if (trimmed.isEmpty()) {
                // Blank line might end list context, but could also be between items
                // Keep list context if next non-blank line is a list item or indented
            } else if (!line.startsWith(" ") && !line.startsWith("\t")) {
                // Non-indented, non-list line ends list context
                inListContext = false;
            }

            // Check if this line is an indented code line
            // Skip if we're in a list context (it's likely a list continuation)
            IndentedCodeInfo codeInfo = null;
            if (!inListContext) {
                codeInfo = getIndentedCodeInfo(line);
            }

            if (codeInfo != null) {
                if (!inIndentedCodeBlock) {
                    // Starting a new code block
                    inIndentedCodeBlock = true;
                    codeBlockPrefix = codeInfo.prefix;

                    // For blockquote code: check if previous line(s) should be included
                    // (non-indented lines that look like code, like "interface Foo {")
                    StringBuilder priorCodeLines = new StringBuilder();
                    if (!codeBlockPrefix.isEmpty()) {
                        priorCodeLines = pullBackNonIndentedCodeLines(result, codeBlockPrefix);
                    }

                    // Ensure blank line before code block if previous line wasn't blank
                    // BUT NOT for blockquote code - blank lines break blockquote continuity
                    if (codeBlockPrefix.isEmpty() && result.length() > 0) {
                        String resultStr = result.toString();
                        if (!resultStr.endsWith("\n\n") && !resultStr.endsWith("\n")) {
                            result.append("\n");
                        }
                        if (!resultStr.endsWith("\n\n")) {
                            result.append("\n");
                        }
                    }
                    // Add opening fence with same prefix (add space after > for readability)
                    result.append(formatBlockquotePrefix(codeBlockPrefix)).append("```\n");
                    // Add any prior code lines we pulled back
                    if (priorCodeLines.length() > 0) {
                        codeBlock.append(priorCodeLines);
                    }
                }
                // Add code line with prefix but without the code indentation
                codeBlock.append(formatBlockquotePrefix(codeBlockPrefix)).append(codeInfo.code).append("\n");
            } else {
                // Check if this is a blank line within a code block
                // For blockquotes: a line like "> > >" (just the prefix) should continue the code block
                // For regular code: a blank/whitespace-only line should continue if more code follows
                boolean isBlankBlockquoteLine = inIndentedCodeBlock &&
                        !codeBlockPrefix.isEmpty() &&
                        isBlankBlockquoteLineWithPrefix(line, codeBlockPrefix);
                boolean isBlankLineInRegularCode = inIndentedCodeBlock &&
                        codeBlockPrefix.isEmpty() &&
                        trimmed.isEmpty();

                if (isBlankBlockquoteLine && hasMoreCodeWithPrefix(lines, i + 1, codeBlockPrefix)) {
                    // Include blank line in code block
                    codeBlock.append(formatBlockquotePrefix(codeBlockPrefix)).append("\n");
                } else if (isBlankLineInRegularCode && hasMoreIndentedCode(lines, i + 1)) {
                    // Include blank line in regular code block
                    codeBlock.append("\n");
                } else if (inIndentedCodeBlock) {
                    // Ending code block - but first check if this line should be included
                    // (non-indented line that looks like code, like a closing "}")
                    boolean includeThisLine = false;
                    if (!codeBlockPrefix.isEmpty()) {
                        String contentAfterPrefix = extractContentAfterBlockquotePrefix(line, codeBlockPrefix);
                        if (contentAfterPrefix != null && !contentAfterPrefix.trim().isEmpty() &&
                                looksLikeCode(contentAfterPrefix.trim())) {
                            includeThisLine = true;
                        }
                    }

                    if (includeThisLine) {
                        // Include this line in the code block and continue
                        String contentAfterPrefix = extractContentAfterBlockquotePrefix(line, codeBlockPrefix);
                        codeBlock.append(formatBlockquotePrefix(codeBlockPrefix))
                                .append(contentAfterPrefix.trim()).append("\n");
                    } else {
                        // Actually ending the code block
                        result.append(stripMinimumIndentation(codeBlock.toString(), codeBlockPrefix));
                        result.append(formatBlockquotePrefix(codeBlockPrefix)).append("```\n");
                        codeBlock.setLength(0);
                        inIndentedCodeBlock = false;
                        codeBlockPrefix = "";
                        result.append(line);
                        if (i < lines.length - 1) {
                            result.append("\n");
                        }
                    }
                } else {
                    result.append(line);
                    if (i < lines.length - 1) {
                        result.append("\n");
                    }
                }
            }
        }

        // Close any remaining code block
        if (inIndentedCodeBlock) {
            result.append(stripMinimumIndentation(codeBlock.toString(), codeBlockPrefix));
            result.append(formatBlockquotePrefix(codeBlockPrefix)).append("```");
        }

        return result.toString();
    }

    /**
     * Format a blockquote prefix for output, ensuring proper spacing.
     * Adds a trailing space if the prefix is non-empty and doesn't already have one.
     */
    private String formatBlockquotePrefix(String prefix) {
        if (prefix.isEmpty()) {
            return prefix;
        }
        // Add space after > for readability if not already present
        if (!prefix.endsWith(" ")) {
            return prefix + " ";
        }
        return prefix;
    }

    /**
     * Pull back non-indented lines from the result that look like code.
     * This handles cases like:
     * <pre>
     * > interface Foo {           # non-indented, but looks like code
     * >   void method();          # indented - starts code block detection
     * </pre>
     * When we detect the indented code, we look back and pull "interface Foo {" into the code block.
     */
    private StringBuilder pullBackNonIndentedCodeLines(StringBuilder result, String expectedPrefix) {
        StringBuilder pulledLines = new StringBuilder();
        String normalizedExpected = expectedPrefix.replace(" ", "");

        // Work backwards through result to find code-like lines to pull
        String resultStr = result.toString();
        String[] resultLines = resultStr.split("\n", -1);

        int pullFromIndex = resultLines.length;
        for (int i = resultLines.length - 1; i >= 0; i--) {
            String line = resultLines[i];

            // Skip empty lines at the end
            if (line.trim().isEmpty() && i == resultLines.length - 1) {
                pullFromIndex = i;
                continue;
            }

            // Check if this is a blockquote line with matching prefix
            if (!line.startsWith(">")) {
                break;
            }

            Matcher matcher = BLOCKQUOTE_PREFIX_PATTERN.matcher(line);
            if (!matcher.find()) {
                break;
            }

            String prefix = matcher.group(1);
            String normalizedPrefix = prefix.replace(" ", "");
            if (!normalizedPrefix.equals(normalizedExpected)) {
                break;
            }

            String rest = line.substring(prefix.length());
            // Strip optional space after prefix
            if (rest.startsWith(" ")) {
                rest = rest.substring(1);
            }

            // Check if this is a non-indented line that looks like code
            if (!rest.startsWith(" ") && !rest.trim().isEmpty() && looksLikeCode(rest.trim())) {
                pullFromIndex = i;
            } else {
                // Hit a non-code line, stop looking
                break;
            }
        }

        // Pull the identified lines
        if (pullFromIndex < resultLines.length) {
            // Rebuild result without the pulled lines
            StringBuilder newResult = new StringBuilder();
            for (int i = 0; i < pullFromIndex; i++) {
                newResult.append(resultLines[i]);
                if (i < pullFromIndex - 1) {
                    newResult.append("\n");
                }
            }
            // Add trailing newline if there was one
            if (pullFromIndex > 0) {
                newResult.append("\n");
            }
            result.setLength(0);
            result.append(newResult);

            // Build the pulled lines for the code block
            for (int i = pullFromIndex; i < resultLines.length; i++) {
                String line = resultLines[i];
                if (line.trim().isEmpty()) {
                    continue; // Skip empty lines
                }
                Matcher matcher = BLOCKQUOTE_PREFIX_PATTERN.matcher(line);
                if (matcher.find()) {
                    String prefix = matcher.group(1);
                    String rest = line.substring(prefix.length());
                    if (rest.startsWith(" ")) {
                        rest = rest.substring(1);
                    }
                    pulledLines.append(formatBlockquotePrefix(expectedPrefix)).append(rest.trim()).append("\n");
                }
            }
        }

        return pulledLines;
    }

    /**
     * Extract the content after a blockquote prefix, if the line matches the expected prefix.
     * Returns null if the line doesn't match the expected prefix pattern.
     */
    private String extractContentAfterBlockquotePrefix(String line, String expectedPrefix) {
        if (!line.startsWith(">")) {
            return null;
        }

        Matcher matcher = BLOCKQUOTE_PREFIX_PATTERN.matcher(line);
        if (!matcher.find()) {
            return null;
        }

        String prefix = matcher.group(1);
        String normalizedPrefix = prefix.replace(" ", "");
        String normalizedExpected = expectedPrefix.replace(" ", "");

        if (!normalizedPrefix.equals(normalizedExpected)) {
            return null;
        }

        String rest = line.substring(prefix.length());
        // Strip optional space after prefix
        if (rest.startsWith(" ")) {
            rest = rest.substring(1);
        }

        return rest;
    }

    /**
     * Detect and fence code inside list items using the looksLikeCode heuristic.
     * <p>
     * Handles cases like:
     * <pre>
     * - you mutate variables when you reduce accumulators in a loop
     *   var v1 = ...
     *   for(...) {
     *     (v1, v2) = f(v1, v2);
     *   }
     * </pre>
     * Where the code is indented as list continuation but should be a code block.
     */
    private String convertListItemCodeToFenced(String content) {
        String[] lines = content.split("\n", -1);
        StringBuilder result = new StringBuilder();
        StringBuilder codeBlock = new StringBuilder();
        boolean inListItem = false;
        boolean inCodeSection = false;
        String listIndent = "";  // Indentation to use for fenced block
        int codeBaseIndent = 0;  // Base indentation of the code block (first code line's indent)

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String trimmed = line.trim();

            // Detect list item start
            if (trimmed.matches("^([-*]|\\d+\\.)\\s.*")) {
                // Close any open code section
                if (inCodeSection) {
                    result.append(listIndent).append("```\n");
                    result.append(codeBlock);
                    result.append(listIndent).append("```\n");
                    codeBlock.setLength(0);
                    inCodeSection = false;
                }
                inListItem = true;
                // Calculate the indentation for code blocks (align with list text)
                int markerEnd = line.indexOf(trimmed) + trimmed.indexOf(' ') + 1;
                listIndent = " ".repeat(Math.max(2, line.indexOf(trimmed.charAt(0)) + 2));
                result.append(line);
                if (i < lines.length - 1) {
                    result.append("\n");
                }
                continue;
            }

            // Check if we're still in a list context
            if (inListItem) {
                // Blank line might end list or separate items
                if (trimmed.isEmpty()) {
                    if (inCodeSection) {
                        result.append(listIndent).append("```\n");
                        result.append(codeBlock);
                        result.append(listIndent).append("```\n");
                        codeBlock.setLength(0);
                        inCodeSection = false;
                    }
                    result.append(line);
                    if (i < lines.length - 1) {
                        result.append("\n");
                    }
                    continue;
                }

                // Non-indented, non-list line ends list context
                if (!line.startsWith(" ") && !line.startsWith("\t")) {
                    if (inCodeSection) {
                        result.append(listIndent).append("```\n");
                        result.append(codeBlock);
                        result.append(listIndent).append("```\n");
                        codeBlock.setLength(0);
                        inCodeSection = false;
                    }
                    inListItem = false;
                    result.append(line);
                    if (i < lines.length - 1) {
                        result.append("\n");
                    }
                    continue;
                }

                // Calculate line's indentation
                int lineIndent = 0;
                while (lineIndent < line.length() && line.charAt(lineIndent) == ' ') {
                    lineIndent++;
                }

                // Indented line - check if it looks like code
                if (looksLikeCode(trimmed)) {
                    if (!inCodeSection) {
                        // Starting code section - add blank line and opening fence
                        if (result.length() > 0 && !result.toString().endsWith("\n\n")) {
                            if (!result.toString().endsWith("\n")) {
                                result.append("\n");
                            }
                        }
                        inCodeSection = true;
                        codeBaseIndent = lineIndent;  // Record base indentation
                    }
                    // Preserve relative indentation: listIndent + (lineIndent - codeBaseIndent) spaces + trimmed
                    int relativeIndent = Math.max(0, lineIndent - codeBaseIndent);
                    codeBlock.append(listIndent).append(" ".repeat(relativeIndent)).append(trimmed).append("\n");
                    continue;
                } else if (inCodeSection) {
                    // Non-code line while in code section
                    // Check if it might be part of the code (like a closing brace or continuation)
                    if (trimmed.matches("^[}\\]);]+$") || trimmed.startsWith("//")) {
                        int relativeIndent = Math.max(0, lineIndent - codeBaseIndent);
                        codeBlock.append(listIndent).append(" ".repeat(relativeIndent)).append(trimmed).append("\n");
                        continue;
                    }
                    // End code section
                    result.append(listIndent).append("```\n");
                    result.append(codeBlock);
                    result.append(listIndent).append("```\n");
                    codeBlock.setLength(0);
                    inCodeSection = false;
                }
            }

            result.append(line);
            if (i < lines.length - 1) {
                result.append("\n");
            }
        }

        // Close any remaining code section
        if (inCodeSection) {
            result.append(listIndent).append("```\n");
            result.append(codeBlock);
            result.append(listIndent).append("```");
        }

        return result.toString();
    }

    /**
     * Detect and fence code blocks at column 0 (no indentation) or at column 0 within blockquotes.
     * <p>
     * This handles cases where code was pasted into an email without indentation,
     * or where email wrapping caused code to lose its indentation. We look for
     * consecutive lines that look like code and wrap them in fenced blocks.
     * <p>
     * Also handles blockquoted code like "> interface Foo {" where the code starts
     * immediately after the blockquote prefix without additional indentation.
     * <p>
     * Requires at least 2 consecutive code-like lines to avoid false positives.
     * Also skips lines that are already inside fenced code blocks.
     */
    private String convertColumnZeroCodeToFenced(String content) {
        String[] lines = content.split("\n", -1);
        StringBuilder result = new StringBuilder();
        StringBuilder codeBlock = new StringBuilder();
        boolean inExistingFencedBlock = false;
        int consecutiveCodeLines = 0;
        String currentBlockquotePrefix = null;  // Track blockquote context

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String trimmed = line.trim();

            // Check for blockquoted lines - extract prefix and content FIRST
            String prefix = "";
            String contentAfterPrefix = line;
            if (line.startsWith(">")) {
                Matcher matcher = BLOCKQUOTE_PREFIX_PATTERN.matcher(line);
                if (matcher.find()) {
                    prefix = matcher.group(1);
                    contentAfterPrefix = line.substring(prefix.length());
                    // Strip one optional space after the prefix
                    if (contentAfterPrefix.startsWith(" ")) {
                        contentAfterPrefix = contentAfterPrefix.substring(1);
                    }
                }
            }

            // Track existing fenced code blocks - don't modify content inside them
            // Handle fences both at start of line and after blockquote prefixes
            String contentForFenceCheck = contentAfterPrefix.trim();
            if (contentForFenceCheck.startsWith("```")) {
                // Flush any pending code block first
                flushCodeBlock(result, codeBlock, consecutiveCodeLines, currentBlockquotePrefix);
                codeBlock.setLength(0);
                consecutiveCodeLines = 0;
                currentBlockquotePrefix = null;

                inExistingFencedBlock = !inExistingFencedBlock;
                result.append(line);
                if (i < lines.length - 1) {
                    result.append("\n");
                }
                continue;
            }

            // Pass through content inside existing fenced blocks
            if (inExistingFencedBlock) {
                result.append(line);
                if (i < lines.length - 1) {
                    result.append("\n");
                }
                continue;
            }

            // Handle indented lines (but NOT list items - they should remain as markdown lists)
            boolean isListItem = contentAfterPrefix.trim().matches("^([-*]|\\d+\\.)\\s.*");
            boolean isIndented = contentAfterPrefix.startsWith("    ") || contentAfterPrefix.startsWith("\t");

            // List items should never be treated as code - skip them entirely
            if (isListItem) {
                flushCodeBlock(result, codeBlock, consecutiveCodeLines, currentBlockquotePrefix);
                codeBlock.setLength(0);
                consecutiveCodeLines = 0;
                currentBlockquotePrefix = null;
                result.append(line);
                if (i < lines.length - 1) {
                    result.append("\n");
                }
                continue;
            }

            if (isIndented) {
                String trimmedContent = contentAfterPrefix.trim();
                String normalizedPrefix = prefix.replace(" ", "");
                String currentNormalized = currentBlockquotePrefix != null ?
                        currentBlockquotePrefix.replace(" ", "") : "";

                // If we're already building a code block with the same prefix, include this indented line
                // (even if it doesn't look like code - indentation within a block is a strong signal)
                if (consecutiveCodeLines > 0 && normalizedPrefix.equals(currentNormalized)) {
                    codeBlock.append(line).append("\n");
                    consecutiveCodeLines++;
                    continue;
                }

                // If this indented line looks like code, start a new code block
                if (looksLikeCode(trimmedContent)) {
                    flushCodeBlock(result, codeBlock, consecutiveCodeLines, currentBlockquotePrefix);
                    codeBlock.setLength(0);
                    currentBlockquotePrefix = prefix;
                    codeBlock.append(line).append("\n");
                    consecutiveCodeLines = 1;
                    continue;
                }

                // Non-code indented line and not in a code block: flush and skip
                flushCodeBlock(result, codeBlock, consecutiveCodeLines, currentBlockquotePrefix);
                codeBlock.setLength(0);
                consecutiveCodeLines = 0;
                currentBlockquotePrefix = null;

                result.append(line);
                if (i < lines.length - 1) {
                    result.append("\n");
                }
                continue;
            }

            // Check if content looks like code (at column 0 or right after blockquote prefix)
            // But never treat list items as code, even if their content matches code patterns
            String trimmedContent = contentAfterPrefix.trim();
            boolean isColumnZeroListItem = trimmedContent.matches("^([-*]|\\d+\\.)\\s.*");
            if (!trimmedContent.isEmpty() && !isColumnZeroListItem && looksLikeCode(trimmedContent)) {
                // Check if we're continuing in the same blockquote context
                String normalizedPrefix = prefix.replace(" ", "");
                String currentNormalized = currentBlockquotePrefix != null ?
                        currentBlockquotePrefix.replace(" ", "") : "";

                if (currentBlockquotePrefix == null || normalizedPrefix.equals(currentNormalized)) {
                    if (currentBlockquotePrefix == null) {
                        currentBlockquotePrefix = prefix;
                    }
                    consecutiveCodeLines++;
                    codeBlock.append(line).append("\n");
                } else {
                    // Different blockquote context - flush and start new
                    flushCodeBlock(result, codeBlock, consecutiveCodeLines, currentBlockquotePrefix);
                    codeBlock.setLength(0);
                    consecutiveCodeLines = 1;
                    currentBlockquotePrefix = prefix;
                    codeBlock.append(line).append("\n");
                }
            } else if (trimmedContent.isEmpty() && consecutiveCodeLines > 0) {
                // Blank line (or blank blockquote line) within potential code block
                // Check for more code (including indented) to continue the block
                if (hasMoreCodeWithPrefix(lines, i + 1, currentBlockquotePrefix, true)) {
                    codeBlock.append(line).append("\n");
                } else {
                    // End of code block
                    flushCodeBlock(result, codeBlock, consecutiveCodeLines, currentBlockquotePrefix);
                    codeBlock.setLength(0);
                    consecutiveCodeLines = 0;
                    currentBlockquotePrefix = null;

                    result.append(line);
                    if (i < lines.length - 1) {
                        result.append("\n");
                    }
                }
            } else {
                // Non-code line - flush any pending code block
                flushCodeBlock(result, codeBlock, consecutiveCodeLines, currentBlockquotePrefix);
                codeBlock.setLength(0);
                consecutiveCodeLines = 0;
                currentBlockquotePrefix = null;

                result.append(line);
                if (i < lines.length - 1) {
                    result.append("\n");
                }
            }
        }

        // Flush any remaining code block
        if (consecutiveCodeLines >= 2 && codeBlock.length() > 0) {
            String fencePrefix = currentBlockquotePrefix != null ? formatBlockquotePrefix(currentBlockquotePrefix) : "";
            String prefix = currentBlockquotePrefix != null ? currentBlockquotePrefix : "";
            result.append(fencePrefix).append("```\n");
            result.append(stripMinimumIndentation(codeBlock.toString(), prefix));
            result.append(fencePrefix).append("```");
        } else if (codeBlock.length() > 0) {
            result.append(codeBlock.toString().stripTrailing());
        }

        return result.toString();
    }

    /**
     * Helper to flush a code block to the result buffer.
     */
    private void flushCodeBlock(StringBuilder result, StringBuilder codeBlock,
                                int consecutiveCodeLines, String blockquotePrefix) {
        if (consecutiveCodeLines >= 2 && codeBlock.length() > 0) {
            String fencePrefix = blockquotePrefix != null ? formatBlockquotePrefix(blockquotePrefix) : "";
            String prefix = blockquotePrefix != null ? blockquotePrefix : "";
            result.append(fencePrefix).append("```\n");
            result.append(stripMinimumIndentation(codeBlock.toString(), prefix));
            result.append(fencePrefix).append("```\n");
        } else if (codeBlock.length() > 0) {
            result.append(codeBlock);
        }
    }

    /**
     * Check if there's more column-0 code-like content in upcoming lines with the same prefix.
     */
    private boolean hasMoreColumnZeroCodeWithPrefix(String[] lines, int startIndex, String expectedPrefix) {
        return hasMoreCodeWithPrefix(lines, startIndex, expectedPrefix, false);
    }

    private boolean hasMoreCodeWithPrefix(String[] lines, int startIndex, String expectedPrefix, boolean includeIndented) {
        String normalizedExpected = expectedPrefix != null ? expectedPrefix.replace(" ", "") : "";

        for (int i = startIndex; i < lines.length; i++) {
            String line = lines[i];

            // Extract prefix and content
            String prefix = "";
            String contentAfterPrefix = line;
            if (line.startsWith(">")) {
                Matcher matcher = BLOCKQUOTE_PREFIX_PATTERN.matcher(line);
                if (matcher.find()) {
                    prefix = matcher.group(1);
                    contentAfterPrefix = line.substring(prefix.length());
                    if (contentAfterPrefix.startsWith(" ")) {
                        contentAfterPrefix = contentAfterPrefix.substring(1);
                    }
                }
            }

            String trimmedContent = contentAfterPrefix.trim();

            // Skip blank lines with matching prefix
            if (trimmedContent.isEmpty()) {
                String normalizedPrefix = prefix.replace(" ", "");
                if (normalizedPrefix.equals(normalizedExpected)) {
                    continue;
                }
                return false;
            }

            // Check if prefix matches
            String normalizedPrefix = prefix.replace(" ", "");
            if (!normalizedPrefix.equals(normalizedExpected)) {
                return false;
            }

            // Check for indented code (if allowed)
            boolean isIndented = contentAfterPrefix.startsWith("    ") || contentAfterPrefix.startsWith("\t");
            if (isIndented && includeIndented && looksLikeCode(trimmedContent)) {
                return true;
            }

            // Check for column-zero code
            if (!isIndented &&
                    !trimmedContent.matches("^([-*]|\\d+\\.)\\s.*") &&
                    looksLikeCode(trimmedContent)) {
                return true;
            }

            return false;
        }
        return false;
    }

    /**
     * Information about an indented code line.
     */
    private record IndentedCodeInfo(String prefix, String code) {}

    // Pattern to match blockquote prefixes like ">", "> >", ">>", "> > >", etc.
    // Uses ">(?:[ ]?>)*" to avoid greedily consuming trailing space before code indent
    private static final Pattern BLOCKQUOTE_PREFIX_PATTERN = Pattern.compile("^(>(?:[ ]?>)*)");

    // Pattern to detect email header lines that should NOT be treated as code
    // Matches lines like "*From: *" or "From:" at the start (with optional leading * for bold)
    // Common headers: From, To, Cc, Bcc, Subject, Sent, Date, Reply-To
    private static final Pattern EMAIL_HEADER_PATTERN = Pattern.compile(
            "^\\*?(From|To|Cc|Bcc|Subject|Sent|Date|Reply-To):\\s?\\*?",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * Check if a line is an indented code block line and extract its components.
     * Returns null if not an indented code line.
     * <p>
     * Handles nested blockquotes like "> > >    code" or ">>>    code".
     * Note: Blockquote normalization may add extra spaces, so we strip leading space from code.
     */
    private IndentedCodeInfo getIndentedCodeInfo(String line) {
        // Regular indented code block (4+ spaces)
        if (line.startsWith("    ")) {
            String code = line.substring(4);
            // Don't treat email headers as code (e.g., "*From: *Viktor Klang")
            if (looksLikeEmailHeader(code)) {
                return null;
            }
            return new IndentedCodeInfo("", code);
        }

        // Note: Blockquote code is NOT handled here.
        // All blockquote code detection is done by convertColumnZeroCodeToFenced()
        // which preserves the original line spacing for proper indentation within code blocks.

        return null;
    }

    /**
     * Check if a line looks like an email header (From, To, Cc, Subject, etc.)
     * These should not be treated as code even when indented.
     */
    private boolean looksLikeEmailHeader(String line) {
        String trimmed = line.trim();
        return EMAIL_HEADER_PATTERN.matcher(trimmed).find();
    }

    /**
     * Check if a line is a blank blockquote line matching the given prefix.
     * For example, "> > >" with prefix "> > " would return true.
     */
    private boolean isBlankBlockquoteLineWithPrefix(String line, String expectedPrefix) {
        if (!line.startsWith(">")) {
            return false;
        }
        // Normalize and compare: the line should be just blockquote markers
        String trimmed = line.trim();
        // Check if it's only > characters and spaces
        if (!trimmed.matches("^(>[ ]?)+$")) {
            return false;
        }
        // Check if it matches or is compatible with our prefix
        Matcher matcher = BLOCKQUOTE_PREFIX_PATTERN.matcher(line);
        if (matcher.find()) {
            String linePrefix = matcher.group(1);
            // The prefix should match (allowing for trailing space differences)
            return linePrefix.replace(" ", "").equals(expectedPrefix.replace(" ", ""));
        }
        return false;
    }

    /**
     * Check if there's more indented code with the same prefix in upcoming lines.
     */
    private boolean hasMoreCodeWithPrefix(String[] lines, int startIndex, String expectedPrefix) {
        for (int i = startIndex; i < lines.length; i++) {
            String line = lines[i];
            // Skip blank blockquote lines
            if (line.trim().matches("^(>[ ]?)*$")) {
                continue;
            }
            // Check if this line is code with matching prefix
            IndentedCodeInfo info = getIndentedCodeInfo(line);
            if (info != null) {
                // Check if prefix matches (allowing for space variations)
                return info.prefix.replace(" ", "").equals(expectedPrefix.replace(" ", ""));
            }
            // Non-blank, non-code line - no more code coming
            return false;
        }
        return false;
    }

    /**
     * Strip the minimum common indentation from all lines in a code block.
     * This preserves relative indentation while removing common leading whitespace.
     * <p>
     * For example, if all lines have at least 1 space of indentation, that space
     * is removed from all lines, preserving any additional indentation.
     *
     * @param codeBlock the code block content (may include blockquote prefixes)
     * @param blockquotePrefix the blockquote prefix (e.g., "> ") or empty string
     * @return the code block with minimum indentation stripped
     */
    private String stripMinimumIndentation(String codeBlock, String blockquotePrefix) {
        if (codeBlock.isEmpty()) {
            return codeBlock;
        }

        String[] lines = codeBlock.split("\n", -1);
        String formattedPrefix = formatBlockquotePrefix(blockquotePrefix);
        int prefixLen = formattedPrefix.length();

        // Find minimum indentation across all non-empty lines
        int minIndent = Integer.MAX_VALUE;
        for (String line : lines) {
            // Strip the blockquote prefix if present
            String content = line;
            if (!formattedPrefix.isEmpty() && line.startsWith(formattedPrefix)) {
                content = line.substring(prefixLen);
            } else if (!blockquotePrefix.isEmpty() && line.startsWith(blockquotePrefix)) {
                content = line.substring(blockquotePrefix.length());
            }

            // Skip empty lines when calculating minimum indent
            if (content.trim().isEmpty()) {
                continue;
            }

            // Count leading spaces
            int indent = 0;
            while (indent < content.length() && content.charAt(indent) == ' ') {
                indent++;
            }
            minIndent = Math.min(minIndent, indent);
        }

        // If no indentation to strip, return as-is
        if (minIndent == 0 || minIndent == Integer.MAX_VALUE) {
            return codeBlock;
        }

        // Strip minimum indentation from all lines
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];

            // Extract content after blockquote prefix
            String prefix = "";
            String content = line;
            if (!formattedPrefix.isEmpty() && line.startsWith(formattedPrefix)) {
                prefix = formattedPrefix;
                content = line.substring(prefixLen);
            } else if (!blockquotePrefix.isEmpty() && line.startsWith(blockquotePrefix)) {
                prefix = blockquotePrefix;
                content = line.substring(blockquotePrefix.length());
            }

            // Strip minimum indentation from content
            if (content.length() >= minIndent) {
                result.append(prefix).append(content.substring(minIndent));
            } else {
                result.append(prefix).append(content);
            }

            if (i < lines.length - 1) {
                result.append("\n");
            }
        }

        return result.toString();
    }

    /**
     * Check if there's more indented code (4+ spaces) in upcoming lines.
     * Used for regular (non-blockquote) code blocks.
     */
    private boolean hasMoreIndentedCode(String[] lines, int startIndex) {
        for (int i = startIndex; i < lines.length; i++) {
            String line = lines[i];
            // Skip blank/whitespace-only lines
            if (line.trim().isEmpty()) {
                continue;
            }
            // Check if this line is indented code (4+ spaces)
            return line.startsWith("    ");
        }
        return false;
    }

    // Pattern for detecting code-like syntax (operators, braces, etc.)
    // These are reliable indicators of code that rarely appear in prose
    // Note: `;$` (semicolon at end of line) was intentionally excluded because
    // prose can end with semicolons too (e.g., list items in discussions)
    // Note: `\(.*\)` was replaced with a more specific pattern that requires
    // parentheses to look like code (no spaces right after opening paren, or
    // contains code-like content like commas between identifiers)
    // Note: `--` requires context (not standalone) to avoid matching signature separators
    // Note: `//` uses negative lookbehind to exclude URLs (http:// https://)
    private static final Pattern CODE_SYNTAX_PATTERN = Pattern.compile(
            "->|=>|==|!=|<=|>=|&&|\\|\\||\\{|\\}|(?<!:)//|/\\*|\\*/|\\+\\+|\\w--(?!$)|(?<!^)--\\w"
    );

    // Pattern for code-like parentheses: method calls, type parameters, etc.
    // Matches: foo(), foo(x), foo(x, y), but NOT prose with parentheses like "itself (not the case)"
    // Key: NO space allowed between identifier and opening paren for method calls
    // Note: Excludes Big-O notation like O(n), O(n^2), O(a.length) which is common in algorithm discussions
    private static final Pattern CODE_PARENS_PATTERN = Pattern.compile(
            "(?![Oo]\\()[a-zA-Z_]\\w*\\([^)]*\\)|" +  // method call but NOT O() or o() - Big-O notation
            "\\([^)]*,\\s*[^)]*\\)|" +          // tuple/params: (a, b)
            "<[^>]+>\\s*\\(|" +                 // generics before paren: <T>(
            "\\)\\s*\\{|" +                     // ) followed by { : method signature
            "\\w+<[^>]+>"                       // generic type: List<String>
    );

    // Pattern for variable declarations: int x = ..., var y = ..., String s = ...
    private static final Pattern VARIABLE_DECLARATION_PATTERN = Pattern.compile(
            "^(int|long|double|float|boolean|char|byte|short|var|String|" +
            "\\w+(?:<[^>]+>)?)\\s+\\w+\\s*="    // type identifier = (with optional generics)
    );

    // Pattern for simple assignments: a = x; b = y; (identifier = expression;)
    private static final Pattern SIMPLE_ASSIGNMENT_PATTERN = Pattern.compile(
            "^\\w+\\s*=\\s*\\w+\\s*;\\s*$"     // identifier = identifier;
    );

    // Pattern for variable declarations without initializers: Type1 v1; String name;
    private static final Pattern DECLARATION_PATTERN = Pattern.compile(
            "^[A-Z]\\w*\\s+\\w+\\s*;\\s*$"     // TypeName identifier;
    );

    // Pattern for Java keywords - these alone are NOT enough to identify code
    // because prose about Java naturally uses words like "record", "class", "case"
    private static final Pattern CODE_KEYWORD_PATTERN = Pattern.compile(
            "\\b(case|switch|if|else|for|while|do|return|break|continue|class|interface|enum|record|" +
            "void|int|long|double|float|boolean|char|byte|short|var|final|static|public|private|protected|" +
            "new|null|true|false|this|super|throws?|try|catch|finally|instanceof|extends|implements)\\b"
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
     * <p>
     * Lines that look like list items (starting with - or * after indentation)
     * are excluded from this conversion.
     * <p>
     * To preserve relative indentation, when we enter a code region (2-3 space line),
     * we add an offset to ALL subsequent indented lines until the code region ends.
     */
    private String convertLightlyIndentedCodeToBlocks(String content) {
        String[] lines = content.split("\n", -1);
        StringBuilder result = new StringBuilder();
        int indentOffset = 0;  // Extra spaces to add to lines in a code region
        int codeRegionBaseIndent = 0;  // The starting indent level of the code region

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String prevLine = i > 0 ? lines[i - 1] : "";
            String trimmed = line.trim();

            // Count leading spaces
            int leadingSpaces = 0;
            while (leadingSpaces < line.length() && line.charAt(leadingSpaces) == ' ') {
                leadingSpaces++;
            }

            // Check for lines with 2-3 space indentation that look like code
            boolean isListItem = trimmed.matches("^([-*]|\\d+\\.)\\s.*");
            boolean is2to3SpaceCode = line.matches("^ {2,3}\\S.*") && !isListItem && looksLikeCode(trimmed);

            if (is2to3SpaceCode && indentOffset == 0) {
                // Starting a new code region
                codeRegionBaseIndent = leadingSpaces;
                indentOffset = 4 - leadingSpaces;  // How much to add to reach 4 spaces

                // Add blank line before code block if:
                // - Previous line is not blank
                // - Previous line is not already an indented code block (4+ spaces)
                if (!prevLine.isBlank() && !prevLine.startsWith("    ")) {
                    result.append("\n");
                }

                // Add the line with offset
                result.append(" ".repeat(leadingSpaces + indentOffset)).append(trimmed);
            } else if (indentOffset > 0) {
                // We're in a code region

                // Check if this line ends the code region:
                // - Non-indented, non-blank line that doesn't look like code
                // - Or a line with less indentation than the base (back to prose)
                boolean isBlankLine = trimmed.isEmpty();
                boolean isContinuedCode = leadingSpaces >= codeRegionBaseIndent ||
                        (leadingSpaces > 0 && looksLikeCode(trimmed));

                if (!isBlankLine && !isContinuedCode) {
                    // End of code region
                    indentOffset = 0;
                    codeRegionBaseIndent = 0;
                    result.append(line);
                } else if (isBlankLine) {
                    // Blank line in code region - check if code continues after
                    boolean moreCodeAhead = hasMoreLightlyIndentedCode(lines, i + 1, codeRegionBaseIndent);
                    if (moreCodeAhead) {
                        // Keep blank line in code block
                        result.append("");
                    } else {
                        // End of code region
                        indentOffset = 0;
                        codeRegionBaseIndent = 0;
                        result.append(line);
                    }
                } else {
                    // Continue code region - add offset to preserve relative indentation
                    result.append(" ".repeat(leadingSpaces + indentOffset)).append(trimmed);
                }
            } else {
                // Not in a code region
                result.append(line);
            }

            if (i < lines.length - 1) {
                result.append("\n");
            }
        }

        return result.toString();
    }

    /**
     * Check if there's more lightly-indented code in upcoming lines.
     */
    private boolean hasMoreLightlyIndentedCode(String[] lines, int startIndex, int baseIndent) {
        for (int i = startIndex; i < lines.length && i < startIndex + 3; i++) {
            String line = lines[i];
            if (line.trim().isEmpty()) {
                continue;
            }
            int spaces = 0;
            while (spaces < line.length() && line.charAt(spaces) == ' ') {
                spaces++;
            }
            // If line has indentation >= base and looks like code, there's more
            if (spaces >= baseIndent && looksLikeCode(line.trim())) {
                return true;
            }
            // If non-blank, non-code, no more code ahead
            return false;
        }
        return false;
    }

    // Pattern to match markdown links: [text](url)
    private static final Pattern MARKDOWN_LINK_PATTERN = Pattern.compile("\\[[^\\]]*\\]\\([^)]*\\)");

    /**
     * Heuristic check if a line looks like code based on common patterns.
     * <p>
     * A line is considered code if it has:
     * 1. Code syntax/operators (reliable indicators like ->, {}, ==, etc.), OR
     * 2. Code-like parentheses (method calls, type parameters, tuples)
     * <p>
     * Keywords alone are NOT enough because prose about Java naturally uses
     * words like "record", "class", "case".
     * <p>
     * Markdown links [text](url) are stripped before checking, as they contain
     * parentheses that would otherwise match the code pattern.
     */
    private boolean looksLikeCode(String line) {
        // Strip markdown links before checking - they contain () which would match code patterns
        String lineWithoutLinks = MARKDOWN_LINK_PATTERN.matcher(line).replaceAll("");

        // Check for definite code syntax (operators, braces)
        if (CODE_SYNTAX_PATTERN.matcher(lineWithoutLinks).find()) {
            return true;
        }

        // Check for code-like parentheses (method calls, generics, tuples)
        if (CODE_PARENS_PATTERN.matcher(lineWithoutLinks).find()) {
            return true;
        }

        // Check for variable declarations (int x = ..., var y = ...)
        if (VARIABLE_DECLARATION_PATTERN.matcher(lineWithoutLinks).find()) {
            return true;
        }

        // Check for simple assignments (a = x;)
        if (SIMPLE_ASSIGNMENT_PATTERN.matcher(lineWithoutLinks).find()) {
            return true;
        }

        // Check for variable declarations without initializers (Type1 v1;)
        if (DECLARATION_PATTERN.matcher(lineWithoutLinks).find()) {
            return true;
        }

        return false;
    }

    // Maximum length for an orphan fragment (short word/phrase pushed to next line)
    private static final int MAX_ORPHAN_LENGTH = 15;
    // Minimum length for a "long" line that likely hit the wrap limit
    private static final int MIN_LONG_LINE_LENGTH = 65;
    // Pattern for signature/greeting lines that should not be joined
    private static final Pattern SIGNATURE_PATTERN = Pattern.compile(
            "(?i).*(regards|cheers|thanks|thank you|best|sincerely|cordialement|greetings),?\\s*$");

    /**
     * Insert blank lines when transitioning from blockquoted text to non-blockquoted text.
     * <p>
     * In CommonMark, blockquotes use "lazy continuation" - a line without the > prefix
     * is still considered part of the blockquote unless there's a blank line. This method
     * ensures proper blockquote termination by inserting blank lines at transitions.
     */
    private String addBlankLinesAfterBlockquotes(String content) {
        String[] lines = content.split("\n", -1);
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < lines.length; i++) {
            result.append(lines[i]);

            // Check if we need to insert a blank line after this line
            if (i + 1 < lines.length) {
                String currentLine = lines[i].stripLeading();
                String nextLine = lines[i + 1].stripLeading();

                // If current line is a blockquote and next line is not empty and not a blockquote,
                // insert a blank line to properly terminate the blockquote
                if (currentLine.startsWith(">") && !nextLine.isEmpty() && !nextLine.startsWith(">")) {
                    result.append("\n");  // Add extra blank line
                }
            }

            if (i < lines.length - 1) {
                result.append("\n");
            }
        }

        return result.toString();
    }

    /**
     * Join lines that were artificially wrapped by pipermail's line length limit.
     * <p>
     * Pipermail wraps long lines at ~72-76 characters. When a word would exceed the limit,
     * it gets pushed to the next line, creating short "orphan" fragments. For example:
     * <pre>
     * One might think that we would need some marking on the `x` and `y`
     * components of
     * `Point3d` to indicate that they map to the corresponding components of
     * `Point`,
     * </pre>
     * becomes:
     * <pre>
     * One might think that we would need some marking on the `x` and `y` components of
     * `Point3d` to indicate that they map to the corresponding components of `Point`,
     * </pre>
     * <p>
     * The heuristic:
     * <ul>
     *   <li>Previous line is LONG (likely hit the wrap limit, > 65 chars)</li>
     *   <li>Current line starts at column 0 (no indentation)</li>
     *   <li>Current line is SHORT (orphan fragment, ≤ 15 chars)</li>
     *   <li>Not a signature pattern (e.g., "regards," followed by name)</li>
     * </ul>
     */
    private String joinPipermailWrappedLines(String content) {
        String[] lines = content.split("\n", -1);
        StringBuilder result = new StringBuilder();

        int i = 0;
        while (i < lines.length) {
            String line = lines[i];
            result.append(line);

            // Look ahead for orphan lines to join
            while (i + 1 < lines.length) {
                String currentLine = lines[i];
                String nextLine = lines[i + 1];

                if (isOrphanWrappedLine(currentLine, nextLine)) {
                    // Remove trailing whitespace and join with single space
                    while (result.length() > 0 && Character.isWhitespace(result.charAt(result.length() - 1))) {
                        result.setLength(result.length() - 1);
                    }
                    result.append(" ").append(nextLine.trim());
                    // Update lines[i] for next iteration check (in case of multiple orphans)
                    lines[i] = lines[i].stripTrailing() + " " + nextLine.trim();
                    i++;
                } else {
                    break;
                }
            }

            if (i < lines.length - 1) {
                result.append("\n");
            }
            i++;
        }

        return result.toString();
    }

    /**
     * Check if a line is an orphan fragment from pipermail line wrapping.
     */
    private boolean isOrphanWrappedLine(String prevLine, String line) {
        // Not an orphan if empty
        if (line.isEmpty()) {
            return false;
        }

        // Not an orphan if it starts with whitespace (indented content)
        if (Character.isWhitespace(line.charAt(0))) {
            return false;
        }

        // Not an orphan if it starts with > (blockquote)
        if (line.startsWith(">")) {
            return false;
        }

        // Not an orphan if previous line was a blockquote but current line isn't
        // This is a transition out of quoted text, not a wrapped fragment
        if (prevLine.stripLeading().startsWith(">")) {
            return false;
        }

        // Not an orphan if it's a fenced code block marker
        if (line.startsWith("```")) {
            return false;
        }

        // Not an orphan if it's too long (it's a full line, not a fragment)
        if (line.length() > MAX_ORPHAN_LENGTH) {
            return false;
        }

        // Not an orphan if previous line is short (intentional line break)
        String prevTrimmed = prevLine.stripTrailing();
        if (prevTrimmed.length() < MIN_LONG_LINE_LENGTH) {
            return false;
        }

        // Not an orphan if previous line looks like a signature/greeting
        if (SIGNATURE_PATTERN.matcher(prevTrimmed).matches()) {
            return false;
        }

        return true;
    }

    // Pattern to extract leading indentation (spaces or blockquote prefix "> " followed by spaces)
    private static final Pattern INDENT_PATTERN = Pattern.compile("^((?:> ?)?[ ]+)");

    // Pattern to detect list items (optional indent + - or * or number. + space)
    private static final Pattern LIST_ITEM_PATTERN = Pattern.compile("^( {0,3})([-*]|\\d+\\.)\\s");

    /**
     * Fix orphan continuation lines caused by email line wrapping.
     * <p>
     * When email clients wrap long lines in code blocks or list items, the continuation
     * often starts at column 0, which breaks the formatting. For example:
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
     * Handles: code blocks (4+ space indent), list items, and blockquotes.
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
            // 2. Previous line was indented (code block, list item, or blockquote)
            // 3. Current line is not blank
            // 4. Next line either resumes indentation or is blank (single orphan)
            boolean isOrphanLine = false;

            if (!line.isEmpty() && !Character.isWhitespace(line.charAt(0)) && !line.startsWith(">")) {
                // Check for code block continuation (4+ spaces)
                Matcher prevIndentMatcher = INDENT_PATTERN.matcher(prevLine);
                if (prevIndentMatcher.find()) {
                    String prevIndent = prevIndentMatcher.group(1);
                    int spaceCount = prevIndent.length() - prevIndent.replace(" ", "").length();

                    if (spaceCount >= 4) {
                        // Don't treat closing braces/brackets as orphans in code blocks
                        // They're intentionally at column 0 to close the block
                        if (line.trim().matches("^[}\\]);]+$")) {
                            result.append(line);
                            if (i < lines.length - 1) {
                                result.append("\n");
                            }
                            continue;
                        }

                        boolean nextLineResumes = nextLine.isBlank() ||
                                INDENT_PATTERN.matcher(nextLine).find() ||
                                nextLine.startsWith("```");

                        if (nextLineResumes) {
                            isOrphanLine = true;
                        }
                    }
                }

                // Check for list item continuation
                // But don't treat the line as orphan if it's itself a list item
                if (!isOrphanLine && LIST_ITEM_PATTERN.matcher(prevLine).find() &&
                        !LIST_ITEM_PATTERN.matcher(line).find()) {
                    // Next line should resume with indentation or be blank
                    boolean nextLineResumes = nextLine.isBlank() ||
                            nextLine.startsWith(" ") ||
                            LIST_ITEM_PATTERN.matcher(nextLine).find();

                    if (nextLineResumes) {
                        isOrphanLine = true;
                    }
                }
            }

            if (isOrphanLine) {
                // Join with previous line: remove trailing newline/whitespace and add single space
                if (result.length() > 0 && result.charAt(result.length() - 1) == '\n') {
                    result.setLength(result.length() - 1);
                }
                // Also remove any trailing whitespace from previous line
                while (result.length() > 0 && Character.isWhitespace(result.charAt(result.length() - 1))) {
                    result.setLength(result.length() - 1);
                }
                result.append(" ");
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

    /**
     * Convert markdown to simple HTML for the HTML view.
     * This is a basic conversion for display purposes.
     */
    private String markdownToSimpleHtml(String markdown) {
        // Use the markdown converter to convert markdown to HTML
        return markdownConverter.toHtml(markdown);
    }
}
