# OpenJarvis Multi-Agent Bridge

An optional, provider-neutral collaboration service that lets an OpenAI model and Anthropic Claude independently analyze the same engineering task, critique each other's proposals, and produce a reconciled implementation plan.

> This bridge is an integration component, not a connection to a user's live ChatGPT or Claude chat sessions. It calls the providers' APIs using separately supplied API keys. API usage may incur charges.

## Architecture

```text
OpenJarvis UI / API
       |
       | HTTPS + Bearer token
       v
Multi-Agent Bridge (FastAPI)
       |                         |
       v                         v
OpenAI API                  Anthropic API
       |                         |
       +---- independent --------+
             analysis
                  |
            cross-review
                  |
          OpenAI synthesis
                  |
       structured engineering report
```

## Features

- Independent first-pass analysis from both models.
- Cross-review: each model receives the other's proposal and identifies gaps.
- Final synthesis with risks, test plan, and next steps.
- Portuguese-friendly prompts; task language is preserved.
- Bearer-token protection, bounded request size, no conversation persistence.
- No automatic code execution, shell access, repository writes, deployment, or merge.
- Provider model names are configurable through environment variables.

## Quick start

Python 3.11+ is recommended.

```bash
cd integrations/multi-agent-bridge
python -m venv .venv
# Linux/macOS:
source .venv/bin/activate
# Windows PowerShell:
# .venv\Scripts\Activate.ps1
pip install -r requirements.txt
cp .env.example .env
```

Set real credentials in the private environment (never commit them):

- `OPENAI_API_KEY`
- `ANTHROPIC_API_KEY`
- `BRIDGE_API_TOKEN` (long random secret)

Then run:

```bash
uvicorn app:app --host 0.0.0.0 --port 8000
```

Health check:

```bash
curl http://localhost:8000/health
```

Collaboration request:

```bash
curl -X POST http://localhost:8000/v1/collaborate \
  -H "Authorization: Bearer YOUR_BRIDGE_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"task":"Analise a arquitetura atual do OpenJarvis e proponha melhorias incrementais.","context":"Android PWA, hospedagem econômica, segurança em primeiro lugar.","language":"pt-BR"}'
```

## Environment variables

See `.env.example`. Defaults are examples only; verify model availability and pricing in the provider dashboards before use.

## Deployment

Deploy as a separate private backend service (for example, a Render web service). Add secrets through the host's environment-variable dashboard. Do not put API keys or the bridge token in browser JavaScript, mobile app bundles, GitHub files, screenshots, or public logs.

If deployed publicly, use HTTPS, rotate the bridge token, set provider spending limits, and consider adding rate limiting, request logging with redaction, and a persistent job queue before exposing it to multiple users.

## Integration contract

The host application sends a task to `POST /v1/collaborate`. The response contains:
- `openai_initial`
- `claude_initial`
- `openai_review`
- `claude_review`
- `synthesis`

The bridge is stateless. The OpenJarvis application remains responsible for its own memory, permissions, user interface, and approval workflow.

## Safety and change control

Treat model output as proposals, not trusted commands. Review diffs, run tests in an isolated environment, and require the owner to approve repository writes, secrets changes, deployments, external messages, purchases, or destructive actions. Never pass untrusted user content to a shell or tool without validation.

## License

This integration directory is intended to be distributed under the parent repository's license.