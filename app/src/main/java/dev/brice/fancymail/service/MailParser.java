package dev.brice.fancymail.service;

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
                mailPath.toOriginalUrl()
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

        // Decode HTML entities
        content = Parser.unescapeEntities(content, false);

        // Replace non-breaking spaces with regular spaces
        content = content.replace('\u00A0', ' ');

        // Remove attachment notices (lines starting with "-------------- next part")
        content = content.replaceAll("(?m)^-{10,} next part.*(?:\\r?\\n.*)*$", "");

        // Trim trailing whitespace from each line but preserve line breaks
        content = content.lines()
                .map(String::stripTrailing)
                .reduce((a, b) -> a + "\n" + b)
                .orElse("");

        // Remove excessive blank lines (more than 2 consecutive)
        content = content.replaceAll("\n{3,}", "\n\n");

        return content.trim();
    }

    /**
     * Convert markdown to simple HTML for the HTML view.
     * This is a basic conversion for display purposes.
     */
    private String markdownToSimpleHtml(String markdown) {
        // Use the markdown converter's HTML output
        return markdownConverter.toMarkdown("<pre>" + markdown + "</pre>");
    }
}
