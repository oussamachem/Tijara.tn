// Service worker minimal (PWA installable) — SANS cache applicatif. On sert toujours depuis le
// réseau (pas de contenu périmé) : l'app est en ligne (API multi-tenant), un shell hors-ligne
// n'apporte rien et peut masquer les mises à jour. À l'activation, on PURGE tout ancien cache
// (versions précédentes qui gardaient un vieux bundle).
self.addEventListener('install', () => self.skipWaiting());

self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches.keys()
      .then((keys) => Promise.all(keys.map((k) => caches.delete(k))))
      .then(() => self.clients.claim())
  );
});

// Pas de handler 'fetch' qui met en cache : le navigateur va directement au réseau.
