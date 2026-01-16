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
 * Represents a parsed email from the OpenJDK mailing list.
 */
public record ParsedMail(
        String subject,
        String from,
        String email,
        String date,
        String list,
        String bodyMarkdown,
        String bodyHtml,
        String originalUrl,
        MailNavigation navigation
) {
}
