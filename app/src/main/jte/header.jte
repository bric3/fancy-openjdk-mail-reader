<%--
  fancy-mail-openjdk-reader

  Copyright (c) 2026 - Brice Dutheil

  This Source Code Form is subject to the terms of the Mozilla Public
  License, v. 2.0. If a copy of the MPL was not distributed with this
  file, You can obtain one at https://mozilla.org/MPL/2.0/.
--%>
@param boolean isHome = false
@param String siteName = "Fancy Mail"
@param String navUrl = ""
@param String navText = ""

<div class="header">
    @if(isHome)
    <span class="site-name">${siteName}</span>
    @else
    <a href="/" class="home-link" title="Home"><i class="fa-solid fa-house"></i></a>
    @endif
    @if(navUrl != null && !navUrl.isEmpty())
    <a href="${navUrl}" class="nav-link">
        <i class="fa-solid fa-arrow-left"></i> ${navText}
    </a>
    @endif
</div>
