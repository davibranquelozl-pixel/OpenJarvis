# JARVIS Upgrade Plan — Phase 1 Foundation

This document records a safe, testable path for evolving this OpenJarvis fork into a personal Jarvis assistant. It is deliberately additive: it does not change runtime behavior, deploy services, or introduce new third-party dependencies.

## Current verified baseline

The repository already contains substantial foundations:

- FastAPI server and a React/Vite frontend.
- A PWA build configured with `vite-plugin-pwa` in `frontend/vite.config.ts`.
- An OpenAI-compatible chat API and a `/health` health check used by the Render blueprint.
- Built-in agent modes documented in the root README: simple chat, orchestrator, ReAct, deep research, scheduled digest, continuous operative/monitoring, and code execution.
- A Skills catalog workflow with install/sync support, including public skill sources such as Hermes and OpenClaw.
- Optional connectors, MCP tools, memory backends, speech, browser automation, scheduling, and security-related extras in `pyproject.toml`.
- A Render Blueprint at `render.yaml` that starts the cloud engine with `gpt-4o-mini`.

The existing PWA is an installable web application shell; it does not by itself grant unrestricted Android device control, background execution, always-on microphone access, or native automation privileges.

## Important deployment constraint

The current Render free blueprint uses an ephemeral filesystem. The repository's `docs/deployment/render.md` explicitly warns that configuration, local databases, connector tokens, and learned state under `/home/openjarvis/.openjarvis` can be lost on restart, spin-down, or redeploy.

Therefore:

1. Treat the free Render service as a stateless/demo backend only.
2. Do not store long-lived secrets or irreplaceable memory on its local filesystem.
3. For durable memory, use a supported external datastore or a paid persistent disk mounted at `/home/openjarvis/.openjarvis`.
4. Keep API authentication enabled and restrict CORS to the actual frontend origin before exposing the service publicly.
5. Never commit API keys, OAuth tokens, private URLs containing credentials, or production environment files.

## Target architecture

Keep OpenJarvis as the core runtime and add capabilities through explicit adapters rather than forking or duplicating entire frameworks.

| Layer | Responsibility | Candidate integrations | Acceptance criteria |
|---|---|---|---|
| Core assistant | Identity, chat, tool routing | Existing OpenJarvis agents | Existing chat and health tests remain green |
| Specialist agents | Research, coding, planning, content | Existing agents first; evaluate LangGraph, CrewAI, Agno, OpenHands only where a gap exists | Each agent has a narrow role, tool allowlist, timeout, and trace |
| Memory | User-approved long-term facts and project state | Existing memory interfaces; evaluate Letta or Mem0 as adapters | Data survives restart; export and deletion path documented |
| Skills | Reusable tool instructions | Existing Skills system; Agent Skills standard | Source, version, permissions, and integrity are visible |
| Browser | Research and controlled web workflows | Existing browser tools; evaluate Browser Use/Stagehand | Domain allowlist, action confirmation, audit log |
| Automation | Scheduled workflows | Existing scheduler; evaluate n8n only for external workflows | Idempotent jobs, retry policy, visible run history |
| Models | Cloud/local inference routing | Existing engines; evaluate Ollama | Explicit routing, cost/latency tracking, safe fallback |
| Android | Mobile-first access | Existing PWA; native wrapper only if later needed | Installable, responsive, authenticated, HTTPS |
| Voice | Speech input/output | Existing speech/voice extras | Explicit mic permission, visible recording state, no hidden recording |
| Security | Policy enforcement and audit | Existing capability policy/rate limiter/audit logger | Deny-by-default for sensitive tools; tests cover authorization |

## Implementation order

### Phase 1 — Baseline and safe deployment

- Confirm the deployed commit matches the intended Git branch.
- Confirm `/health` returns healthy and the API rejects missing/invalid credentials.
- Verify the frontend is served from the same trusted origin as the API, or configure exact CORS origins.
- Add a deployment smoke-test checklist and keep secrets in the hosting provider's secret manager.
- Do not promise persistent memory on Render Free.

### Phase 2 — Mobile PWA validation

On Android, open the HTTPS deployment in Chrome, use the browser menu's **Install app** or **Add to Home screen** option, and test:

- Login/API authentication survives a normal page reload as designed.
- Chat sends and receives messages.
- Streaming responses work without the browser being backgrounded.
- Navigation and layout work at narrow screen widths.
- PWA update applies after a new deployment.
- Offline behavior is clearly limited to the cached app shell; API-dependent chat should show a useful connection error.
- Microphone and notification permissions are requested only when the corresponding feature is explicitly used.

PWA installation does not make the web app an unrestricted Android automation agent. Native device actions require Android-supported APIs, user-granted permissions, and a separately implemented bridge.

### Phase 3 — Skills and agent governance

Before installing a skill or agent from a public repository:

1. Inspect its source, license, maintenance status, dependency tree, and install scripts.
2. Identify every tool it can call and every file/network boundary it can access.
3. Pin a reviewed version or commit; avoid floating remote execution.
4. Run it in a restricted environment with least privilege.
5. Require approval for external publishing, purchases, account changes, destructive actions, and credential access.
6. Record the source, version, permissions, and test result.

Do not install every framework at once. Prefer the built-in OpenJarvis implementation unless a measured capability gap justifies an adapter.

### Phase 4 — Durable memory

- Select a datastore with backups and access controls.
- Separate user-approved memory from transient conversation history and execution traces.
- Provide inspect, export, correction, and deletion workflows.
- Store secrets separately from semantic memory.
- Add restart, backup-restore, and data-retention tests.

### Phase 5 — Controlled autonomy

- Use a planner to break a task into explicit steps.
- Give each specialist agent only the tools needed for its role.
- Set execution budgets, timeouts, retry limits, and stop conditions.
- Require human approval for irreversible or externally visible actions.
- Persist task state and an audit trail.
- Verify outcomes rather than trusting an agent's self-report.

## Definition of done for the first user test

The first Android test is successful only if all of the following are true:

- The hosted HTTPS URL loads without certificate warnings.
- The PWA can be installed and launched from the Android home screen.
- An authenticated chat request succeeds.
- An invalid or missing API key is rejected.
- The UI reports backend/network failures instead of hanging silently.
- No API key or provider secret is present in frontend assets or the Git repository.
- The user understands that Render Free may sleep and does not provide durable local storage.

## Out of scope for this phase

This phase does not claim that the assistant can modify its own model, bypass platform permissions, control Android silently, or operate external accounts without authorization. It also does not claim that unrelated projects (including any project named GODMOD3) are integrated until their exact repository and compatibility have been reviewed.

## Next engineering tasks

1. Run the repository's documented test suite and frontend build in CI.
2. Add automated deployment smoke tests for health, auth, and PWA asset delivery.
3. Verify the exact PWA icon assets exist and are included in the production build.
4. Review the frontend API-base and authentication flow for production same-origin deployment.
5. Add an external persistent-memory adapter only after choosing a supported datastore and threat model.
6. Evaluate candidate agent frameworks against concrete tasks and measured outcomes before adding dependencies.
