# Security notes

- Keep `.env` private. Never commit API keys, passwords, school credentials, or session cookies.
- The server binds to loopback (127.0.0.1) by default. Keep it local; do not expose port 8787 to the public internet.
- The API rejects browser origins other than the local app and Chrome extension origins.
- The extension reads only the current page's selected text after the user presses its action.
- Do not add automated login, CAPTCHA bypass, stealth, detector evasion, or automatic submission to graded platforms.
- Before adding a platform adapter, verify its official API, terms, school policy, and user authorization.
- For remote hosting, add authentication, HTTPS, per-user authorization, rate limiting, secure secret storage, and privacy controls first.
