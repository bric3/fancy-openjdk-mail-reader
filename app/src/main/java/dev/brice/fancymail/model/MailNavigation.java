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

/**
 * Navigation links for an email in the mailing list archive.
 *
 * @param prevMessage link to previous message (by thread), null if none
 * @param nextMessage link to next message (by thread), null if none
 * @param dateIndexUrl URL to date-sorted index on pipermail
 * @param threadIndexUrl URL to thread-sorted index on pipermail
 * @param subjectIndexUrl URL to subject-sorted index on pipermail
 * @param authorIndexUrl URL to author-sorted index on pipermail
 */
public record MailNavigation(
        NavLink prevMessage,
        NavLink nextMessage,
        String dateIndexUrl,
        String threadIndexUrl,
        String subjectIndexUrl,
        String authorIndexUrl
) {
    /**
     * A navigation link with URL and title.
     */
    public record NavLink(String url, String title) {}
}
