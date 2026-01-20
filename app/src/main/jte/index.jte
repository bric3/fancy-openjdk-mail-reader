<%--
  fancy-mail-openjdk-reader

  Copyright (c) 2026 - Brice Dutheil

  This Source Code Form is subject to the terms of the Mozilla Public
  License, v. 2.0. If a copy of the MPL was not distributed with this
  file, You can obtain one at https://mozilla.org/MPL/2.0/.
--%>
@param String title
@param dev.brice.fancymail.config.Messages.Index msg = null
@param java.util.List<dev.brice.fancymail.config.MailingListsConfig> mailingLists = null
@param String currentMonth = ""
@param dev.brice.fancymail.config.PathsConfig paths = null

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta name="color-scheme" content="light dark">
    <title>${title}</title>
    <link rel="icon" type="image/svg+xml" href="/favicon-light.svg" media="(prefers-color-scheme: light)">
    <link rel="icon" type="image/svg+xml" href="/favicon-dark.svg" media="(prefers-color-scheme: dark)">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.1/css/all.min.css">
    <link rel="stylesheet" href="/styles.css">
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
        .mailing-lists {
            margin-top: 30px;
        }
        .mailing-lists h3 {
            margin: 0 0 15px 0;
            color: var(--text-secondary);
            font-size: 0.9em;
            text-transform: uppercase;
            letter-spacing: 0.5px;
        }
        .mailing-lists-grid {
            display: grid;
            grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
            gap: 10px;
        }
        .mailing-list-item {
            display: block;
            padding: 12px 15px;
            background: var(--card-bg);
            border-radius: 8px;
            text-decoration: none;
            transition: transform 0.2s, box-shadow 0.2s;
        }
        .mailing-list-item:hover {
            transform: translateY(-2px);
            box-shadow: 0 4px 12px var(--accent-shadow);
        }
        .mailing-list-name {
            font-weight: 600;
            color: var(--accent);
            margin-bottom: 4px;
        }
        .mailing-list-desc {
            font-size: 0.85em;
            color: var(--text-secondary);
        }
        .all-lists {
            margin-top: 20px;
        }
        .all-lists details {
            background: var(--card-bg);
            border-radius: 8px;
            padding: 0;
        }
        .all-lists summary {
            padding: 12px 15px;
            cursor: pointer;
            font-size: 0.9em;
            color: var(--text-secondary);
            list-style: none;
            display: flex;
            align-items: center;
            gap: 8px;
        }
        .all-lists summary::-webkit-details-marker {
            display: none;
        }
        .all-lists summary::before {
            content: "▶";
            font-size: 0.7em;
            transition: transform 0.2s;
        }
        .all-lists details[open] summary::before {
            transform: rotate(90deg);
        }
        .all-lists-content {
            padding: 0 15px 15px 15px;
            display: flex;
            flex-wrap: wrap;
            gap: 6px;
        }
        .all-lists-link {
            display: inline-block;
            padding: 4px 10px;
            background: var(--code-bg);
            border-radius: 4px;
            font-size: 0.8em;
            font-family: 'SF Mono', Monaco, monospace;
            color: var(--text-primary);
            text-decoration: none;
            transition: background 0.2s, color 0.2s;
        }
        .all-lists-link:hover {
            background: var(--accent);
            color: white;
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

        @if(mailingLists != null && !mailingLists.isEmpty())
        <div class="mailing-lists">
            <h3>Browse Mailing Lists</h3>
            <div class="mailing-lists-grid">
                @for(var mailingList : mailingLists)
                <a href="${paths.toThreadsPath(mailingList.name(), currentMonth)}" class="mailing-list-item">
                    <div class="mailing-list-name">${mailingList.name()}</div>
                    <div class="mailing-list-desc">${mailingList.descriptionNbsp()}</div>
                </a>
                @endfor
            </div>
            <div class="all-lists">
                <details>
                    <summary>All OpenJDK mailing lists</summary>
                    <div class="all-lists-content">
                        !{var allLists = java.util.List.of(
                            "2d-dev", "aarch32-port-dev", "aarch64-port-dev", "adoption-discuss", "amber-dev",
                            "amber-spec-comments", "amber-spec-experts", "amber-spec-observers", "anno-pipeline-dev",
                            "announce", "apidiff-dev", "asmtools-dev", "audio-engine-dev", "awt-dev", "babylon-dev",
                            "beans-dev", "bsd-port-dev", "btrace-dev", "build-dev", "build-infra-dev",
                            "caciocavallo-dev", "classfile-api-dev", "client-libs-dev", "code-tools-dev",
                            "coin-dev", "compiler-dev", "compiler-grammar-dev", "core-libs-dev", "crac-dev",
                            "csr-discuss", "dio-dev", "discuss", "distro-pkg-dev", "doccheck-dev", "duke-dev",
                            "galahad-dev", "graal-dev", "graphics-rasterizer-dev", "guide-dev", "guide-discuss",
                            "haiku-port-dev", "harfbuzz-dev", "hotspot-compiler-dev", "hotspot-dev", "hotspot-gc-dev",
                            "hotspot-jfr-dev", "hotspot-runtime-dev", "i18n-dev", "ide-support-dev", "javadoc-dev",
                            "jcov-dev", "jcstress-dev", "jdk-dev", "jdk-updates-dev", "jemmy-dev", "jextract-dev",
                            "jigsaw-dev", "jmc-dev", "jmh-dev", "jmh-jdk-microbenchmarks-dev", "jmx-dev", "jol-dev",
                            "jtharness-dev", "jtreg-dev", "jtreg-use", "kona-dev", "kulla-dev", "lanai-dev",
                            "leyden-dev", "lilliput-dev", "loom-dev", "macosx-port-dev", "members", "metropolis-dev",
                            "mlvm-dev", "mobile-dev", "nashorn-dev", "net-dev", "nio-dev", "openjfx-dev",
                            "panama-dev", "panama-spec-experts", "penrose-dev", "porters-dev", "portola-dev",
                            "ppc-aix-port-dev", "quality-discuss", "riscv-port-dev", "s390x-port-dev", "sctp-dev",
                            "security-dev", "serviceability-dev", "shenandoah-dev", "sigtest-dev", "skara-dev",
                            "sound-dev", "swing-dev", "threeten-dev", "tsan-dev", "type-annotations-dev",
                            "valhalla-dev", "valhalla-spec-experts", "verona-dev", "wakefield-dev", "webrev-dev",
                            "zero-dev", "zgc-dev"
                        );}
                        @for(var list : allLists)
                        <a href="${paths.toThreadsPath(list, currentMonth)}" class="all-lists-link">${list}</a>
                        @endfor
                    </div>
                </details>
            </div>
        </div>
        @endif

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
