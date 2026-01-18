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

import dev.brice.fancymail.model.ThreadEntry;
import dev.brice.fancymail.model.ThreadTree;
import io.crums.util.mrkl.Builder;
import io.crums.util.mrkl.Tree;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

/**
 * Service to build Merkle trees from thread entries and compute content hashes.
 * <p>
 * The Merkle tree is constructed by:
 * 1. Computing content hash for each entry: SHA-256(id || subject || author)
 * 2. Flattening the thread tree in depth-first order to get the leaf nodes
 * 3. Building the Merkle tree from these leaves
 * <p>
 * This provides integrity verification - if any message metadata changes,
 * the Merkle root hash will change.
 */
@Singleton
public class ThreadMerkleService {

    private static final Logger LOG = LoggerFactory.getLogger(ThreadMerkleService.class);
    private static final String HASH_ALGORITHM = "SHA-256";

    /**
     * Compute content hashes for all entries and build the Merkle tree.
     *
     * @param tree the ThreadTree with entries (without hashes)
     * @return a new ThreadTree with content hashes and Merkle root hash
     */
    public ThreadTree computeMerkleTree(ThreadTree tree) {
        if (tree.roots().isEmpty()) {
            LOG.debug("Empty thread tree, returning with null Merkle root");
            return tree;
        }

        // First, compute content hashes for all entries
        List<ThreadEntry> hashedRoots = new ArrayList<>();
        List<byte[]> leafHashes = new ArrayList<>();

        for (ThreadEntry root : tree.roots()) {
            ThreadEntry hashedEntry = computeEntryHashes(root, leafHashes);
            hashedRoots.add(hashedEntry);
        }

        // Build Merkle tree from leaf hashes
        byte[] merkleRoot = buildMerkleRoot(leafHashes);

        LOG.debug("Computed Merkle tree with {} leaves, root hash: {}",
                leafHashes.size(), bytesToHex(merkleRoot));

        return new ThreadTree(
                tree.list(),
                tree.yearMonth(),
                hashedRoots,
                merkleRoot,
                tree.totalMessages()
        );
    }

    /**
     * Recursively compute content hashes for an entry and its replies.
     * Collects leaf hashes in depth-first order.
     */
    private ThreadEntry computeEntryHashes(ThreadEntry entry, List<byte[]> leafHashes) {
        // Compute content hash: SHA-256(id || subject || author)
        byte[] contentHash = computeContentHash(entry.id(), entry.subject(), entry.author());
        leafHashes.add(contentHash);

        // Process replies recursively
        List<ThreadEntry> hashedReplies = new ArrayList<>();
        for (ThreadEntry reply : entry.replies()) {
            hashedReplies.add(computeEntryHashes(reply, leafHashes));
        }

        // Create new entry with hash and updated replies
        return new ThreadEntry(
                entry.id(),
                entry.subject(),
                entry.author(),
                entry.depth(),
                hashedReplies,
                contentHash
        );
    }

    /**
     * Compute SHA-256 hash of id || subject || author.
     */
    private byte[] computeContentHash(String id, String subject, String author) {
        try {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            digest.update(id.getBytes(StandardCharsets.UTF_8));
            digest.update(subject.getBytes(StandardCharsets.UTF_8));
            digest.update(author.getBytes(StandardCharsets.UTF_8));
            return digest.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Build Merkle tree and return the root hash.
     */
    private byte[] buildMerkleRoot(List<byte[]> leafHashes) {
        if (leafHashes.isEmpty()) {
            return new byte[32]; // Empty tree - return zero hash
        }

        if (leafHashes.size() == 1) {
            // Single leaf - the content hash is also the root
            return leafHashes.get(0);
        }

        // Use the crums Merkle tree builder
        Builder builder = new Builder(HASH_ALGORITHM);

        for (byte[] hash : leafHashes) {
            builder.add(hash);
        }

        Tree tree = builder.build();

        // The root is at level (tree.height() - 1), index 0
        // For the crums library, root() method returns the root node
        return tree.root().data();
    }

    /**
     * Convert bytes to hex string for logging.
     */
    private String bytesToHex(byte[] bytes) {
        if (bytes == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
