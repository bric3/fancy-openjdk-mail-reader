<%--
  fancy-mail-openjdk-reader

  Copyright (c) 2026 - Brice Dutheil

  This Source Code Form is subject to the terms of the Mozilla Public
  License, v. 2.0. If a copy of the MPL was not distributed with this
  file, You can obtain one at https://mozilla.org/MPL/2.0/.
--%>
@param dev.brice.fancymail.model.ThreadContext threadContext
@param dev.brice.fancymail.config.Messages.Rendered msg
@param boolean isOpen = false

@if(threadContext != null && threadContext.threadRoot() != null)
<div class="thread-section">
    <details class="thread-details"${isOpen ? " open" : ""}>
        <summary class="thread-summary">
            <span class="thread-title"><i class="fa-solid fa-comments"></i> ${msg.threadTitle()}</span>
            <span class="thread-count">${threadContext.threadMessageCount()} ${msg.threadToggle()}</span>
        </summary>
        <div class="thread-content">
            <ul class="thread-tree">
                @template.threadEntry(entry = threadContext.threadRoot(), currentId = threadContext.currentEntry().id(), list = threadContext.list(), yearMonth = threadContext.yearMonth(), parentSubject = "")
            </ul>
            @if(threadContext.merkleRootHash() != null)
            <div class="thread-merkle">
                <span class="merkle-label">${msg.threadMerkleRoot()}</span>
                <code class="merkle-hash">${threadContext.merkleRootHash().substring(0, 16)}...</code>
            </div>
            @endif
        </div>
    </details>
</div>
@endif
