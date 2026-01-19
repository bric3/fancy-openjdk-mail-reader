<%--
  fancy-mail-openjdk-reader

  Copyright (c) 2026 - Brice Dutheil

  This Source Code Form is subject to the terms of the Mozilla Public
  License, v. 2.0. If a copy of the MPL was not distributed with this
  file, You can obtain one at https://mozilla.org/MPL/2.0/.
--%>
@param dev.brice.fancymail.model.ThreadEntry entry
@param String list
@param String yearMonth
@param String currentId = ""
@param String parentSubject = ""
@param dev.brice.fancymail.config.PathsConfig paths = null
@param boolean useHtmx = false

!{var isCurrent = currentId != null && !currentId.isEmpty() && entry.id().equals(currentId);}
!{var displaySubject = entry.subject().equals(parentSubject) ? null : entry.subject();}
!{var url = (paths != null ? paths.toRenderedPath(list, yearMonth, entry.id()) : "/" + list + "/" + yearMonth + "/" + entry.id() + ".html") + "?thread=open";}

<li class="thread-entry${isCurrent ? " current" : ""}">
    <div class="thread-entry-content">
        @if(isCurrent)
        <span class="thread-current-marker"><i class="fa-solid fa-arrow-right"></i></span>
        @endif
        @if(useHtmx)
        <a href="${url}" class="thread-link${isCurrent ? " current" : ""}"
           hx-get="${url}" hx-target="#mail-container" hx-select="#mail-container" hx-swap="outerHTML" hx-push-url="true">
            @if(displaySubject != null)
            <span class="thread-subject">${displaySubject}</span>
            @endif
            <span class="thread-author">${entry.author()}</span>
        </a>
        @else
        <a href="${url}" class="thread-link${isCurrent ? " current" : ""}">
            @if(displaySubject != null)
            <span class="thread-subject">${displaySubject}</span>
            @endif
            <span class="thread-author">${entry.author()}</span>
        </a>
        @endif
    </div>
    @if(!entry.replies().isEmpty())
    <ul class="thread-replies">
        @for(var reply : entry.replies())
        @template.threadEntry(entry = reply, list = list, yearMonth = yearMonth, currentId = currentId, parentSubject = entry.subject(), paths = paths, useHtmx = useHtmx)
        @endfor
    </ul>
    @endif
</li>
