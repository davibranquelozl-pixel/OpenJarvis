# Deploy on Render

This Blueprint deploys the OpenJarvis API and bundled web/PWA interface using
the Dockerfile in `deploy/docker/Dockerfile`. It runs Google's Gemini
`gemini-2.5-flash` through OpenJarvis's cloud inference engine.

## Create the service

1. Open Render's New Blueprint flow and select your OpenJarvis fork.
2. When prompted, provide `GEMINI_API_KEY` from Google AI Studio.
3. Apply the Blueprint and wait for the `/health` check to pass.
4. Retrieve the generated `OPENJARVIS_API_KEY` from the Render dashboard.
   Keep it private; it authenticates requests to your OpenJarvis server.

## Connect the Android PWA to the server

1. Open the deployed HTTPS URL in Chrome and wait for the OpenJarvis interface to load.
2. Open **Settings** in the app.
3. Leave **API URL** empty because the interface and API are served from the same origin.
4. Paste the generated `OPENJARVIS_API_KEY` into the **API key** field and save the setting.
5. Return to Chat and send a short test message. If the service is healthy but requests return `401`, recheck the API key; do not paste it into a public issue, chat, or source file.

The web app stores this API key in the browser's local settings. Anyone with access to the unlocked browser profile may be able to use it, so protect the device and avoid sharing the browser profile.

The Docker image installs the `inference-google` extra. The API key is
configured as a Render secret and must never be committed to the repository.

## Install the web app on Android

After deployment, open the service's HTTPS URL in Chrome on Android. Use
Chrome's menu and choose **Add to Home screen** or **Install app** when offered.
This installs the web/PWA experience; it is not a native APK.

## Storage and free-instance limits

Render's free web-service filesystem is ephemeral. Data under
`/home/openjarvis/.openjarvis`, including local databases, settings, and
learned state, can be lost when the service restarts, spins down, or redeploys.
Free services may also spin down after inactivity. Therefore this setup is a
low-cost test/development deployment, not durable persistent memory.

For durable state, use a persistent disk on a paid Render service or configure
an external managed datastore supported by the relevant OpenJarvis feature.
Gemini API usage may also be subject to Google's quotas, eligibility rules,
and pricing; free hosting does not guarantee free model inference.
