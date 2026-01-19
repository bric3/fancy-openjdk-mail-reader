/*
 * fancy-mail-openjdk-reader
 *
 * Copyright (c) 2026 - Brice Dutheil
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package dev.brice.fancymail.markdown;

import com.vladsch.flexmark.ast.LinkRef;
import com.vladsch.flexmark.ast.Paragraph;
import com.vladsch.flexmark.parser.PostProcessor;
import com.vladsch.flexmark.parser.PostProcessorFactory;
import com.vladsch.flexmark.util.ast.Document;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.ast.NodeTracker;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Post-processor that finds mailing list style link references and transforms them.
 * <p>
 * This processor:
 * <ol>
 *   <li>Finds paragraphs at the end containing {@code [n] URL} definitions</li>
 *   <li>Extracts references into a map</li>
 *   <li>Replaces inline {@code [n]} with {@link InlineLinkRef} nodes</li>
 *   <li>Adds a {@link LinkReferencesBlock} at the end</li>
 * </ol>
 */
public class MailingListLinkRefPostProcessor implements PostProcessor {

    // Pattern to match reference definitions: [n] URL
    private static final Pattern REFERENCE_DEFINITION_PATTERN = Pattern.compile(
            "^\\[(\\d+)]\\s+(https?://\\S+)\\s*$", Pattern.MULTILINE
    );

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(MailingListLinkRefPostProcessor.class);

    public MailingListLinkRefPostProcessor() {
    }

    @Override
    public void process(@NotNull NodeTracker state, @NotNull Node node) {
        // Not used - document-level processing happens in processDocument()
    }

    @Override
    public @NotNull Document processDocument(@NotNull Document document) {
        LOG.debug("processDocument() called with {} children", countChildren(document));

        // Collect reference definitions from the end of the document
        Map<String, String> references = new LinkedHashMap<>();
        List<Node> nodesToRemove = new ArrayList<>();

        collectReferencesFromEnd(document, references, nodesToRemove);
        LOG.debug("Found {} references: {}", references.size(), references);

        if (references.isEmpty()) {
            return document;
        }

        // Remove the definition paragraphs
        for (Node toRemove : nodesToRemove) {
            toRemove.unlink();
        }

        // Process inline references in all text nodes
        processInlineReferences(document, references);

        // Add the references block at the end
        LinkReferencesBlock referencesBlock = new LinkReferencesBlock(references);
        document.appendChild(referencesBlock);
        LOG.debug("Added LinkReferencesBlock to document");

        return document;
    }

    private void collectReferencesFromEnd(Document document, Map<String, String> references, List<Node> nodesToRemove) {
        // Walk backwards from the last child
        Node current = document.getLastChild();
        LOG.debug("Starting to collect references, last child: {}", current != null ? current.getClass().getSimpleName() : "null");

        while (current != null) {
            LOG.debug("Checking node: {} with chars: '{}'", current.getClass().getSimpleName(),
                    current.getChars().toString().replace("\n", "\\n"));
            if (current instanceof Paragraph paragraph) {
                String text = paragraph.getChars().toString().trim();
                LOG.debug("Paragraph text: '{}'", text);

                // Check if the entire paragraph consists only of reference definitions
                // A paragraph can have multiple definitions on separate lines
                if (isReferenceDefinitionParagraph(text, references)) {
                    LOG.debug("Paragraph is all reference definitions, marking for removal");
                    nodesToRemove.add(paragraph);
                    current = current.getPrevious();
                    continue;
                }
            }

            // Stop at first non-reference content
            // But allow blank paragraphs (though flexmark usually doesn't create them)
            if (current instanceof Paragraph paragraph) {
                String text = paragraph.getChars().toString().trim();
                if (text.isEmpty()) {
                    current = current.getPrevious();
                    continue;
                }
            }

            // Non-reference content found, stop
            break;
        }
    }

    /**
     * Check if a paragraph text consists entirely of reference definitions.
     * Extracts any found references into the provided map.
     *
     * @param text       the paragraph text (trimmed)
     * @param references map to collect found references into
     * @return true if ALL lines in the text are reference definitions
     */
    private boolean isReferenceDefinitionParagraph(String text, Map<String, String> references) {
        // Split by lines and check each line
        String[] lines = text.split("\n");
        Map<String, String> foundRefs = new LinkedHashMap<>();

        for (String line : lines) {
            String trimmedLine = line.trim();
            if (trimmedLine.isEmpty()) {
                continue; // Allow blank lines within the paragraph
            }

            Matcher matcher = REFERENCE_DEFINITION_PATTERN.matcher(trimmedLine);
            if (matcher.matches()) {
                String refNum = matcher.group(1);
                String url = matcher.group(2);
                LOG.debug("Line matched reference: {} -> {}", refNum, url);
                foundRefs.put(refNum, url);
            } else {
                // Found a non-reference line, this paragraph is not all references
                LOG.debug("Line '{}' does not match reference pattern", trimmedLine);
                return false;
            }
        }

        // All lines matched, add found references to the main map
        // Keep first encountered definition for each number (reversed order due to walking backwards)
        for (var entry : foundRefs.entrySet()) {
            if (!references.containsKey(entry.getKey())) {
                references.put(entry.getKey(), entry.getValue());
            }
        }

        return !foundRefs.isEmpty();
    }

    private void processInlineReferences(Document document, Map<String, String> references) {
        // Find all LinkRef nodes (Flexmark parses [n] as LinkRef nodes)
        List<LinkRef> linkRefNodes = new ArrayList<>();
        collectLinkRefNodes(document, linkRefNodes);
        LOG.debug("Found {} LinkRef nodes to process", linkRefNodes.size());

        for (LinkRef linkRef : linkRefNodes) {
            processLinkRefNode(linkRef, references);
        }
    }

    private void collectLinkRefNodes(Node node, List<LinkRef> linkRefNodes) {
        if (node instanceof LinkRef linkRef) {
            linkRefNodes.add(linkRef);
        }
        for (Node child = node.getFirstChild(); child != null; child = child.getNext()) {
            collectLinkRefNodes(child, linkRefNodes);
        }
    }

    private void processLinkRefNode(LinkRef linkRef, Map<String, String> references) {
        // Get the reference text (e.g., "1" from "[1]")
        String refText = linkRef.getReference().toString();
        LOG.debug("Processing LinkRef with reference: '{}'", refText);

        // Check if it's a numeric reference that matches our pattern
        if (refText.matches("\\d+") && references.containsKey(refText)) {
            String url = references.get(refText);
            LOG.debug("Replacing LinkRef [{}] with InlineLinkRef pointing to: {}", refText, url);

            // Create our custom InlineLinkRef node
            InlineLinkRef inlineLinkRef = new InlineLinkRef(linkRef.getChars(), refText);
            inlineLinkRef.setResolvedUrl(url);

            // Replace the LinkRef with our InlineLinkRef
            linkRef.insertBefore(inlineLinkRef);
            linkRef.unlink();
        } else {
            LOG.debug("LinkRef [{}] does not match any reference definition", refText);
        }
    }

    private int countChildren(Node node) {
        int count = 0;
        for (Node child = node.getFirstChild(); child != null; child = child.getNext()) {
            count++;
        }
        return count;
    }

    /**
     * Factory that creates the post processor.
     * <p>
     * This implements PostProcessorFactory directly to support document-level processing.
     * The affectsGlobalScope() returns true to indicate this processes the entire document.
     */
    public static class Factory implements PostProcessorFactory {
        private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(Factory.class);

        public Factory() {
            LOG.debug("MailingListLinkRefPostProcessor.Factory created");
        }

        @Override
        public @NotNull Map<Class<?>, Set<Class<?>>> getNodeTypes() {
            // Return empty map - we do document-level processing, not node-level
            return Map.of();
        }

        @Override
        public @NotNull Set<Class<?>> getAfterDependents() {
            return Set.of();
        }

        @Override
        public @NotNull Set<Class<?>> getBeforeDependents() {
            return Set.of();
        }

        @Override
        public boolean affectsGlobalScope() {
            // Return true to indicate this is a document-level processor
            return true;
        }

        @Override
        public @Nullable PostProcessor apply(@NotNull Document document) {
            LOG.debug("Factory.apply() called with document");
            // Return a new processor instance
            return new MailingListLinkRefPostProcessor();
        }
    }
}
