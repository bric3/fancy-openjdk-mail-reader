/*
 * fancy-mail-openjdk-reader
 *
 * Copyright (c) 2026 - Brice Dutheil
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package dev.brice.fancymail.model;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents the path components of an OpenJDK mailing list URL.
 * Example URL: https://mail.openjdk.org/pipermail/amber-spec-experts/2026-January/004307.html
 */
public record MailPath(String list, String yearMonth, String id) {

    private static final Pattern URL_PATTERN = Pattern.compile(
            "https?://mail\\.openjdk\\.org/pipermail/([^/]+)/([^/]+)/(\\d+)\\.html"
    );

    private static final Pattern PATH_PATTERN = Pattern.compile(
            "([^/]+)/([^/]+)/(\\d+)\\.html"
    );

    /**
     * Parse a full URL into a MailPath.
     */
    public static MailPath fromUrl(String url) {
        Matcher matcher = URL_PATTERN.matcher(url);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid OpenJDK mailing list URL: " + url);
        }
        return new MailPath(matcher.group(1), matcher.group(2), matcher.group(3));
    }

    /**
     * Parse a path string (e.g., "amber-spec-experts/2026-January/004307.html") into a MailPath.
     */
    public static MailPath fromPath(String path) {
        Matcher matcher = PATH_PATTERN.matcher(path);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid path: " + path);
        }
        return new MailPath(matcher.group(1), matcher.group(2), matcher.group(3));
    }

    /**
     * Returns the original OpenJDK URL for this mail.
     */
    public String toOriginalUrl() {
        return "https://mail.openjdk.org/pipermail/" + list + "/" + yearMonth + "/" + id + ".html";
    }

    /**
     * Returns the local rendered path.
     */
    public String toRenderedPath() {
        return "/rendered/" + list + "/" + yearMonth + "/" + id + ".html";
    }

    /**
     * Returns the cache key for this mail.
     */
    public String toCacheKey() {
        return list + "/" + yearMonth + "/" + id;
    }
}
