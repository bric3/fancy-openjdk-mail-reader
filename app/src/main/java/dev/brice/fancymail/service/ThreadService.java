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

import dev.brice.fancymail.cache.ThreadCache;
import dev.brice.fancymail.model.ThreadContext;
import dev.brice.fancymail.model.ThreadEntry;
import dev.brice.fancymail.model.ThreadTree;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service to orchestrate thread fetching, parsing, caching, and context building.
 */
@Singleton
public class ThreadService {

    private static final Logger LOG = LoggerFactory.getLogger(ThreadService.class);

    private final MailFetcher mailFetcher;
    private final ThreadParser threadParser;
    private final ThreadMerkleService merkleService;
    private final ThreadCache threadCache;

    public ThreadService(
            MailFetcher mailFetcher,
            ThreadParser threadParser,
            ThreadMerkleService merkleService,
            ThreadCache threadCache) {
        this.mailFetcher = mailFetcher;
        this.threadParser = threadParser;
        this.merkleService = merkleService;
        this.threadCache = threadCache;
    }

    /**
     * Get the thread tree for a mailing list month, using cache if available.
     *
     * @param list      the mailing list name
     * @param yearMonth the year-month
     * @return the thread tree with Merkle hashes
     */
    public ThreadTree getThreadTree(String list, String yearMonth) {
        // Check cache first
        Optional<ThreadTree> cached = threadCache.get(list, yearMonth);
        if (cached.isPresent()) {
            return cached.get();
        }

        // Fetch and parse
        LOG.info("Fetching thread tree for {}/{}", list, yearMonth);
        String html = mailFetcher.fetchThreadPage(list, yearMonth);
        ThreadTree tree = threadParser.parse(html, list, yearMonth);

        // Compute Merkle hashes
        tree = merkleService.computeMerkleTree(tree);

        // Cache the result
        threadCache.put(tree);

        return tree;
    }

    /**
     * Get the thread context for a specific message.
     * Returns the current entry, its ancestors, siblings, and the thread root.
     *
     * @param list      the mailing list name
     * @param yearMonth the year-month
     * @param messageId the message ID
     * @return the thread context, or null if message not found in thread
     */
    public ThreadContext getThreadContext(String list, String yearMonth, String messageId) {
        ThreadTree tree = getThreadTree(list, yearMonth);

        // Find the entry for this message
        ThreadEntry current = tree.findById(messageId);
        if (current == null) {
            LOG.warn("Message {} not found in thread tree for {}/{}", messageId, list, yearMonth);
            return null;
        }

        // Find ancestors and siblings
        List<ThreadEntry> ancestors = new ArrayList<>();
        List<ThreadEntry> siblings = new ArrayList<>();
        ThreadEntry threadRoot = findThreadRoot(tree, messageId, ancestors, siblings);

        return new ThreadContext(
                current,
                ancestors,
                siblings,
                threadRoot,
                list,
                yearMonth,
                tree.merkleRootHashHex()
        );
    }

    /**
     * Find the thread root containing the given message, collecting ancestors and siblings.
     */
    private ThreadEntry findThreadRoot(
            ThreadTree tree,
            String messageId,
            List<ThreadEntry> ancestors,
            List<ThreadEntry> siblings) {

        for (ThreadEntry root : tree.roots()) {
            if (findInSubtree(root, messageId, ancestors, siblings)) {
                return root;
            }
        }
        return null;
    }

    /**
     * Search for message in subtree, building ancestors and siblings lists.
     * Returns true if message is found in this subtree.
     */
    private boolean findInSubtree(
            ThreadEntry entry,
            String messageId,
            List<ThreadEntry> ancestors,
            List<ThreadEntry> siblings) {

        if (entry.id().equals(messageId)) {
            // Found it - no ancestors or siblings to add at this level
            return true;
        }

        // Check if message is in any of the replies
        for (int i = 0; i < entry.replies().size(); i++) {
            ThreadEntry reply = entry.replies().get(i);

            if (reply.id().equals(messageId)) {
                // Found it as a direct child - add current as ancestor
                ancestors.add(entry);
                // Add siblings (other replies at this level)
                for (int j = 0; j < entry.replies().size(); j++) {
                    if (i != j) {
                        siblings.add(entry.replies().get(j));
                    }
                }
                return true;
            }

            // Search deeper
            if (findInSubtree(reply, messageId, ancestors, siblings)) {
                // Found in subtree - add current as ancestor (prepend since we're going up)
                ancestors.add(0, entry);
                return true;
            }
        }

        return false;
    }
}
