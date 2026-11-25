import test, { before, after } from "node:test";
import assert from "node:assert/strict";
import { spawn } from "node:child_process";
import net from "node:net";

let child;
let base;

async function freePort() {
  const server = net.createServer();
  await new Promise((resolve, reject) => server.listen(0, "127.0.0.1", resolve).once("error", reject));
  const port = server.address().port;
  await new Promise(resolve => server.close(resolve));
  return port;
}

before(async () => {
  const port = await freePort();
  base = `http://127.0.0.1:${port}`;
  child = spawn(process.execPath, ["server.mjs"], {
    cwd: new URL("..", import.meta.url),
    env: { ...process.env, PORT: String(port), AI_API_KEY: "" },
    stdio: "ignore"
  });
  let ready = false;
  for (let i = 0; i < 50; i++) {
    try {
      const response = await fetch(base + "/api/health");
      if (response.ok) { ready = true; break; }
    } catch {}
    await new Promise(resolve => setTimeout(resolve, 100));
  }
  assert.equal(ready, true, "server should start for smoke tests");
});

after(() => { child?.kill("SIGTERM"); });

test("health endpoint reports service status without exposing secrets", async () => {
  const response = await fetch(base + "/api/health");
  const data = await response.json();
  assert.equal(response.status, 200);
  assert.equal(data.ok, true);
  assert.equal(data.aiConfigured, false);
  assert.equal("apiKey" in data, false);
});

test("selected text can be imported and retrieved", async () => {
  const imported = await fetch(base + "/api/import", {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify({ text: "Resolva 2 + 2." })
  });
  assert.equal(imported.status, 200);
  const current = await fetch(base + "/api/imported");
  assert.equal((await current.json()).task, "Resolva 2 + 2.");
});

test("empty selected text is rejected", async () => {
  const response = await fetch(base + "/api/import", {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify({ text: "  " })
  });
  assert.equal(response.status, 400);
});

test("untrusted browser origins are rejected", async () => {
  const response = await fetch(base + "/api/import", {
    method: "POST",
    headers: { origin: "https://malicious.example", "content-type": "application/json" },
    body: JSON.stringify({ text: "unexpected" })
  });
  assert.equal(response.status, 403);
});

test("AI route clearly reports missing local configuration", async () => {
  const response = await fetch(base + "/api/assist", {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify({ task: "Explique fotossíntese", mode: "explain" })
  });
  assert.equal(response.status, 503);
  assert.match((await response.json()).error, /AI_API_KEY/);
});

test("invalid image formats are rejected before provider calls", async () => {
  const response = await fetch(base + "/api/assist", {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify({ image: "data:image/gif;base64,R0lGODlh" })
  });
  assert.equal(response.status, 400);
});
