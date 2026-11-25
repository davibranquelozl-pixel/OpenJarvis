# Jarvis EDU

Jarvis EDU is an assistive study workspace: a responsive web app, a Manifest V3 browser extension, and a small local API. It helps organize school tasks, explain questions, draft answers for student review, and generate/debug code.

## Safety and platform boundaries
- No CAPTCHA bypass, stealth, detector evasion, credential collection, or automatic submission of graded work.
- The extension only captures the selected text after the user clicks its button.
- Review generated work and follow each platform's rules.
- Use official APIs and permitted integrations; platform adapters remain opt-in and must respect terms of service.

## Requirements
- Node.js 20+
- An OpenAI-compatible API key (optional; without it, the UI and local task organizer still work).

## Run locally
```bash
cd jarvis-edu
cp .env.example .env
# Set AI_API_KEY and optionally AI_MODEL in .env
node server.mjs
```
Open http://localhost:8787

The server uses Node's built-in modules only. Environment variables are loaded by Node 20.6+ with `node --env-file=.env server.mjs`; alternatively export them in your shell and run `node server.mjs`.

## Load the extension
1. Open `chrome://extensions`.
2. Enable Developer mode.
3. Choose **Load unpacked** and select `jarvis-edu/extension`.
4. Open a supported learning page, select the text you want help with, click the extension icon, then **Import selected text**.
5. Open Jarvis EDU at http://localhost:8787 and review the imported task.

## Current capabilities
- Task workspace with platform tagging and local browser storage.
- AI-assisted explanation, draft answer, and code generation through an OpenAI-compatible endpoint.
- Selected-text capture through an explicit extension action.
- Health endpoint and configurable local AI provider.

## Roadmap
- Add platform-specific adapters only after verifying official APIs, permissions, and terms.
- Add tests, authentication for remote deployment, encrypted secrets, and user-controlled task history.
- Add accessibility, export, and teacher-approved classroom workflows.

## Configuration
See `.env.example`. Never commit real API keys or student credentials.
