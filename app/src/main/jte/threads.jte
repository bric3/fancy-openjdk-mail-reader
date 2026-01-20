<%--
  fancy-mail-openjdk-reader

  Copyright (c) 2026 - Brice Dutheil

  This Source Code Form is subject to the terms of the Mozilla Public
  License, v. 2.0. If a copy of the MPL was not distributed with this
  file, You can obtain one at https://mozilla.org/MPL/2.0/.
--%>
@param String title
@param String list = ""
@param String yearMonth = ""
@param String prevMonth = ""
@param String nextMonth = ""
@param boolean nextMonthDisabled = false
@param dev.brice.fancymail.model.ArchiveIndex archiveIndex = null
@param dev.brice.fancymail.model.ThreadTree threadTree = null
@param String error = null
@param String stackTrace = null
@param dev.brice.fancymail.config.Messages.Threads msg = null
@param boolean devMode = false
@param boolean hideBot = true
@param dev.brice.fancymail.config.PathsConfig paths = null

!{var isBotMessage = dev.brice.fancymail.model.BotMessageFilter.instance();}
!{var currentYearMonth = java.time.YearMonth.now();}

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta name="color-scheme" content="light dark">
    <title>${title} - Fancy Mail</title>
    <link rel="icon" type="image/svg+xml" href="/favicon-light.svg" media="(prefers-color-scheme: light)">
    <link rel="icon" type="image/svg+xml" href="/favicon-dark.svg" media="(prefers-color-scheme: dark)">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.1/css/all.min.css">
    <link rel="stylesheet" href="/styles.css">
    <style>
        :root {
            color-scheme: light dark;
            --bg-page: #f5f5f5;
            --bg-container: white;
            --container-shadow: rgba(0, 0, 0, 0.1);
            --text-primary: #333;
            --text-secondary: #666;
            --text-heading: #222;
            --accent: #667eea;
            --accent-light: rgba(102, 126, 234, 0.2);
            --border-color: #e0e0e0;
            --card-bg: #f8f9fa;
            --code-bg: #f6f8fa;
            --code-border: #e1e4e8;
            --inline-code-bg: #f0f0f0;
            --action-bg: #f0f0f0;
            --action-hover: #e0e0e0;
            --error-bg: #fee;
            --error-border: #fcc;
            --error-text: #c00;
        }
        @media (prefers-color-scheme: dark) {
            :root {
                --bg-page: #121212;
                --bg-container: #1e1e2e;
                --container-shadow: rgba(0, 0, 0, 0.4);
                --text-primary: #e0e0e0;
                --text-secondary: #a0a0a0;
                --text-heading: #f0f0f0;
                --accent: #8b9fef;
                --accent-light: rgba(139, 159, 239, 0.2);
                --border-color: #3a3a4a;
                --card-bg: #2a2a3a;
                --code-bg: #2d2d3d;
                --code-border: #3a3a4a;
                --inline-code-bg: #3a3a4a;
                --action-bg: #2a2a3a;
                --action-hover: #3a3a4a;
                --error-bg: #3d1f1f;
                --error-border: #5c2a2a;
                --error-text: #ff6b6b;
            }
        }
        * {
            box-sizing: border-box;
        }
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, sans-serif;
            max-width: 900px;
            margin: 0 auto;
            padding: 40px 20px;
            background: var(--bg-page);
            line-height: 1.6;
            color: var(--text-primary);
        }
        .container {
            background: var(--bg-container);
            border-radius: 16px;
            padding: 40px;
            box-shadow: 0 4px 20px var(--container-shadow);
        }
        h1 {
            margin: 0 0 10px 0;
            color: var(--text-heading);
            font-size: 1.8em;
            line-height: 1.3;
        }
        .subtitle {
            color: var(--text-secondary);
            margin-bottom: 30px;
            font-size: 1.1em;
        }
        .error {
            background: var(--error-bg);
            border: 1px solid var(--error-border);
            color: var(--error-text);
            padding: 20px;
            border-radius: 8px;
            margin: 20px 0;
        }
        .dev-stack-trace {
            margin: 15px 0;
            background: var(--code-bg);
            border: 1px solid var(--code-border);
            border-radius: 8px;
            overflow: hidden;
        }
        .dev-stack-trace summary {
            padding: 10px 15px;
            cursor: pointer;
            font-weight: 600;
            color: var(--text-secondary);
            background: var(--card-bg);
        }
        .dev-stack-trace summary:hover {
            background: var(--action-hover);
        }
        .stack-trace-content {
            margin: 0;
            padding: 15px;
            font-size: 0.8em;
            overflow-x: auto;
            white-space: pre-wrap;
            word-break: break-all;
        }
        .thread-list {
            list-style: none;
            padding: 0;
            margin: 0;
        }
        .thread-item {
            background: var(--card-bg);
            border-radius: 8px;
            margin-bottom: 12px;
            overflow: hidden;
        }
        .thread-item-header {
            padding: 16px;
            cursor: pointer;
            display: flex;
            align-items: flex-start;
            gap: 12px;
        }
        .thread-item-header:hover {
            background: var(--action-hover);
        }
        .thread-icon {
            color: var(--accent);
            margin-top: 2px;
        }
        .thread-info {
            flex: 1;
            min-width: 0;
        }
        .thread-subject {
            font-weight: 600;
            color: var(--text-primary);
            margin-bottom: 4px;
            word-wrap: break-word;
        }
        .thread-meta {
            display: flex;
            align-items: center;
            gap: 8px;
        }
        .thread-header-author {
            font-size: 0.8em;
            color: var(--text-secondary);
            font-style: italic;
            white-space: nowrap;
            overflow: hidden;
            text-overflow: ellipsis;
            max-width: 150px;
        }
        details[open] .thread-header-author {
            display: none;
        }
        .thread-author {
            font-style: italic;
        }
        .no-threads {
            text-align: center;
            color: var(--text-secondary);
            padding: 40px 20px;
            font-style: italic;
        }
        .stats {
            background: var(--card-bg);
            padding: 15px 20px;
            border-radius: 8px;
            margin-bottom: 20px;
            display: flex;
            gap: 20px;
            flex-wrap: wrap;
        }
        .stat {
            display: flex;
            align-items: center;
            gap: 8px;
        }
        .stat-icon {
            color: var(--accent);
        }
        .stat-value {
            font-weight: 600;
            color: var(--text-primary);
        }
        .stat-label {
            color: var(--text-secondary);
        }
        .filters {
            background: var(--card-bg);
            padding: 12px 20px;
            border-radius: 8px;
            margin-bottom: 20px;
            display: flex;
            align-items: center;
            gap: 12px;
        }
        .filter-label {
            color: var(--text-secondary);
            font-size: 0.9em;
        }
        .filter-toggle {
            display: inline-flex;
            align-items: center;
            gap: 8px;
            padding: 6px 12px;
            background: var(--action-bg);
            border-radius: 6px;
            text-decoration: none;
            font-size: 0.85em;
            color: var(--text-primary);
            transition: background 0.2s;
        }
        .filter-toggle:hover {
            background: var(--action-hover);
        }
        .filter-toggle.active {
            background: var(--accent-light);
            color: var(--accent);
        }
        .filter-toggle i {
            font-size: 0.9em;
        }
        /* Month navigation */
        .month-nav {
            display: flex;
            align-items: center;
            justify-content: space-between;
            gap: 16px;
            margin-bottom: 20px;
        }
        .month-nav-btn {
            display: inline-flex;
            align-items: center;
            gap: 8px;
            padding: 10px 16px;
            background: var(--action-bg);
            border-radius: 8px;
            text-decoration: none;
            font-size: 0.9em;
            color: var(--text-primary);
            transition: background 0.2s, opacity 0.2s;
            border: 1px solid var(--border-color);
        }
        .month-nav-btn:hover:not(.disabled) {
            background: var(--action-hover);
        }
        .month-nav-btn.disabled {
            opacity: 0.4;
            cursor: not-allowed;
            pointer-events: none;
        }
        .month-nav-btn i {
            font-size: 0.85em;
        }
        .month-nav-current {
            text-align: center;
            flex: 1;
        }
        .month-nav-current .month-label {
            font-weight: 600;
            color: var(--text-heading);
            font-size: 1.1em;
        }
        /* Calendar grid */
        .calendar-picker {
            background: var(--card-bg);
            border-radius: 12px;
            margin-bottom: 20px;
            border: 1px solid var(--border-color);
            overflow: hidden;
        }
        .calendar-picker summary {
            padding: 16px 20px;
            cursor: pointer;
            display: flex;
            align-items: center;
            justify-content: space-between;
            gap: 12px;
            font-weight: 600;
            color: var(--text-heading);
            list-style: none;
        }
        .calendar-picker summary::-webkit-details-marker {
            display: none;
        }
        .calendar-picker summary:hover {
            background: var(--action-hover);
        }
        .calendar-picker summary .calendar-title {
            display: flex;
            align-items: center;
            gap: 10px;
        }
        .calendar-picker summary .calendar-icon {
            color: var(--accent);
        }
        .calendar-picker summary .chevron {
            transition: transform 0.2s;
            color: var(--text-secondary);
        }
        .calendar-picker[open] summary .chevron {
            transform: rotate(180deg);
        }
        .calendar-content {
            padding: 0 20px 20px;
        }
        .calendar-year {
            margin-bottom: 16px;
        }
        .calendar-year:last-child {
            margin-bottom: 0;
        }
        .calendar-year-header {
            font-weight: 600;
            color: var(--text-secondary);
            font-size: 0.85em;
            text-transform: uppercase;
            letter-spacing: 0.5px;
            margin-bottom: 8px;
            padding-bottom: 4px;
            border-bottom: 1px solid var(--border-color);
        }
        .calendar-months {
            display: grid;
            grid-template-columns: repeat(6, 1fr);
            gap: 6px;
        }
        .calendar-month {
            padding: 8px 4px;
            text-align: center;
            border-radius: 6px;
            font-size: 0.85em;
            text-decoration: none;
            transition: background 0.2s, transform 0.1s;
        }
        .calendar-month.available {
            background: var(--action-bg);
            color: var(--text-primary);
            cursor: pointer;
        }
        .calendar-month.available:hover {
            background: var(--accent-light);
            color: var(--accent);
            transform: scale(1.05);
        }
        .calendar-month.unavailable {
            color: var(--text-secondary);
            opacity: 0.4;
            cursor: not-allowed;
        }
        .calendar-month.current {
            background: var(--accent);
            color: white;
            font-weight: 600;
        }
        .calendar-month.current:hover {
            background: var(--accent);
            color: white;
            transform: none;
        }
        .calendar-month.future {
            color: var(--text-secondary);
            opacity: 0.3;
        }
        /* Recent months quick access */
        .recent-months {
            display: flex;
            flex-wrap: wrap;
            gap: 8px;
            margin-bottom: 12px;
        }
        .recent-month {
            padding: 8px 14px;
            background: var(--action-bg);
            border-radius: 8px;
            text-decoration: none;
            font-size: 0.85em;
            color: var(--text-primary);
            transition: background 0.2s, transform 0.1s;
            border: 1px solid var(--border-color);
        }
        .recent-month:hover {
            background: var(--accent-light);
            color: var(--accent);
            transform: translateY(-1px);
        }
        .recent-month.current {
            background: var(--accent);
            color: white;
            border-color: var(--accent);
            font-weight: 600;
        }
        .recent-month.current:hover {
            background: var(--accent);
            color: white;
            transform: none;
        }
        .recent-month.unavailable {
            opacity: 0.4;
            cursor: not-allowed;
            pointer-events: none;
        }
        /* Mobile responsiveness */
        @media (max-width: 768px) {
            body {
                padding: 20px 12px;
            }
            .header {
                margin: -20px -12px 20px -12px;
                padding: 15px 12px;
            }
            .container {
                padding: 20px 16px;
                border-radius: 12px;
            }
            h1 {
                font-size: 1.4em;
            }
            .thread-item-header {
                padding: 12px;
            }
            .stats {
                flex-direction: column;
                gap: 10px;
            }
            .month-nav-btn span {
                display: none;
            }
            .calendar-months {
                grid-template-columns: repeat(4, 1fr);
            }
        }
        @media (max-width: 480px) {
            .calendar-months {
                grid-template-columns: repeat(3, 1fr);
            }
            body {
                padding: 12px 8px;
            }
            .header {
                margin: -12px -8px 16px -8px;
            }
            .container {
                padding: 16px 12px;
                border-radius: 8px;
            }
            h1 {
                font-size: 1.25em;
            }
        }
    </style>
</head>
<body>
    @template.header()

    <div class="container">
        @if(error != null)
            <div class="error">
                <strong>${msg.errorLabel()}</strong> ${error}
            </div>
            @if(devMode && stackTrace != null)
            <details class="dev-stack-trace">
                <summary>Stack Trace (dev mode)</summary>
                <pre class="stack-trace-content">${stackTrace}</pre>
            </details>
            @endif
            <h1>${list}</h1>
            <nav class="month-nav">
                <a href="${paths.toThreadsPath(list, prevMonth)}" class="month-nav-btn" title="${msg.prevMonth()}">
                    <i class="fa-solid fa-chevron-left"></i>
                    <span>${prevMonth}</span>
                </a>
                <div class="month-nav-current">
                    <span class="month-label">${yearMonth}</span>
                </div>
                @if(nextMonthDisabled)
                <span class="month-nav-btn disabled" title="${msg.nextMonth()}">
                    <span>${nextMonth}</span>
                    <i class="fa-solid fa-chevron-right"></i>
                </span>
                @else
                <a href="${paths.toThreadsPath(list, nextMonth)}" class="month-nav-btn" title="${msg.nextMonth()}">
                    <span>${nextMonth}</span>
                    <i class="fa-solid fa-chevron-right"></i>
                </a>
                @endif
            </nav>
            @if(archiveIndex != null)
            <div class="recent-months">
                @for(int i = 5; i >= 0; i--)
                !{var recentYm = currentYearMonth.minusMonths(i);}
                !{var recentYmStr = dev.brice.fancymail.model.ArchiveIndex.formatYearMonth(recentYm.getYear(), recentYm.getMonthValue());}
                !{var recentLabel = recentYm.getMonth().getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.ENGLISH) + " " + recentYm.getYear();}
                !{var isRecentCurrent = recentYmStr.equals(yearMonth);}
                !{var isRecentAvailable = archiveIndex.isAvailable(recentYmStr);}
                @if(isRecentCurrent)
                <span class="recent-month current">${recentLabel}</span>
                @elseif(isRecentAvailable)
                <a href="${paths.toThreadsPath(list, recentYmStr)}" class="recent-month">${recentLabel}</a>
                @else
                <span class="recent-month unavailable">${recentLabel}</span>
                @endif
                @endfor
            </div>
            <details class="calendar-picker">
                <summary>
                    <span class="calendar-title">
                        <i class="fa-solid fa-calendar-days calendar-icon"></i>
                        <span>Browse archive</span>
                    </span>
                    <i class="fa-solid fa-chevron-down chevron"></i>
                </summary>
                <div class="calendar-content">
                    @for(int year : archiveIndex.years())
                    <div class="calendar-year">
                        <div class="calendar-year-header">${year}</div>
                        <div class="calendar-months">
                            @for(int month = 1; month <= 12; month++)
                            !{var ym = dev.brice.fancymail.model.ArchiveIndex.formatYearMonth(year, month);}
                            !{var shortName = dev.brice.fancymail.model.ArchiveIndex.shortMonthName(month);}
                            !{var isAvailable = archiveIndex.isAvailable(year, month);}
                            !{var isCurrent = ym.equals(yearMonth);}
                            !{var isFuture = java.time.YearMonth.of(year, month).isAfter(currentYearMonth);}
                            @if(isCurrent)
                            <span class="calendar-month current">${shortName}</span>
                            @elseif(isFuture)
                            <span class="calendar-month future">${shortName}</span>
                            @elseif(isAvailable)
                            <a href="${paths.toThreadsPath(list, ym)}" class="calendar-month available">${shortName}</a>
                            @else
                            <span class="calendar-month unavailable">${shortName}</span>
                            @endif
                            @endfor
                        </div>
                    </div>
                    @endfor
                </div>
            </details>
            @endif
            <p><a href="/">&#8592; ${msg.errorGoback()}</a></p>
        @else
            <h1>${list}</h1>
            <nav class="month-nav">
                <a href="${paths.toThreadsPath(list, prevMonth)}" class="month-nav-btn" title="${msg.prevMonth()}">
                    <i class="fa-solid fa-chevron-left"></i>
                    <span>${prevMonth}</span>
                </a>
                <div class="month-nav-current">
                    <span class="month-label">${yearMonth}</span>
                </div>
                @if(nextMonthDisabled)
                <span class="month-nav-btn disabled" title="${msg.nextMonth()}">
                    <span>${nextMonth}</span>
                    <i class="fa-solid fa-chevron-right"></i>
                </span>
                @else
                <a href="${paths.toThreadsPath(list, nextMonth)}" class="month-nav-btn" title="${msg.nextMonth()}">
                    <span>${nextMonth}</span>
                    <i class="fa-solid fa-chevron-right"></i>
                </a>
                @endif
            </nav>

            @if(archiveIndex != null)
            <div class="recent-months">
                @for(int i = 5; i >= 0; i--)
                !{var recentYm = currentYearMonth.minusMonths(i);}
                !{var recentYmStr = dev.brice.fancymail.model.ArchiveIndex.formatYearMonth(recentYm.getYear(), recentYm.getMonthValue());}
                !{var recentLabel = recentYm.getMonth().getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.ENGLISH) + " " + recentYm.getYear();}
                !{var isRecentCurrent = recentYmStr.equals(yearMonth);}
                !{var isRecentAvailable = archiveIndex.isAvailable(recentYmStr);}
                @if(isRecentCurrent)
                <span class="recent-month current">${recentLabel}</span>
                @elseif(isRecentAvailable)
                <a href="${paths.toThreadsPath(list, recentYmStr)}" class="recent-month">${recentLabel}</a>
                @else
                <span class="recent-month unavailable">${recentLabel}</span>
                @endif
                @endfor
            </div>
            <details class="calendar-picker">
                <summary>
                    <span class="calendar-title">
                        <i class="fa-solid fa-calendar-days calendar-icon"></i>
                        <span>Browse archive</span>
                    </span>
                    <i class="fa-solid fa-chevron-down chevron"></i>
                </summary>
                <div class="calendar-content">
                    @for(int year : archiveIndex.years())
                    <div class="calendar-year">
                        <div class="calendar-year-header">${year}</div>
                        <div class="calendar-months">
                            @for(int month = 1; month <= 12; month++)
                            !{var ym = dev.brice.fancymail.model.ArchiveIndex.formatYearMonth(year, month);}
                            !{var shortName = dev.brice.fancymail.model.ArchiveIndex.shortMonthName(month);}
                            !{var isAvailable = archiveIndex.isAvailable(year, month);}
                            !{var isCurrent = ym.equals(yearMonth);}
                            !{var isFuture = java.time.YearMonth.of(year, month).isAfter(currentYearMonth);}
                            @if(isCurrent)
                            <span class="calendar-month current">${shortName}</span>
                            @elseif(isFuture)
                            <span class="calendar-month future">${shortName}</span>
                            @elseif(isAvailable)
                            <a href="${paths.toThreadsPath(list, ym)}" class="calendar-month available">${shortName}</a>
                            @else
                            <span class="calendar-month unavailable">${shortName}</span>
                            @endif
                            @endfor
                        </div>
                    </div>
                    @endfor
                </div>
            </details>
            @endif

            @if(threadTree != null && !threadTree.roots().isEmpty())
            !{var filteredRoots = threadTree.roots().stream()
                .filter(r -> !hideBot || !isBotMessage.test(r.subject()))
                .toList();}
            !{var filteredCount = filteredRoots.stream().mapToInt(r -> r.totalCount()).sum();}
            !{var hiddenCount = threadTree.roots().size() - filteredRoots.size();}

            <div class="stats">
                <div class="stat">
                    <i class="fa-solid fa-comments stat-icon"></i>
                    <span class="stat-value">${filteredRoots.size()}</span>
                    <span class="stat-label">threads</span>
                </div>
                <div class="stat">
                    <i class="fa-solid fa-envelope stat-icon"></i>
                    <span class="stat-value">${filteredCount}</span>
                    <span class="stat-label">${msg.totalMessages()}</span>
                </div>
            </div>

            <div class="filters">
                <span class="filter-label">Filter:</span>
                @if(hideBot)
                <a href="?hideBot=false" class="filter-toggle active">
                    <i class="fa-solid fa-robot"></i>
                    <span>Hide bot messages (${hiddenCount} hidden)</span>
                </a>
                @else
                <a href="?hideBot=true" class="filter-toggle">
                    <i class="fa-regular fa-robot"></i>
                    <span>Hide bot messages</span>
                </a>
                @endif
            </div>

            <ul class="thread-list">
                @for(var root : filteredRoots)
                !{var url = paths.toRenderedPath(list, yearMonth, root.id());}
                <li class="thread-item">
                    <details>
                        <summary class="thread-item-header">
                            <i class="fa-solid fa-comments thread-icon"></i>
                            <div class="thread-info">
                                <div class="thread-subject">${root.subject()}</div>
                            </div>
                            <div class="thread-meta">
                                <span class="thread-header-author">${root.author()}</span>
                                @if(root.totalCount() > 1)
                                <span class="thread-count">${root.totalCount()} ${msg.totalMessages()}</span>
                                @endif
                            </div>
                        </summary>
                        <div class="thread-content">
                            <ul class="thread-tree">
                                @template.threadEntry(entry = root, list = list, yearMonth = yearMonth, parentSubject = root.subject(), paths = paths)
                            </ul>
                        </div>
                    </details>
                </li>
                @endfor
            </ul>
            @else
            <div class="no-threads">
                <i class="fa-solid fa-inbox"></i>
                <p>${msg.noThreads()}</p>
            </div>
            @endif
        @endif
    </div>
</body>
</html>
