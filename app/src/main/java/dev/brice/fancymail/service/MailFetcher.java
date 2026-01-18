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

import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service to fetch raw HTML content from OpenJDK mailing list.
 */
@Singleton
public class MailFetcher {

    private static final Logger LOG = LoggerFactory.getLogger(MailFetcher.class);

    private final HttpClient httpClient;

    public MailFetcher(@Client("https://mail.openjdk.org") HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    /**
     * Fetches the raw HTML content from a mailing list URL.
     *
     * @param url the full URL to fetch
     * @return the HTML content as a string
     */
    public String fetch(String url) {
        LOG.debug("Fetching URL: {}", url);
        // Extract the path from the full URL
        String path = url.replace("https://mail.openjdk.org", "")
                        .replace("http://mail.openjdk.org", "");
        return httpClient.toBlocking().retrieve(path);
    }

    /**
     * Fetches HTML content using path components.
     *
     * @param list      the mailing list name (e.g., "amber-spec-experts")
     * @param yearMonth the year-month (e.g., "2026-January")
     * @param id        the message ID (e.g., "004307")
     * @return the HTML content as a string
     */
    public String fetch(String list, String yearMonth, String id) {
        String path = "/pipermail/" + list + "/" + yearMonth + "/" + id + ".html";
        LOG.debug("Fetching path: {}", path);
        return httpClient.toBlocking().retrieve(path);
    }

    /**
     * Fetches the thread.html page for a mailing list month.
     *
     * @param list      the mailing list name (e.g., "amber-spec-experts")
     * @param yearMonth the year-month (e.g., "2026-January")
     * @return the HTML content of thread.html
     */
    public String fetchThreadPage(String list, String yearMonth) {
        String path = "/pipermail/" + list + "/" + yearMonth + "/thread.html";
        LOG.debug("Fetching thread page: {}", path);
        return httpClient.toBlocking().retrieve(path);
    }
}
