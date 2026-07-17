// Shim : les pages CLIENT importent `shopsApi` (annuaire/catalogue/galerie/commande marketplace)
// et `authApi`. On les mappe sur les endpoints unifiés (marketplace = tenant par slug, sans X-Shop-Id).
export { authApi, marketplaceApi as shopsApi } from '../../api/endpoints.js';
