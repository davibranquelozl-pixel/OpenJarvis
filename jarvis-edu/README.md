# Jarvis EDU

Jarvis EDU is an assistive study workspace: a responsive web app, a Manifest V3 browser extension, and a small local API. It helps organize school tasks, explain questions, draft answers for student review, and generate/debug code.

## Safety and platform boundaries
- No CAPTCHA bypass, stealth, detector evasion, credential collection, or automatic submission of graded work.
- The extension only captures the selected text after the user clicks its button.
- Review generated work and follow each platform's rules.
- Use official APIs and permitted integrations; platform adapters remain opt-in and must respect terms of service.

## Requirements
- Node.js 22.9+
- An OpenAI-compatible API key (optional; without it, the UI and local task organizer still work).

## Run locally
```bash
cd jarvis-edu
cp .env.example .env
# Set AI_API_KEY and optionally AI_MODEL in .env
npm start
```
Open http://localhost:8787

The server uses Node's built-in modules only. `npm start` loads `.env` with Node 22.9+.

## Install as an app (PWA)

When opened from `localhost` or an HTTPS host, Jarvis EDU can be installed from the browser menu using **Install app** or **Add to Home screen**. The app shell is cached for offline display; AI requests still require the local server and network access.

## Load the extension
1. Use desktop Google Chrome or another Chromium browser with Manifest V3 extension support. Chrome for Android does not support desktop extensions.
2. Open `chrome://extensions`.
3. Enable Developer mode.
4. Choose **Load unpacked** and select `jarvis-edu/extension`.
5. Open a supported learning page, select the text you want help with, click the extension icon, then **Import selected text**.
6. Open Jarvis EDU at http://localhost:8787 and review the imported task.

## Current capabilities
- Task workspace with platform tagging and local browser storage.
- AI-assisted explanation, draft answer, and code generation through an OpenAI-compatible endpoint.
- Selected-text capture through an explicit extension action.
- Health endpoint and configurable local AI provider.

## Roadmap
- Add platform-specific adapters only after verifying official APIs, permissions, and terms.
- Add platform adapters only when official APIs and permissions are verified.
- Add authentication, encrypted secrets, and user-controlled task history before any remote deployment.
- Add accessibility, export, and teacher-approved classroom workflows.

## Configuration
See `.env.example`. Never commit real API keys or student credentials.

## Security and deployment

The server is intentionally local-first and binds to 127.0.0.1 by default. Do not expose it directly to the public internet. The included Compose file binds the host port to 127.0.0.1. Remote deployment is not enabled until authentication, HTTPS, per-user authorization, rate limits, and managed secrets are implemented. See [SECURITY.md](SECURITY.md).

Run `npm test` for smoke tests. For local Docker use `docker compose up`.
