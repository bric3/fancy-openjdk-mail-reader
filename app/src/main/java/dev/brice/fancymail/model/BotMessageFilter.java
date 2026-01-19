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

/**
 * Filter for identifying bot-generated messages in OpenJDK mailing lists.
 * <p>
 * Bot messages typically include automated notifications from the JDK Git workflow:
 * <ul>
 *   <li>RFR (Request for Review) notifications</li>
 *   <li>Integrated notifications when a PR is merged</li>
 *   <li>Withdrawn notifications when a PR is withdrawn</li>
 *   <li>Git commit notifications</li>
 * </ul>
 * <p>
 * These keywords may appear after tag prefixes like {@code [branch-name]} or {@code [project]},
 * so the filter uses {@code contains()} rather than {@code startsWith()} for most patterns.
 */
public final class BotMessageFilter implements Predicate<String> {

    private static final BotMessageFilter INSTANCE = new BotMessageFilter();

    private BotMessageFilter() {
    }

    /**
     * Returns the singleton instance of the bot message filter.
     */
    public static BotMessageFilter instance() {
        return INSTANCE;
    }

    /**
     * Tests whether the given subject indicates a bot-generated message.
     *
     * @param subject the email subject to test
     * @return {@code true} if the subject matches a bot message pattern
     */
    @Override
    public boolean test(String subject) {
        if (subject == null) {
            return false;
        }
        // Use contains() to handle subjects with tag prefixes like [branch-name]
        return subject.contains("RFR:") || subject.contains("Re: RFR:")
                || subject.contains("Integrated:") || subject.contains("Re: Integrated:")
                || subject.contains("Withdrawn:") || subject.contains("Re: Withdrawn:")
                || subject.startsWith("git:");
    }
}
