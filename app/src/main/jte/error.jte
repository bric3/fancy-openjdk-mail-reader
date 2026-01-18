<%--
  fancy-mail-openjdk-reader

  Copyright (c) 2026 - Brice Dutheil

  This Source Code Form is subject to the terms of the Mozilla Public
  License, v. 2.0. If a copy of the MPL was not distributed with this
  file, You can obtain one at https://mozilla.org/MPL/2.0/.
--%>
@param String title
@param int status
@param String message
@param String path = null
@param String stackTrace = null
@param boolean devMode = false

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta name="color-scheme" content="light dark">
    <title>${title} - Fancy Mail</title>
    <style>
        :root {
            color-scheme: light dark;
            --bg-gradient-start: #667eea;
            --bg-gradient-end: #764ba2;
            --container-bg: white;
            --container-shadow: rgba(0, 0, 0, 0.3);
            --text-primary: #333;
            --text-secondary: #666;
            --accent: #667eea;
            --error-bg: #fee;
            --error-border: #fcc;
            --error-text: #c00;
            --code-bg: #f6f8fa;
            --code-border: #e1e4e8;
            --card-bg: #f8f9fa;
            --action-hover: #e0e0e0;
        }
        @media (prefers-color-scheme: dark) {
            :root {
                --bg-gradient-start: #1a1a2e;
                --bg-gradient-end: #16213e;
                --container-bg: #1e1e2e;
                --container-shadow: rgba(0, 0, 0, 0.5);
                --text-primary: #e0e0e0;
                --text-secondary: #a0a0a0;
                --accent: #8b9fef;
                --error-bg: #3d1f1f;
                --error-border: #5c2a2a;
                --error-text: #ff6b6b;
                --code-bg: #2d2d3d;
                --code-border: #3a3a4a;
                --card-bg: #2a2a3a;
                --action-hover: #3a3a4a;
            }
        }
        * {
            box-sizing: border-box;
        }
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, sans-serif;
            max-width: 800px;
            margin: 0 auto;
            padding: 40px 20px;
            background: linear-gradient(135deg, var(--bg-gradient-start) 0%, var(--bg-gradient-end) 100%);
            min-height: 100vh;
            color: var(--text-primary);
        }
        .container {
            background: var(--container-bg);
            border-radius: 16px;
            padding: 40px;
            box-shadow: 0 20px 60px var(--container-shadow);
        }
        h1 {
            margin: 0 0 10px 0;
            color: var(--text-primary);
            font-size: 2em;
        }
        .status-code {
            font-size: 4em;
            font-weight: bold;
            color: var(--error-text);
            margin-bottom: 10px;
        }
        .error-message {
            background: var(--error-bg);
            border: 1px solid var(--error-border);
            color: var(--error-text);
            padding: 20px;
            border-radius: 8px;
            margin: 20px 0;
        }
        .back-link {
            display: inline-block;
            margin-top: 20px;
            padding: 12px 24px;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            text-decoration: none;
            border-radius: 8px;
            font-weight: 600;
            transition: transform 0.2s, box-shadow 0.2s;
        }
        .back-link:hover {
            transform: translateY(-2px);
            box-shadow: 0 5px 20px rgba(102, 126, 234, 0.4);
        }
        .dev-details {
            margin-top: 30px;
            background: var(--code-bg);
            border: 1px solid var(--code-border);
            border-radius: 8px;
            overflow: hidden;
        }
        .dev-details summary {
            padding: 12px 16px;
            cursor: pointer;
            font-weight: 600;
            color: var(--text-secondary);
            background: var(--card-bg);
        }
        .dev-details summary:hover {
            background: var(--action-hover);
        }
        .dev-content {
            padding: 16px;
        }
        .dev-content pre {
            margin: 0;
            font-size: 0.8em;
            overflow-x: auto;
            white-space: pre-wrap;
            word-break: break-all;
            font-family: 'SF Mono', Monaco, monospace;
        }
        .dev-content .path {
            margin-bottom: 12px;
            padding-bottom: 12px;
            border-bottom: 1px solid var(--code-border);
        }
        .dev-content .path-label {
            font-weight: 600;
            color: var(--text-secondary);
        }
        .dev-content .path-value {
            font-family: 'SF Mono', Monaco, monospace;
            color: var(--text-primary);
        }
        @media (max-width: 768px) {
            body {
                padding: 20px 12px;
            }
            .container {
                padding: 24px 20px;
                border-radius: 12px;
            }
            .status-code {
                font-size: 3em;
            }
            h1 {
                font-size: 1.6em;
            }
        }
        @media (max-width: 480px) {
            body {
                padding: 12px 8px;
            }
            .container {
                padding: 20px 16px;
                border-radius: 8px;
            }
            .status-code {
                font-size: 2.5em;
            }
            h1 {
                font-size: 1.4em;
            }
        }
    </style>
</head>
<body>
    <div class="container">
        <div class="status-code">${status}</div>
        <h1>${title}</h1>

        <div class="error-message">
            ${message}
        </div>

        <a href="/" class="back-link">Back to Home</a>

        @if(devMode && (path != null || stackTrace != null))
        <details class="dev-details">
            <summary>Debug Information (dev mode)</summary>
            <div class="dev-content">
                @if(path != null)
                <div class="path">
                    <span class="path-label">Path:</span>
                    <span class="path-value">${path}</span>
                </div>
                @endif
                @if(stackTrace != null)
                <pre>${stackTrace}</pre>
                @endif
            </div>
        </details>
        @endif
    </div>
</body>
</html>
