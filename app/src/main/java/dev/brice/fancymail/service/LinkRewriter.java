package dev.brice.fancymail.service;

import jakarta.inject.Singleton;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service to rewrite links in parsed email content to point to the local proxy server.
 */
@Singleton
public class LinkRewriter {

    private static final Logger LOG = LoggerFactory.getLogger(LinkRewriter.class);

    // Pattern to match OpenJDK mailing list URLs
    private static final Pattern OPENJDK_MAIL_PATTERN = Pattern.compile(
            "https?://mail\\.openjdk\\.org/pipermail/([^/]+)/([^/]+)/(\\d+)\\.html"
    );

    // Pattern to match relative links (e.g., "004306.html")
    private static final Pattern RELATIVE_MAIL_PATTERN = Pattern.compile(
            "^(\\d+)\\.html$"
    );

    /**
     * Rewrite all links in the given element to point to the local proxy.
     *
     * @param element the element containing links to rewrite
     */
    public void rewriteLinks(Element element) {
        Elements links = element.select("a[href]");
        for (Element link : links) {
            String href = link.attr("href");
            String rewritten = rewriteLink(href);
            if (!rewritten.equals(href)) {
                LOG.debug("Rewriting link: {} -> {}", href, rewritten);
                link.attr("href", rewritten);
            }
        }
    }

    /**
     * Rewrite a single link URL.
     *
     * @param href the original href
     * @return the rewritten href, or the original if no rewrite is needed
     */
    public String rewriteLink(String href) {
        if (href == null || href.isBlank()) {
            return href;
        }

        // Check for full OpenJDK mail URLs
        Matcher fullMatcher = OPENJDK_MAIL_PATTERN.matcher(href);
        if (fullMatcher.matches()) {
            String list = fullMatcher.group(1);
            String yearMonth = fullMatcher.group(2);
            String id = fullMatcher.group(3);
            return "/rendered/" + list + "/" + yearMonth + "/" + id + ".html";
        }

        // Skip navigation links and external links
        if (href.startsWith("date.html") || href.startsWith("thread.html") ||
            href.startsWith("subject.html") || href.startsWith("author.html") ||
            href.startsWith("http://") || href.startsWith("https://") ||
            href.startsWith("mailto:") || href.startsWith("#")) {
            return href;
        }

        // Note: Relative links (e.g., "004306.html") are kept as-is
        // They will be resolved relative to the current rendered URL
        // which maintains the correct path structure

        return href;
    }

    /**
     * Rewrite markdown content links.
     *
     * @param markdown the markdown content
     * @param currentList the current mailing list (for relative links)
     * @param currentYearMonth the current year-month (for relative links)
     * @return the markdown with rewritten links
     */
    public String rewriteMarkdownLinks(String markdown, String currentList, String currentYearMonth) {
        if (markdown == null) {
            return null;
        }

        // Rewrite full OpenJDK mail URLs in markdown
        Matcher fullMatcher = OPENJDK_MAIL_PATTERN.matcher(markdown);
        StringBuffer sb = new StringBuffer();
        while (fullMatcher.find()) {
            String list = fullMatcher.group(1);
            String yearMonth = fullMatcher.group(2);
            String id = fullMatcher.group(3);
            fullMatcher.appendReplacement(sb, "/rendered/" + list + "/" + yearMonth + "/" + id + ".html");
        }
        fullMatcher.appendTail(sb);

        return sb.toString();
    }
}
