/*
 * fancy-mail-openjdk-reader
 *
 * Copyright (c) 2026 - Brice Dutheil
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package dev.brice.fancymail.config;

import io.micronaut.context.annotation.ConfigurationProperties;

/**
 * Configuration for URL path prefixes.
 * Allows customizing the paths used for rendered and markdown endpoints.
 */
@ConfigurationProperties("fancymail.paths")
public class PathsConfig {

    private String rendered = "rendered";
    private String markdown = "markdown";
    private String threads = "threads";

    public String getRendered() {
        return rendered;
    }

    public void setRendered(String rendered) {
        this.rendered = rendered;
    }

    public String getMarkdown() {
        return markdown;
    }

    public void setMarkdown(String markdown) {
        this.markdown = markdown;
    }

    public String getThreads() {
        return threads;
    }

    public void setThreads(String threads) {
        this.threads = threads;
    }

    // Template-friendly accessors
    public String rendered() {
        return rendered;
    }

    public String markdown() {
        return markdown;
    }

    public String threads() {
        return threads;
    }

    /**
     * Build a rendered path for the given mail components.
     */
    public String toRenderedPath(String list, String yearMonth, String id) {
        return "/" + rendered + "/" + list + "/" + yearMonth + "/" + id + ".html";
    }

    /**
     * Build a markdown path for the given mail components.
     */
    public String toMarkdownPath(String list, String yearMonth, String id) {
        return "/" + markdown + "/" + list + "/" + yearMonth + "/" + id + ".md";
    }

    /**
     * Build a threads path for the given list and month.
     */
    public String toThreadsPath(String list, String yearMonth) {
        return "/" + threads + "/" + list + "/" + yearMonth;
    }
}
