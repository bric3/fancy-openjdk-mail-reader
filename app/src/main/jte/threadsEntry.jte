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
@param String parentSubject = ""
@param dev.brice.fancymail.config.PathsConfig paths = null

!{var displaySubject = entry.subject().equals(parentSubject) ? null : entry.subject();}
!{var url = (paths != null ? paths.toRenderedPath(list, yearMonth, entry.id()) : "/" + list + "/" + yearMonth + "/" + entry.id() + ".html") + "?thread=open";}

<li class="thread-entry">
    <div class="thread-entry-content">
        <a href="${url}" class="thread-link">
            @if(displaySubject != null)
            <span class="thread-subject">${displaySubject}</span>
            @endif
            <span class="thread-author">${entry.author()}</span>
        </a>
    </div>
    @if(!entry.replies().isEmpty())
    <ul class="thread-replies">
        @for(var reply : entry.replies())
        @template.threadsEntry(entry = reply, list = list, yearMonth = yearMonth, parentSubject = entry.subject(), paths = paths)
        @endfor
    </ul>
    @endif
</li>
