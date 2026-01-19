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

import com.vladsch.flexmark.html.HtmlWriter;
import com.vladsch.flexmark.html.renderer.NodeRenderer;
import com.vladsch.flexmark.html.renderer.NodeRendererContext;
import com.vladsch.flexmark.html.renderer.NodeRendererFactory;
import com.vladsch.flexmark.html.renderer.NodeRenderingHandler;
import com.vladsch.flexmark.util.data.DataHolder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Renderer for mailing list link reference nodes.
 * <p>
 * Renders:
 * <ul>
 *   <li>{@link InlineLinkRef} as {@code <sup><a href="url">[n]</a></sup>}</li>
 *   <li>{@link LinkReferencesBlock} as a styled references section</li>
 * </ul>
 */
public class MailingListLinkRefRenderer implements NodeRenderer {

    public MailingListLinkRefRenderer(DataHolder options) {
    }

    @Override
    public @Nullable Set<NodeRenderingHandler<?>> getNodeRenderingHandlers() {
        Set<NodeRenderingHandler<?>> handlers = new HashSet<>();

        handlers.add(new NodeRenderingHandler<>(
                InlineLinkRef.class,
                this::renderInlineLinkRef
        ));

        handlers.add(new NodeRenderingHandler<>(
                LinkReferencesBlock.class,
                this::renderLinkReferencesBlock
        ));

        return handlers;
    }

    private void renderInlineLinkRef(InlineLinkRef node, NodeRendererContext context, HtmlWriter html) {
        if (node.isResolved()) {
            html.raw("<sup><a href=\"")
                .raw(escapeHtml(node.getResolvedUrl()))
                .raw("\">[")
                .raw(node.getRefNumber())
                .raw("]</a></sup>");
        } else {
            // Unresolved reference, render as plain text
            html.raw("[")
                .raw(node.getRefNumber())
                .raw("]");
        }
    }

    private void renderLinkReferencesBlock(LinkReferencesBlock node, NodeRendererContext context, HtmlWriter html) {
        Map<String, String> references = node.getReferences();

        if (references.isEmpty()) {
            return;
        }

        html.line();
        html.raw("<hr />").line();
        html.raw("<div class=\"link-references\">").line();
        html.raw("<p><strong>References:</strong></p>").line();
        html.raw("<ol>").line();

        // Sort by reference number
        references.entrySet().stream()
                .sorted((a, b) -> Integer.compare(
                        Integer.parseInt(a.getKey()),
                        Integer.parseInt(b.getKey())
                ))
                .forEach(entry -> {
                    String url = entry.getValue();
                    html.raw("<li><a href=\"")
                        .raw(escapeHtml(url))
                        .raw("\">")
                        .raw(escapeHtml(url))
                        .raw("</a></li>")
                        .line();
                });

        html.raw("</ol>").line();
        html.raw("</div>").line();
    }

    private String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;");
    }

    public static class Factory implements NodeRendererFactory {
        @Override
        public @NotNull NodeRenderer apply(@NotNull DataHolder options) {
            return new MailingListLinkRefRenderer(options);
        }
    }
}
