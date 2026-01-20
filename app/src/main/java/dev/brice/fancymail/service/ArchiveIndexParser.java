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

import dev.brice.fancymail.model.ArchiveIndex;
import jakarta.inject.Singleton;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service to parse the archive index page from OpenJDK mailing lists.
 * <p>
 * The archive index page contains links to monthly archives in the format:
 * <pre>
 * &lt;a href="2026-January/thread.html"&gt;Thread&lt;/a&gt;
 * </pre>
 */
@Singleton
public class ArchiveIndexParser {

    private static final Logger LOG = LoggerFactory.getLogger(ArchiveIndexParser.class);

    // Pattern to extract year-month from hrefs like "2026-January/thread.html"
    private static final Pattern YEAR_MONTH_PATTERN = Pattern.compile("(\\d{4}-(January|February|March|April|May|June|July|August|September|October|November|December))/");

    /**
     * Parse the archive index HTML to extract available months.
     *
     * @param html the HTML content of the archive index page
     * @param list the mailing list name
     * @return an ArchiveIndex containing all available months
     */
    public ArchiveIndex parse(String html, String list) {
        Document doc = Jsoup.parse(html);
        Set<String> availableMonths = new HashSet<>();

        // Find all links that match the year-month pattern
        Elements links = doc.select("a[href]");
        for (Element link : links) {
            String href = link.attr("href");
            Matcher matcher = YEAR_MONTH_PATTERN.matcher(href);
            if (matcher.find()) {
                String yearMonth = matcher.group(1);
                availableMonths.add(yearMonth);
            }
        }

        LOG.info("Parsed archive index for {}: found {} available months", list, availableMonths.size());
        LOG.debug("Available months for {}: {}", list, availableMonths);

        return ArchiveIndex.of(list, availableMonths);
    }
}
