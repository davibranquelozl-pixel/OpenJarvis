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

## Run on Android with Termux

1. Install Termux from its official F-Droid or GitHub distribution (avoid outdated Play Store builds).
2. In Termux run:
   ```bash
   pkg update && pkg install nodejs-lts git
   git clone --branch feat/jarvis-edu --single-branch https://github.com/davibranquelozl-pixel/OpenJarvis.git
   cd OpenJarvis/jarvis-edu
   cp .env.example .env
   bash scripts/start-android.sh
   ```
3. Open `http://localhost:8787` in Android Chrome.
4. Use the browser menu to **Add to Home screen** or **Install app**.

The web app works on Android through the PWA. The Chrome extension is for desktop Chromium browsers; Chrome for Android does not support desktop extensions. The AI assistant requires your own compatible API key in the private `.env` file; never send that key in chat or commit it to GitHub.

## Load the extension
1. Use desktop Google Chrome or another Chromium browser with Manifest V3 extension support. Chrome for Android does not support desktop extensions.
2. Open `chrome://extensions`.
3. Enable Developer mode.
4. Choose **Load unpacked** and select `jarvis-edu/extension`.
5. Open a supported learning page, select the text you want help with, click the extension icon, then **Import selected text**.
6. Open Jarvis EDU at http://localhost:8787 and review the imported task.

## Current capabilities
- Task workspace with platform tagging, local browser storage, JSON export, and clear-history controls.
- AI-assisted explanation, draft answer, code generation, and screenshot analysis through a multimodal OpenAI-compatible endpoint.
- Selected-text capture through an explicit extension action.
- Optional PNG/JPEG/WebP screenshot input (up to 5 MB), with base64, size, and file-signature validation; processed in memory and not stored by the server.
- AI provider requests have a 90-second timeout and return a safe timeout message.
- Health endpoint and configurable local AI provider.

## Roadmap
- Add platform-specific adapters only after verifying official APIs, permissions, school policies, and terms. Current platforms are labels and user-driven text import, not direct integrations.
- Add authentication, encrypted secrets, and user-controlled task history before any remote deployment.
- Add accessibility, export, and teacher-approved classroom workflows.

## Integration status

Alura, Sala do Futuro, Khan Academy, and Matific can be selected as task labels. This release does not log in to these services, read their private pages automatically, or submit work. See [PLATFORM-INTEGRATIONS.md](PLATFORM-INTEGRATIONS.md) for the reviewed public API status and references.

## Configuration
See `.env.example`. Never commit real API keys or student credentials.

## Security and deployment

The server is intentionally local-first and binds to 127.0.0.1 by default. Do not expose it directly to the public internet. The included Compose file binds the host port to 127.0.0.1. Remote deployment is not enabled until authentication, HTTPS, per-user authorization, rate limits, and managed secrets are implemented. See [SECURITY.md](SECURITY.md).

Run `npm test` for smoke tests. For local Docker use `docker compose up`.
