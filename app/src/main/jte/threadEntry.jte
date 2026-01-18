<%--
  fancy-mail-openjdk-reader

  Copyright (c) 2026 - Brice Dutheil

  This Source Code Form is subject to the terms of the Mozilla Public
  License, v. 2.0. If a copy of the MPL was not distributed with this
  file, You can obtain one at https://mozilla.org/MPL/2.0/.
--%>
@param dev.brice.fancymail.model.ThreadEntry entry
@param String currentId
@param String list
@param String yearMonth
@param String parentSubject = ""

!{var isCurrent = entry.id().equals(currentId);}
!{var displaySubject = entry.subject().equals(parentSubject) ? null : entry.subject();}
!{var url = entry.toRenderedPath(list, yearMonth) + "?thread=open";}

<li class="thread-entry${isCurrent ? " current" : ""}">
    <div class="thread-entry-content">
        @if(isCurrent)
        <span class="thread-current-marker"><i class="fa-solid fa-arrow-right"></i></span>
        @endif
        <a href="${url}" class="thread-link${isCurrent ? " current" : ""}"
           hx-get="${url}" hx-target="#mail-container" hx-select="#mail-container" hx-swap="outerHTML" hx-push-url="true">
            @if(displaySubject != null)
            <span class="thread-subject">${displaySubject}</span>
            @endif
            <span class="thread-author">${entry.author()}</span>
        </a>
    </div>
    @if(!entry.replies().isEmpty())
    <ul class="thread-replies">
        @for(var reply : entry.replies())
        @template.threadEntry(entry = reply, currentId = currentId, list = list, yearMonth = yearMonth, parentSubject = entry.subject())
        @endfor
    </ul>
    @endif
</li>
