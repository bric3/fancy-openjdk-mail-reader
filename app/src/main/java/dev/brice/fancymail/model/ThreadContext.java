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

import java.util.List;

/**
 * Represents the thread context for viewing a specific message.
 * Contains the current message, its ancestors, siblings, and the full thread.
 *
 * @param currentEntry   the currently viewed message entry
 * @param ancestors      list of ancestor entries from root to parent (empty if current is root)
 * @param siblings       sibling entries at the same level (excluding current)
 * @param threadRoot     the root entry of the thread containing this message
 * @param list           the mailing list name
 * @param yearMonth      the year-month
 * @param merkleRootHash the Merkle root hash (hex string) for the full thread tree
 */
public record ThreadContext(
        ThreadEntry currentEntry,
        List<ThreadEntry> ancestors,
        List<ThreadEntry> siblings,
        ThreadEntry threadRoot,
        String list,
        String yearMonth,
        String merkleRootHash
) {
    /**
     * Returns true if the current message is the thread root.
     */
    public boolean isRoot() {
        return ancestors.isEmpty();
    }

    /**
     * Returns the total message count in this thread.
     */
    public int threadMessageCount() {
        return threadRoot != null ? threadRoot.totalCount() : 1;
    }

    /**
     * Returns the depth of the current message in the thread.
     */
    public int currentDepth() {
        return currentEntry != null ? currentEntry.depth() : 0;
    }
}
