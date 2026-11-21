# Android Companion Roadmap — Jarvis Mobile

Status: design proposal; not yet implemented in the Android app.

This roadmap translates the mobile Jarvis requirements into incremental, testable work. It is additive and does not claim that a PWA can perform native Android operations.

## Product goal

Deliver a polished Android companion for the existing OpenJarvis core:

- Fast, conversational chat with streaming responses.
- Voice input and spoken replies with visible microphone/speaker state.
- Persistent conversation history and user-controlled memory.
- A mobile-first navigation system for Chat, Agents, Skills, Activity, and Settings.
- Optional quick access from a notification or a user-enabled floating launcher.
- Web research and approved tools through the existing backend.
- Explicit model selection and future provider routing (OpenAI, Anthropic/Claude, local engines where supported).

The Jarvis identity/personality belongs in the assistant configuration and orchestration layer, not in a hard-coded UI-only prompt.

## Verified repository facts

The current frontend is React + Vite. The package manifest includes Tauri 2 APIs and plugins (notification, global shortcut, shell, updater, dialog, autostart), plus `vite-plugin-pwa`. The application routes include Chat, Dashboard, Settings, Get Started, Data Sources, Agents, and Logs.

The root README documents OpenJarvis agents and the Skills install/sync workflow. The existing upgrade plan also records the Render Free ephemeral-filesystem limitation.

These facts do not establish that the current hosted PWA is already a native Android app, that the desktop overlay works on Android, or that a floating overlay has been implemented.

## Recommended implementation strategy

### Stage A — Mobile web experience (lowest risk)

Keep the current React/Vite frontend and validate it as an installable PWA before adding native complexity.

Acceptance criteria:

- Responsive layout at 360 px, 390 px, and 430 px viewport widths.
- Composer remains reachable above the mobile keyboard and respects safe-area insets.
- Streaming output does not cause horizontal overflow or jump the user away from a message they are reading.
- Chat actions have large touch targets and clear pressed/disabled/loading states.
- Sidebar becomes a mobile drawer with clear close/back behavior.
- Conversation list supports search, rename, and delete with confirmation.
- Network, authentication, rate-limit, and provider errors are visible and recoverable.
- PWA install/update behavior and offline shell are tested on a physical Android device.

Do not represent offline shell caching as offline AI: backend-dependent requests still require connectivity.

### Stage B — Native Android packaging decision

Evaluate Tauri 2 Android support against the current app's plugins and backend assumptions. Build a minimal Android proof of concept on a separate branch before changing the production deployment.

Compare:
1. Tauri 2 Android target with narrowly scoped Kotlin plugins.
2. A dedicated Kotlin/Jetpack Compose shell that hosts the existing web frontend.
3. Keeping the PWA as the first release and delaying native packaging.

Choose based on a successful Android build, plugin compatibility, maintenance cost, accessibility, and reliable permission handling—not on appearance alone.

### Stage C — Voice interaction

Implement push-to-talk first:

1. User taps the microphone.
2. Android permission is requested only when needed.
3. A visible recording indicator and elapsed time appear.
4. User can cancel before sending.
5. Speech is transcribed and shown for review or sent according to a user setting.
6. Assistant response can be read aloud with pause/stop controls.

Do not implement hidden, continuous, or covert microphone capture. Wake-word listening, if ever considered, requires a separate privacy and battery review and must comply with Android foreground-service rules.

### Stage D — Quick launcher / floating assistant

Preferred progression:
- First: persistent notification action or Android app shortcut that opens the chat.
- Later: optional native floating bubble, enabled only after an explicit in-app explanation and the user granting Android's overlay permission in system settings.

Overlay requirements:

- A visible close and pause control.
- No automatic typing, tapping, or reading other apps' screens.
- No overlay on sensitive system screens or payment/authentication flows.
- The bubble opens a compact assistant panel; it does not imply unrestricted control of the phone.
- Permission can be revoked from the app settings.
- The feature degrades gracefully when permission is unavailable.

Android platform constraints to design around:

- `SYSTEM_ALERT_WINDOW` is a special user-granted overlay permission.
- Android 15 narrows the background foreground-service exemption: when relying on overlay permission, a visible overlay must already exist before starting the foreground service.
- Microphone foreground services require the correct service type and permissions; microphone access is subject to while-in-use restrictions and generally cannot be started from the background.
- A PWA cannot independently create a system-wide Android overlay or bypass these restrictions.

Official references:
- https://developer.android.com/about/versions/15/behavior-changes-15
- https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start
- https://developer.android.com/about/versions/14/changes/fgs-types-required

### Stage E — Assistant capabilities

Expose existing capabilities through a consistent mobile interface:

| Area | Mobile experience | Safety requirement |
|---|---|---|
| Chat | Streaming, markdown, code, citations | Show source links and failure states |
| Research | Task progress, sources, summary | Distinguish evidence from inference |
| Agents | Role, status, run history | Narrow tool permissions |
| Skills | Search, inspect, enable/disable | Show source, version, license, permissions |
| Memory | View, correct, export, delete | User-controlled and separate from secrets |
| Automations | Schedules and run history | Confirm external or irreversible actions |
| Files | Upload, preview, export | Explicit file permissions and retention |
| Voice | Push-to-talk and TTS | Visible capture state and stop control |
| Models | Provider/model selector | Never expose API keys in client UI/logs |

## Skills and plugin governance

Do not bulk-install community skills. Treat every skill as untrusted code/instructions until reviewed.

Required metadata for each candidate:

- Name, source repository, author/maintainer, exact commit or release.
- License and last meaningful maintenance date.
- Required dependencies and install scripts.
- Tools and permissions requested.
- Network destinations and filesystem access.
- Secrets or environment variables it expects.
- Test results in a restricted environment.
- Rollback/uninstall procedure.

Initial evaluation order:
1. Skills already supported by OpenJarvis and its documented Agent Skills-compatible workflow.
2. Small, narrowly scoped research/productivity skills from maintained sources.
3. Developer tools only inside a restricted workspace.
4. High-autonomy bundles (including projects marketed as "God Mode") only after a manual source and permission audit.

A large token count or a huge skill file is not a quality metric. Prefer short, composable skills with explicit tool contracts and tests. Never let skill instructions override system security policy or request secrets through conversation.

## Model-provider routing

Keep provider integrations behind one server-side adapter interface. The mobile client should select a provider/model by ID; it must never contain provider API keys.

Future router policy:
- Fast/low-cost model for simple chat and classification.
- Stronger model for complex reasoning, coding, or long research.
- Provider fallback only when configured and permitted.
- Per-task cost, latency, error, and token metrics.
- User-visible model choice and a way to disable automatic routing.

Claude/Anthropic is a future provider integration, not an assumed active connection. It needs an Anthropic API credential stored server-side or another explicitly configured supported authentication path. A ChatGPT subscription does not automatically provide API credits for OpenAI, and a Claude subscription does not automatically provide API credits for Anthropic.

## Security and reliability baseline

- HTTPS only for hosted traffic.
- Server-side authentication and authorization for every private API.
- Exact CORS allowlist; no wildcard credentialed CORS.
- Rate limits and request size limits.
- Secrets only in host secret storage; redact them from logs and traces.
- Skills and agents run with least privilege and time/resource budgets.
- Confirm before sending messages, publishing content, purchasing, deleting, changing account settings, or triggering other externally visible/irreversible actions.
- Keep an auditable task history with tool inputs redacted where sensitive.
- Back up durable memory; never rely on Render Free local disk for persistent user data.
- Provide account/session revocation and memory export/deletion paths.

## Milestones and exit criteria

### M1 — Mobile PWA quality
- Physical-device test on Android.
- Responsive chat, keyboard/safe-area handling, navigation drawer.
- Streaming, error recovery, install/update, accessibility checks.
- Frontend build and tests pass.

### M2 — Android native proof of concept
- Build/install on a physical device.
- Confirm authentication, chat, notifications, and deep links.
- Document plugin gaps and battery impact.
- No overlay or microphone background service in this milestone.

### M3 — Voice
- Push-to-talk, transcript review, TTS playback controls.
- Permission denial and revocation tests.
- No hidden recording.

### M4 — Quick launcher
- Notification shortcut first.
- Optional overlay only after native proof, threat review, and user-granted permission.
- Visible pause/close and clear permission explanation.

### M5 — Multi-model and governed skills
- Provider adapter tests.
- Skill catalog with provenance and permission metadata.
- Sandboxed execution and rollback.
- Usage/cost visibility.

A milestone is complete only after a reproducible build/test result and a physical-device check where relevant. A commit or a successful web deployment alone is not proof that Android behavior works.

## User decisions to collect later

No decision is required to start the PWA audit. Before native implementation, confirm:

- Whether the floating bubble should be optional (recommended) or absent.
- Whether voice should default to push-to-talk (recommended).
- Whether automatic model routing is opt-in (recommended).
- Which external providers the user is willing to configure API credentials for.

