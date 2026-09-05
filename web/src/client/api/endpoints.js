// Shim : les pages CLIENT importent `shopsApi` (annuaire/catalogue/galerie/commande marketplace)
// et `authApi`, plus le self-service boutique, les notifications et le profil. On les mappe sur
// les endpoints unifiés (marketplace = tenant par slug, sans X-Shop-Id).
export {
  authApi,
  marketplaceApi as shopsApi,
  myShopApi,
  notificationsApi,
  favoritesApi,
  profileApi,
} from '../../api/endpoints.js';
