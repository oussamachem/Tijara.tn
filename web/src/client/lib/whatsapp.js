// Utilitaires lien WhatsApp (wa.me). Aucune API : juste un lien https://wa.me/<num>?text=<message>.

/** Message par défaut si la boutique n'en a pas défini un. */
export const DEFAULT_WA_MESSAGE = 'Bonjour, je suis intéressé(e) par ce produit.';

/** URL PUBLIQUE réelle de la fiche produit (route client /s/:slug/p/:productId), sur le domaine courant. */
export function productUrl(slug, productId) {
  return `${window.location.origin}/s/${slug}/p/${productId}`;
}

/** URL de PARTAGE social (page Open Graph rendue par le serveur) : /s/:slug/produit/:productId. */
export function productShareUrl(slug, productId) {
  return `${window.location.origin}/s/${slug}/produit/${productId}`;
}

/**
 * Normalise un numéro pour wa.me : chiffres uniquement, indicatif pays inclus (8..15 chiffres, E.164).
 * Retourne null si invalide -> le bouton ne s'affiche pas (jamais de lien cassé).
 */
export function waDigits(phone) {
  if (!phone) return null;
  let d = String(phone).replace(/[^0-9]/g, '');
  if (d.startsWith('00')) d = d.slice(2); // préfixe d'appel international
  return d.length >= 8 && d.length <= 15 ? d : null;
}

/** Lien wa.me complet (message encodé via encodeURIComponent), ou null si numéro invalide. */
export function waLink(phone, message) {
  const d = waDigits(phone);
  if (!d) return null;
  return `https://wa.me/${d}?text=${encodeURIComponent(message || '')}`;
}
