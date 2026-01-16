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
    <title>${title}</title>
    <style>
        * {
            box-sizing: border-box;
        }
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, sans-serif;
            max-width: 800px;
            margin: 0 auto;
            padding: 40px 20px;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            min-height: 100vh;
        }
        .container {
            background: white;
            border-radius: 16px;
            padding: 40px;
            box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
        }
        h1 {
            margin: 0 0 10px 0;
            color: #333;
            font-size: 2em;
        }
        .subtitle {
            color: #666;
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
            color: #444;
        }
        input[type="url"] {
            padding: 15px;
            font-size: 16px;
            border: 2px solid #e0e0e0;
            border-radius: 8px;
            transition: border-color 0.2s, box-shadow 0.2s;
        }
        input[type="url"]:focus {
            outline: none;
            border-color: #667eea;
            box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.2);
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
            background: #f8f9fa;
            border-radius: 8px;
            font-size: 0.9em;
        }
        .example h3 {
            margin: 0 0 10px 0;
            color: #666;
            font-size: 0.9em;
            text-transform: uppercase;
            letter-spacing: 0.5px;
        }
        .example code {
            background: #e9ecef;
            padding: 2px 6px;
            border-radius: 4px;
            font-family: 'SF Mono', Monaco, monospace;
            word-break: break-all;
        }
        .features {
            margin-top: 30px;
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
            gap: 15px;
        }
        .feature {
            padding: 15px;
            background: #f8f9fa;
            border-radius: 8px;
        }
        .feature h4 {
            margin: 0 0 5px 0;
            color: #667eea;
        }
        .feature p {
            margin: 0;
            color: #666;
            font-size: 0.9em;
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
