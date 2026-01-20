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

import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Filter for identifying Call for Vote (CFV) messages in OpenJDK mailing lists.
 * <p>
 * CFV messages are typically used for voting on new committers or members:
 * <ul>
 *   <li>CFV: New XXX Committer: Name</li>
 *   <li>CFV: New XXX Member: Name</li>
 *   <li>Result: New XXX Committer: Name</li>
 *   <li>Result: New XXX Member: Name</li>
 * </ul>
 * <p>
 * These subjects may be prefixed with "Re: " for replies in the thread.
 */
public final class CfvMessageFilter implements Predicate<String> {

    private static final CfvMessageFilter INSTANCE = new CfvMessageFilter();

    // Pattern matches CFV or Result followed by committer/member keywords
    // Case insensitive, handles optional "Re: " prefix (possibly multiple)
    private static final Pattern CFV_PATTERN = Pattern.compile(
            "^(Re:\\s*)*(CFV|Result):\\s*.*(Committer|Member)",
            Pattern.CASE_INSENSITIVE
    );

    private CfvMessageFilter() {
    }

    /**
     * Returns the singleton instance of the CFV message filter.
     */
    public static CfvMessageFilter instance() {
        return INSTANCE;
    }

    /**
     * Tests whether the given subject indicates a CFV-related message.
     *
     * @param subject the email subject to test
     * @return {@code true} if the subject matches a CFV message pattern
     */
    @Override
    public boolean test(String subject) {
        if (subject == null || subject.isEmpty()) {
            return false;
        }
        return CFV_PATTERN.matcher(subject).find();
    }
}
