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

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a single entry in an email thread tree.
 *
 * @param id          the message ID (e.g., "004307")
 * @param subject     the email subject
 * @param author      the author name
 * @param depth       the nesting depth in the thread (0 = root)
 * @param replies     child entries (replies to this message)
 * @param contentHash SHA-256 hash of id || subject || author for Merkle tree
 */
public record ThreadEntry(
        String id,
        String subject,
        String author,
        int depth,
        List<ThreadEntry> replies,
        byte[] contentHash
) {
    /**
     * Creates a ThreadEntry without a content hash (to be computed later).
     */
    public ThreadEntry(String id, String subject, String author, int depth) {
        this(id, subject, author, depth, new ArrayList<>(), null);
    }

    /**
     * Creates a copy of this entry with the given content hash.
     */
    public ThreadEntry withContentHash(byte[] hash) {
        return new ThreadEntry(id, subject, author, depth, replies, hash);
    }

    /**
     * Returns the local rendered URL for this entry.
     */
    public String toRenderedPath(String list, String yearMonth) {
        return "/rendered/" + list + "/" + yearMonth + "/" + id + ".html";
    }

    /**
     * Returns the total count of messages in this subtree (including this entry).
     */
    public int totalCount() {
        int count = 1;
        for (ThreadEntry reply : replies) {
            count += reply.totalCount();
        }
        return count;
    }
}
