<%--
  fancy-mail-openjdk-reader

  Copyright (c) 2026 - Brice Dutheil

  This Source Code Form is subject to the terms of the Mozilla Public
  License, v. 2.0. If a copy of the MPL was not distributed with this
  file, You can obtain one at https://mozilla.org/MPL/2.0/.
--%>
@param String title
@param dev.brice.fancymail.config.Messages.Index msg = null

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta name="color-scheme" content="light dark">
    <title>${title}</title>
    <style>
        :root {
            color-scheme: light dark;
            --bg-gradient-start: #667eea;
            --bg-gradient-end: #764ba2;
            --container-bg: white;
            --container-shadow: rgba(0, 0, 0, 0.3);
            --text-primary: #333;
            --text-secondary: #666;
            --text-label: #444;
            --input-border: #e0e0e0;
            --input-bg: white;
            --accent: #667eea;
            --accent-shadow: rgba(102, 126, 234, 0.2);
            --card-bg: #f8f9fa;
            --code-bg: #e9ecef;
        }
        @media (prefers-color-scheme: dark) {
            :root {
                --bg-gradient-start: #1a1a2e;
                --bg-gradient-end: #16213e;
                --container-bg: #1e1e2e;
                --container-shadow: rgba(0, 0, 0, 0.5);
                --text-primary: #e0e0e0;
                --text-secondary: #a0a0a0;
                --text-label: #c0c0c0;
                --input-border: #3a3a4a;
                --input-bg: #2a2a3a;
                --accent: #8b9fef;
                --accent-shadow: rgba(139, 159, 239, 0.3);
                --card-bg: #2a2a3a;
                --code-bg: #3a3a4a;
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
        .subtitle {
            color: var(--text-secondary);
            margin-bottom: 30px;
            font-size: 1.1em;
        }
        form {
            display: flex;
            flex-direction: column;
            gap: 15px;
        }
        label {
            font-weight: 600;
            color: var(--text-label);
        }
        input[type="url"] {
            padding: 15px;
            font-size: 16px;
            border: 2px solid var(--input-border);
            border-radius: 8px;
            background: var(--input-bg);
            color: var(--text-primary);
            transition: border-color 0.2s, box-shadow 0.2s;
        }
        input[type="url"]:focus {
            outline: none;
            border-color: var(--accent);
            box-shadow: 0 0 0 3px var(--accent-shadow);
        }
        button {
            padding: 15px 30px;
            font-size: 16px;
            font-weight: 600;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            border: none;
            border-radius: 8px;
            cursor: pointer;
            transition: transform 0.2s, box-shadow 0.2s;
        }
        button:hover {
            transform: translateY(-2px);
            box-shadow: 0 5px 20px rgba(102, 126, 234, 0.4);
        }
        button:active {
            transform: translateY(0);
        }
        .example {
            margin-top: 30px;
            padding: 20px;
            background: var(--card-bg);
            border-radius: 8px;
            font-size: 0.9em;
        }
        .example h3 {
            margin: 0 0 10px 0;
            color: var(--text-secondary);
            font-size: 0.9em;
            text-transform: uppercase;
            letter-spacing: 0.5px;
        }
        .example code {
            background: var(--code-bg);
            padding: 2px 6px;
            border-radius: 4px;
            font-family: 'SF Mono', Monaco, monospace;
            word-break: break-all;
            color: var(--text-primary);
        }
        .features {
            margin-top: 30px;
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
            gap: 15px;
        }
        .feature {
            padding: 15px;
            background: var(--card-bg);
            border-radius: 8px;
        }
        .feature h4 {
            margin: 0 0 5px 0;
            color: var(--accent);
        }
        .feature p {
            margin: 0;
            color: var(--text-secondary);
            font-size: 0.9em;
        }
        /* Mobile responsiveness */
        @media (max-width: 768px) {
            body {
                padding: 20px 12px;
            }
            .container {
                padding: 24px 20px;
                border-radius: 12px;
            }
            h1 {
                font-size: 1.6em;
            }
            .subtitle {
                font-size: 1em;
            }
            .example code {
                font-size: 0.85em;
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
            h1 {
                font-size: 1.4em;
            }
            input[type="url"] {
                padding: 12px;
            }
            button {
                padding: 12px 20px;
            }
            .example {
                padding: 15px;
            }
            .example code {
                font-size: 0.8em;
            }
            .features {
                gap: 10px;
            }
            .feature {
                padding: 12px;
            }
        }
    </style>
</head>
<body>
    <div class="container">
        <h1>${msg.title()}</h1>
        <p class="subtitle">${msg.subtitle()}</p>

        <form action="/go" method="post">
            <label for="url">${msg.formLabel()}</label>
            <input type="url"
                   id="url"
                   name="url"
                   placeholder="${msg.formPlaceholder()}"
                   required
                   autofocus>
            <button type="submit">${msg.formSubmit()}</button>
        </form>

        <div class="example">
            <h3>${msg.exampleTitle()}</h3>
            <p><code>${msg.formPlaceholder()}</code></p>
        </div>

        <div class="features">
            <div class="feature">
                <h4>${msg.featureCleanTitle()}</h4>
                <p>${msg.featureCleanDesc()}</p>
            </div>
            <div class="feature">
                <h4>${msg.featureCodeTitle()}</h4>
                <p>${msg.featureCodeDesc()}</p>
            </div>
            <div class="feature">
                <h4>${msg.featureLinksTitle()}</h4>
                <p>${msg.featureLinksDesc()}</p>
            </div>
        </div>
    </div>
</body>
</html>
