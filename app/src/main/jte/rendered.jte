<%--
  fancy-mail-openjdk-reader

  Copyright (c) 2026 - Brice Dutheil

  This Source Code Form is subject to the terms of the Mozilla Public
  License, v. 2.0. If a copy of the MPL was not distributed with this
  file, You can obtain one at https://mozilla.org/MPL/2.0/.
--%>
@param String title
@param dev.brice.fancymail.model.ParsedMail mail = null
@param String error = null
@param String list = ""
@param String yearMonth = ""
@param String id = ""
@param dev.brice.fancymail.config.Messages.Rendered msg = null

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta name="color-scheme" content="light dark">
    <title>${title} - Fancy Mail</title>
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.9.0/styles/github.min.css" media="(prefers-color-scheme: light)">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.9.0/styles/github-dark.min.css" media="(prefers-color-scheme: dark)">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.1/css/all.min.css">
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
            --blockquote-border: #9ca3af;
            --blockquote-bg-1: #f8f9fa;
            --blockquote-bg-2: #f0f1f3;
            --blockquote-bg-3: #e8e9eb;
            --action-bg: #f0f0f0;
            --action-hover: #e0e0e0;
            --disabled-color: #999;
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
                --blockquote-border: #6b7280;
                --blockquote-bg-1: #2a2a3a;
                --blockquote-bg-2: #323242;
                --blockquote-bg-3: #3a3a4a;
                --action-bg: #2a2a3a;
                --action-hover: #3a3a4a;
                --disabled-color: #666;
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
            margin: 0 0 20px 0;
            color: var(--text-heading);
            font-size: 1.8em;
            line-height: 1.3;
        }
        .meta {
            background: var(--card-bg);
            padding: 15px 20px;
            border-radius: 8px;
            margin-bottom: 30px;
            font-size: 0.95em;
        }
        .meta-item {
            margin: 5px 0;
        }
        .meta-label {
            font-weight: 600;
            color: var(--text-secondary);
        }
        .meta-value {
            color: var(--text-primary);
        }
        .meta a {
            color: var(--accent);
            text-decoration: none;
        }
        .meta a:hover {
            text-decoration: underline;
        }
        hr {
            border: none;
            border-top: 1px solid var(--border-color);
            margin: 30px 0;
        }
        .content {
            color: var(--text-primary);
        }
        .content h1, .content h2, .content h3, .content h4 {
            margin-top: 1.5em;
            margin-bottom: 0.5em;
            color: var(--text-heading);
        }
        .content h1 { font-size: 1.6em; border-bottom: 2px solid var(--accent); padding-bottom: 0.3em; }
        .content h2 { font-size: 1.4em; }
        .content h3 { font-size: 1.2em; }
        .content p {
            margin: 1em 0;
        }
        .content ul, .content ol {
            margin: 1em 0;
            padding-left: 2em;
        }
        .content li {
            margin: 0.5em 0;
        }
        .content pre {
            background: var(--code-bg);
            border: 1px solid var(--code-border);
            border-radius: 8px;
            padding: 16px;
            overflow-x: auto;
            font-size: 0.9em;
        }
        .content code {
            font-family: 'SF Mono', Monaco, 'Cascadia Code', monospace;
            font-size: 0.9em;
        }
        .content pre code {
            background: transparent;
            padding: 0;
        }
        .content :not(pre) > code {
            background: var(--inline-code-bg);
            padding: 2px 6px;
            border-radius: 4px;
        }
        .content a {
            color: var(--accent);
            text-decoration: none;
        }
        .content a:hover {
            text-decoration: underline;
        }
        .content blockquote {
            border-left: 4px solid var(--blockquote-border);
            margin: 1em 0;
            padding: 0.5em 1em;
            background: var(--blockquote-bg-1);
            color: var(--text-secondary);
        }
        .content blockquote blockquote {
            background: var(--blockquote-bg-2);
            margin: 0.5em 0;
        }
        .content blockquote blockquote blockquote {
            background: var(--blockquote-bg-3);
        }
        .footer {
            margin-top: 40px;
            padding-top: 20px;
            border-top: 1px solid var(--border-color);
            font-size: 0.9em;
            color: var(--text-secondary);
        }
        .footer a {
            color: var(--accent);
        }
        .error {
            background: var(--error-bg);
            border: 1px solid var(--error-border);
            color: var(--error-text);
            padding: 20px;
            border-radius: 8px;
            margin: 20px 0;
        }
        .actions {
            display: flex;
            gap: 15px;
            margin-top: 20px;
        }
        .actions a {
            padding: 8px 16px;
            background: var(--action-bg);
            border-radius: 6px;
            color: var(--text-secondary);
            text-decoration: none;
            font-size: 0.9em;
        }
        .actions a:hover {
            background: var(--action-hover);
        }
        .nav-section {
            background: var(--card-bg);
            border-radius: 8px;
            padding: 12px 16px;
            margin: 20px 0;
            font-size: 0.9em;
        }
        .nav-row {
            display: flex;
            flex-direction: column;
            gap: 10px;
        }
        .nav-messages {
            display: flex;
            justify-content: space-between;
            gap: 20px;
            padding-bottom: 10px;
            border-bottom: 1px solid var(--border-color);
        }
        .nav-indexes {
            display: flex;
            justify-content: center;
            gap: 8px;
            color: var(--text-secondary);
        }
        .nav-link {
            color: var(--accent);
            text-decoration: none;
        }
        .nav-link:hover {
            text-decoration: underline;
        }
        .nav-link.disabled {
            color: var(--disabled-color);
            cursor: default;
        }
        .nav-link.disabled:hover {
            text-decoration: none;
        }
        .nav-indexes a {
            color: var(--accent);
            text-decoration: none;
        }
        .nav-indexes a:hover {
            text-decoration: underline;
        }
        .external-icon {
            font-size: 0.75em;
            margin-left: 3px;
            opacity: 0.7;
        }
        /* Tables */
        .content table {
            border-collapse: collapse;
            width: 100%;
            margin: 1em 0;
        }
        .content th, .content td {
            border: 1px solid var(--border-color);
            padding: 8px 12px;
            text-align: left;
        }
        .content th {
            background: var(--card-bg);
            font-weight: 600;
        }
        .table-wrapper {
            overflow-x: auto;
            -webkit-overflow-scrolling: touch;
            margin: 1em 0;
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
            .meta {
                padding: 12px 14px;
            }
            .meta-item {
                display: flex;
                flex-direction: column;
                gap: 2px;
            }
            .content pre {
                padding: 12px;
                font-size: 0.85em;
                margin-left: -16px;
                margin-right: -16px;
                border-radius: 0;
                border-left: none;
                border-right: none;
            }
            .content blockquote {
                margin-left: 0;
                margin-right: 0;
                padding: 0.5em 0.75em;
            }
            .nav-section {
                padding: 10px 12px;
            }
            .nav-messages {
                flex-direction: column;
                gap: 10px;
                text-align: center;
            }
            .nav-indexes {
                flex-wrap: wrap;
                justify-content: center;
            }
            .actions {
                flex-wrap: wrap;
            }
            .actions a {
                flex: 1 1 auto;
                text-align: center;
            }
            /* Full-width tables on mobile with horizontal scroll */
            .content table {
                display: block;
                overflow-x: auto;
                -webkit-overflow-scrolling: touch;
                white-space: nowrap;
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
            .content pre {
                margin-left: -12px;
                margin-right: -12px;
                padding: 10px;
                font-size: 0.8em;
            }
            .meta {
                font-size: 0.9em;
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
            <p><a href="/">&#8592; ${msg.errorGoback()}</a></p>
        @else
            <h1>${mail.subject()}</h1>

            <div class="meta">
                <div class="meta-item">
                    <span class="meta-label">${msg.metaFrom()}</span>
                    <span class="meta-value">${mail.from()}@if(mail.email() != null) (${mail.email()})@endif</span>
                </div>
                <div class="meta-item">
                    <span class="meta-label">${msg.metaDate()}</span>
                    <span class="meta-value">${mail.date()}</span>
                </div>
                <div class="meta-item">
                    <span class="meta-label">${msg.metaList()}</span>
                    <span class="meta-value">${mail.list()}</span>
                </div>
                <div class="meta-item">
                    <span class="meta-label">${msg.metaOriginal()}</span>
                    <a href="${mail.originalUrl()}" target="_blank">${mail.originalUrl()} <i class="fa-solid fa-arrow-up-right-from-square external-icon"></i></a>
                </div>
            </div>

            @if(mail.navigation() != null)
            <div class="nav-section">
                <div class="nav-row">
                    <div class="nav-messages">
                        @if(mail.navigation().prevMessage() != null)
                        <span>&#8592; <a href="${mail.navigation().prevMessage().url()}" class="nav-link">${mail.navigation().prevMessage().title()}</a></span>
                        @else
                        <span class="nav-link disabled">&#8592; ${msg.navNoPrevious()}</span>
                        @endif
                        @if(mail.navigation().nextMessage() != null)
                        <span><a href="${mail.navigation().nextMessage().url()}" class="nav-link">${mail.navigation().nextMessage().title()}</a> &#8594;</span>
                        @else
                        <span class="nav-link disabled">${msg.navNoNext()} &#8594;</span>
                        @endif
                    </div>
                    <div class="nav-indexes">
                        <span>${msg.navSortBy()}</span>
                        @if(mail.navigation().dateIndexUrl() != null)
                        <a href="${mail.navigation().dateIndexUrl()}" target="_blank">${msg.navDate()} <i class="fa-solid fa-arrow-up-right-from-square external-icon"></i></a>
                        @endif
                        @if(mail.navigation().threadIndexUrl() != null)
                        <a href="${mail.navigation().threadIndexUrl()}" target="_blank">${msg.navThread()} <i class="fa-solid fa-arrow-up-right-from-square external-icon"></i></a>
                        @endif
                        @if(mail.navigation().subjectIndexUrl() != null)
                        <a href="${mail.navigation().subjectIndexUrl()}" target="_blank">${msg.navSubject()} <i class="fa-solid fa-arrow-up-right-from-square external-icon"></i></a>
                        @endif
                        @if(mail.navigation().authorIndexUrl() != null)
                        <a href="${mail.navigation().authorIndexUrl()}" target="_blank">${msg.navAuthor()} <i class="fa-solid fa-arrow-up-right-from-square external-icon"></i></a>
                        @endif
                    </div>
                </div>
            </div>
            @endif

            <hr>

            <div class="content">
                $unsafe{mail.bodyHtml()}
            </div>

            @if(mail.navigation() != null)
            <div class="nav-section">
                <div class="nav-row">
                    <div class="nav-messages">
                        @if(mail.navigation().prevMessage() != null)
                        <span>&#8592; <a href="${mail.navigation().prevMessage().url()}" class="nav-link">${mail.navigation().prevMessage().title()}</a></span>
                        @else
                        <span class="nav-link disabled">&#8592; ${msg.navNoPrevious()}</span>
                        @endif
                        @if(mail.navigation().nextMessage() != null)
                        <span><a href="${mail.navigation().nextMessage().url()}" class="nav-link">${mail.navigation().nextMessage().title()}</a> &#8594;</span>
                        @else
                        <span class="nav-link disabled">${msg.navNoNext()} &#8594;</span>
                        @endif
                    </div>
                    <div class="nav-indexes">
                        <span>${msg.navSortBy()}</span>
                        @if(mail.navigation().dateIndexUrl() != null)
                        <a href="${mail.navigation().dateIndexUrl()}" target="_blank">${msg.navDate()} <i class="fa-solid fa-arrow-up-right-from-square external-icon"></i></a>
                        @endif
                        @if(mail.navigation().threadIndexUrl() != null)
                        <a href="${mail.navigation().threadIndexUrl()}" target="_blank">${msg.navThread()} <i class="fa-solid fa-arrow-up-right-from-square external-icon"></i></a>
                        @endif
                        @if(mail.navigation().subjectIndexUrl() != null)
                        <a href="${mail.navigation().subjectIndexUrl()}" target="_blank">${msg.navSubject()} <i class="fa-solid fa-arrow-up-right-from-square external-icon"></i></a>
                        @endif
                        @if(mail.navigation().authorIndexUrl() != null)
                        <a href="${mail.navigation().authorIndexUrl()}" target="_blank">${msg.navAuthor()} <i class="fa-solid fa-arrow-up-right-from-square external-icon"></i></a>
                        @endif
                    </div>
                </div>
            </div>
            @endif

            <div class="footer">
                <p>
                    <em>${msg.footerSource()} <a href="${mail.originalUrl()}" target="_blank">${mail.list()} ${msg.footerMailingList()} <i class="fa-solid fa-arrow-up-right-from-square external-icon"></i></a></em>
                </p>
                <div class="actions">
                    <a href="/markdown/${list}/${yearMonth}/${id}.md" target="_blank">${msg.footerViewMarkdown()}</a>
                    <a href="${mail.originalUrl()}" target="_blank">${msg.footerViewOriginal()} <i class="fa-solid fa-arrow-up-right-from-square external-icon"></i></a>
                </div>
            </div>
        @endif
    </div>

    <script src="https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.9.0/highlight.min.js"></script>
    <script src="https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.9.0/languages/java.min.js"></script>
    <script>hljs.highlightAll();</script>
</body>
</html>
