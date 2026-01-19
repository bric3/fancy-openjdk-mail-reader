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
@param dev.brice.fancymail.model.ThreadTree threadTree = null
@param String error = null
@param String stackTrace = null
@param dev.brice.fancymail.config.Messages.Threads msg = null
@param boolean devMode = false
@param boolean hideBot = true
@param dev.brice.fancymail.config.PathsConfig paths = null

!{var isBotMessage = dev.brice.fancymail.model.BotMessageFilter.instance();}

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
    <link rel="stylesheet" href="/thread-tree.css">
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
        .header {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            margin: -40px -20px 30px -20px;
            padding: 20px;
        }
        .header a {
            color: white;
            text-decoration: none;
            font-weight: 600;
            display: inline-flex;
            align-items: center;
            gap: 8px;
        }
        .header a:hover {
            text-decoration: underline;
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
        .thread-count {
            font-size: 0.8em;
            color: var(--text-secondary);
            background: var(--action-bg);
            padding: 2px 8px;
            border-radius: 12px;
            white-space: nowrap;
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
        }
        @media (max-width: 480px) {
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
    <div class="header">
        <a href="/">&#8592; ${msg.back()}</a>
    </div>

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
            <p><a href="/">&#8592; ${msg.errorGoback()}</a></p>
        @else
            <h1>${list}</h1>
            <p class="subtitle">${yearMonth}</p>

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
                !{var url = paths != null ? paths.toRenderedPath(list, yearMonth, root.id()) : "/" + list + "/" + yearMonth + "/" + root.id() + ".html";}
                <li class="thread-item">
                    <details>
                        <summary class="thread-item-header">
                            <i class="fa-solid fa-message thread-icon"></i>
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
