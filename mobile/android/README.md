# OpenJarvis Android — native client foundation

This module is a native Android client, separate from the existing desktop Tauri app and the installable web PWA.

## Included in this first implementation

- Native Android app entry point and launcher.
- Explicit microphone permission request and Android speech-recognition handoff (Portuguese Brazil).
- Text and speech input.
- HTTPS requests to the OpenAI-compatible OpenJarvis endpoint.
- Bearer authentication using the server API key.
- Spoken responses through Android TextToSpeech.
- API endpoint preference saved locally; the API key is held in memory only and must be re-entered after restarting the app.

## Build

Open `mobile/android` in Android Studio (JDK 17, Android SDK 35) and run the `app` configuration on an Android 8.0+ device. The user must configure an HTTPS OpenJarvis server and a valid API key.

## Deliberate limitations of this stage

- This is a push-to-talk foreground client. It does **not** listen continuously, implement a wake word, or record in the background.
- Android's speech-recognition provider may process audio according to the provider's own privacy terms.
- The first version uses the OpenAI-compatible `/v1/chat/completions` endpoint with a starter model ID. Model selection and streaming are follow-up work.
- API key is not persisted in plaintext. A production release should add Android Keystore-backed secret storage and an explicit lock/biometric option.
- No Google, Gmail, Instagram, TikTok, or other account is connected yet. Each integration needs its official OAuth flow, least-privilege scopes, token storage, and provider review/availability. Never ask users to paste account passwords or session cookies.
- Background wake-word detection requires a separately engineered, user-enabled foreground microphone service, persistent notification, battery-impact controls, and Android-version/device testing. It cannot be guaranteed by a PWA or ordinary background service.

## Next milestones

1. Add an Android Keystore token vault and session lock.
2. Add streaming chat and full-duplex voice transport (WebRTC or a supported realtime API adapter).
3. Add an opt-in foreground voice session with clear microphone indicator and stop control; evaluate on-device wake-word detection.
4. Add OAuth integrations one provider at a time, starting with Google APIs and the narrowest useful scopes.
5. Add audited tool execution, per-action confirmation, and revocation controls.
6. Build and test signed APK/AAB in CI before distribution.
