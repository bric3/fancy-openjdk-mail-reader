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

import java.util.HexFormat;
import java.util.List;

/**
 * Represents a complete email thread tree with Merkle root hash.
 *
 * @param list          the mailing list name
 * @param yearMonth     the year-month (e.g., "2026-January")
 * @param roots         the root entries of the thread tree (multiple threads in a month)
 * @param merkleRootHash the Merkle root hash for integrity verification
 * @param totalMessages total number of messages in the tree
 */
public record ThreadTree(
        String list,
        String yearMonth,
        List<ThreadEntry> roots,
        byte[] merkleRootHash,
        int totalMessages
) {
    /**
     * Returns the Merkle root hash as a hex string.
     */
    public String merkleRootHashHex() {
        return merkleRootHash != null ? HexFormat.of().formatHex(merkleRootHash) : null;
    }

    /**
     * Find an entry by its message ID.
     *
     * @param messageId the message ID to find
     * @return the ThreadEntry if found, or null
     */
    public ThreadEntry findById(String messageId) {
        for (ThreadEntry root : roots) {
            ThreadEntry found = findByIdRecursive(root, messageId);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private ThreadEntry findByIdRecursive(ThreadEntry entry, String messageId) {
        if (entry.id().equals(messageId)) {
            return entry;
        }
        for (ThreadEntry reply : entry.replies()) {
            ThreadEntry found = findByIdRecursive(reply, messageId);
            if (found != null) {
                return found;
            }
        }
        return null;
    }
}
