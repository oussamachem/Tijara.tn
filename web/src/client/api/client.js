// Shim : le module CLIENT (pages marketplace portées de web-client) réutilise le client HTTP
// unifié (interceptors JWT + X-Shop-Id + gestion 401) sans dupliquer la configuration.
export { apiError, default } from '../../api/client.js';
