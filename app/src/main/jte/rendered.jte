@param String title
@param dev.brice.fancymail.model.ParsedMail mail = null
@param String error = null
@param String list = ""
@param String yearMonth = ""
@param String id = ""

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>${title} - Fancy Mail</title>
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.9.0/styles/github.min.css">
    <style>
        * {
            box-sizing: border-box;
        }
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, sans-serif;
            max-width: 900px;
            margin: 0 auto;
            padding: 40px 20px;
            background: #f5f5f5;
            line-height: 1.6;
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
            background: white;
            border-radius: 16px;
            padding: 40px;
            box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
        }
        h1 {
            margin: 0 0 20px 0;
            color: #333;
            font-size: 1.8em;
            line-height: 1.3;
        }
        .meta {
            background: #f8f9fa;
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
            color: #666;
        }
        .meta-value {
            color: #333;
        }
        .meta a {
            color: #667eea;
            text-decoration: none;
        }
        .meta a:hover {
            text-decoration: underline;
        }
        hr {
            border: none;
            border-top: 1px solid #e0e0e0;
            margin: 30px 0;
        }
        .content {
            color: #333;
        }
        .content h1, .content h2, .content h3, .content h4 {
            margin-top: 1.5em;
            margin-bottom: 0.5em;
            color: #222;
        }
        .content h1 { font-size: 1.6em; border-bottom: 2px solid #667eea; padding-bottom: 0.3em; }
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
            background: #f6f8fa;
            border: 1px solid #e1e4e8;
            border-radius: 8px;
            padding: 16px;
            overflow-x: auto;
            font-size: 0.9em;
        }
        .content code {
            font-family: 'SF Mono', Monaco, 'Cascadia Code', monospace;
            font-size: 0.9em;
        }
        .content :not(pre) > code {
            background: #f0f0f0;
            padding: 2px 6px;
            border-radius: 4px;
        }
        .content a {
            color: #667eea;
            text-decoration: none;
        }
        .content a:hover {
            text-decoration: underline;
        }
        .content blockquote {
            border-left: 4px solid #667eea;
            margin: 1em 0;
            padding: 0.5em 1em;
            background: #f8f9fa;
            color: #666;
        }
        .footer {
            margin-top: 40px;
            padding-top: 20px;
            border-top: 1px solid #e0e0e0;
            font-size: 0.9em;
            color: #666;
        }
        .footer a {
            color: #667eea;
        }
        .error {
            background: #fee;
            border: 1px solid #fcc;
            color: #c00;
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
            background: #f0f0f0;
            border-radius: 6px;
            color: #666;
            text-decoration: none;
            font-size: 0.9em;
        }
        .actions a:hover {
            background: #e0e0e0;
        }
    </style>
</head>
<body>
    <div class="header">
        <a href="/">&#8592; Back to Fancy Mail</a>
    </div>

    <div class="container">
        @if(error != null)
            <div class="error">
                <strong>Error:</strong> ${error}
            </div>
            <p><a href="/">&#8592; Go back and try another URL</a></p>
        @else
            <h1>${mail.subject()}</h1>

            <div class="meta">
                <div class="meta-item">
                    <span class="meta-label">From:</span>
                    <span class="meta-value">${mail.from()}@if(mail.email() != null) (${mail.email()})@endif</span>
                </div>
                <div class="meta-item">
                    <span class="meta-label">Date:</span>
                    <span class="meta-value">${mail.date()}</span>
                </div>
                <div class="meta-item">
                    <span class="meta-label">List:</span>
                    <span class="meta-value">${mail.list()}</span>
                </div>
                <div class="meta-item">
                    <span class="meta-label">Original:</span>
                    <a href="${mail.originalUrl()}" target="_blank">${mail.originalUrl()}</a>
                </div>
            </div>

            <hr>

            <div class="content">
                $unsafe{mail.bodyHtml()}
            </div>

            <div class="footer">
                <p>
                    <em>Source: <a href="${mail.originalUrl()}" target="_blank">${mail.list()} mailing list</a></em>
                </p>
                <div class="actions">
                    <a href="/markdown/${list}/${yearMonth}/${id}.md" target="_blank">View Markdown</a>
                    <a href="${mail.originalUrl()}" target="_blank">View Original</a>
                </div>
            </div>
        @endif
    </div>

    <script src="https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.9.0/highlight.min.js"></script>
    <script src="https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.9.0/languages/java.min.js"></script>
    <script>hljs.highlightAll();</script>
</body>
</html>
