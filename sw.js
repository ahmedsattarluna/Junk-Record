const CACHE = "junklog-v2";
const SETTINGS = "junklog-settings";
const SHELL = ["./", "./index.html", "./manifest.webmanifest",
  "./icon.png", "./icon-512.png"];

self.addEventListener("install", (e) => {
  e.waitUntil(caches.open(CACHE).then((c) => c.addAll(SHELL)).then(() => self.skipWaiting()));
});

self.addEventListener("activate", (e) => {
  e.waitUntil(
    caches.keys()
      .then((keys) => Promise.all(keys.filter((k) => k !== CACHE && k !== SETTINGS).map((k) => caches.delete(k))))
      .then(() => self.clients.claim())
  );
});

self.addEventListener("fetch", (e) => {
  if (e.request.method !== "GET") return;
  e.respondWith(
    caches.match(e.request).then((hit) => {
      if (hit) return hit;
      return fetch(e.request)
        .then((res) => {
          const copy = res.clone();
          caches.open(CACHE).then((c) => c.put(e.request, copy)).catch(() => {});
          return res;
        })
        .catch(() => caches.match("./index.html"));
    })
  );
});

/* ── daily reminder ─────────────────────────────────────────────
   The page writes { remind, remindTime } into the settings cache.
   When the browser wakes this worker, we check whether the chosen
   time has passed today and whether we already fired.               */

function localISO(d) {
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
}

async function readSettings() {
  try {
    const c = await caches.open(SETTINGS);
    const res = await c.match("/settings.json");
    return res ? await res.json() : null;
  } catch (e) { return null; }
}

async function markFired(stamp) {
  try {
    const c = await caches.open(SETTINGS);
    await c.put("/fired.json", new Response(JSON.stringify({ stamp }),
      { headers: { "content-type": "application/json" } }));
  } catch (e) {}
}

async function alreadyFired(stamp) {
  try {
    const c = await caches.open(SETTINGS);
    const res = await c.match("/fired.json");
    if (!res) return false;
    const j = await res.json();
    return j.stamp === stamp;
  } catch (e) { return false; }
}

async function maybeRemind() {
  const s = await readSettings();
  if (!s || !s.remind || !s.remindTime) return;

  const now = new Date();
  const [h, m] = s.remindTime.split(":").map(Number);
  const target = new Date();
  target.setHours(h, m, 0, 0);
  if (now < target) return;

  const stamp = localISO(now);
  if (await alreadyFired(stamp)) return;
  await markFired(stamp);

  await self.registration.showNotification("Junk Log", {
    body: "Anything to log for today?",
    icon: "icon.png",
    badge: "icon.png",
    tag: "junk-daily",
    renotify: true,
    data: { url: "./" },
  });
}

self.addEventListener("periodicsync", (e) => {
  if (e.tag === "junk-reminder") e.waitUntil(maybeRemind());
});

self.addEventListener("sync", (e) => {
  if (e.tag === "junk-reminder") e.waitUntil(maybeRemind());
});

self.addEventListener("notificationclick", (e) => {
  e.notification.close();
  e.waitUntil(
    self.clients.matchAll({ type: "window", includeUncontrolled: true }).then((list) => {
      for (const c of list) if ("focus" in c) return c.focus();
      if (self.clients.openWindow) return self.clients.openWindow("./");
    })
  );
});
