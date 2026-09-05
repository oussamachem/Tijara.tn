import { useState } from 'react';
import { productShareUrl } from '../lib/whatsapp';

/**
 * Partage d'un produit : partage natif (navigator.share sur mobile) ou copie du lien, + boutons
 * directs Facebook & WhatsApp. Le lien pointe sur la page Open Graph serveur (/s/:slug/produit/:id)
 * -> bel aperçu (carte) sur les réseaux. Aucune donnée sensible : juste le lien public.
 */
export default function ShareBar({ slug, productId, name, className = '' }) {
  const [copied, setCopied] = useState(false);
  const url = productShareUrl(slug, productId);
  const title = name || 'Produit';

  const share = async () => {
    if (navigator.share) {
      try { await navigator.share({ title, text: title, url }); return; } catch { /* partage annulé */ }
    }
    try { await navigator.clipboard.writeText(url); setCopied(true); setTimeout(() => setCopied(false), 1800); }
    catch { window.prompt('Copiez le lien du produit :', url); }
  };

  const fbUrl = `https://www.facebook.com/sharer/sharer.php?u=${encodeURIComponent(url)}`;
  const waUrl = `https://wa.me/?text=${encodeURIComponent(`${title} — ${url}`)}`;

  return (
    <div className={`flex items-center gap-2 ${className}`}>
      <button onClick={share} aria-label="Partager le produit"
        className="flex min-h-[44px] flex-1 items-center justify-center gap-2 rounded-xl border border-slate-200 bg-white px-4 py-3 text-sm font-semibold text-slate-700 transition active:scale-[.98] hover:bg-slate-50">
        <ShareIcon /> {copied ? 'Lien copié ✓' : 'Partager'}
      </button>
      <a href={fbUrl} target="_blank" rel="noopener noreferrer" aria-label="Partager sur Facebook"
        className="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-[#1877F2] text-white transition active:scale-95 hover:brightness-95">
        <FbIcon />
      </a>
      <a href={waUrl} target="_blank" rel="noopener noreferrer" aria-label="Partager sur WhatsApp"
        className="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-[#25D366] text-white transition active:scale-95 hover:brightness-95">
        <WaIcon />
      </a>
    </div>
  );
}

function ShareIcon() {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="shrink-0">
      <circle cx="18" cy="5" r="3" /><circle cx="6" cy="12" r="3" /><circle cx="18" cy="19" r="3" />
      <path d="M8.6 13.5 15.4 17.5M15.4 6.5 8.6 10.5" />
    </svg>
  );
}
function FbIcon() {
  return (
    <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true" className="shrink-0">
      <path d="M14 9h3V6h-3c-1.9 0-3 1.4-3 3.2V11H8v3h3v6h3v-6h2.5l.5-3H14V9.6c0-.4.3-.6.6-.6H14Z" />
    </svg>
  );
}
function WaIcon() {
  return (
    <svg width="20" height="20" viewBox="0 0 32 32" fill="currentColor" aria-hidden="true" className="shrink-0">
      <path d="M16 3C9.4 3 4 8.4 4 15c0 2.1.6 4.1 1.6 5.9L4 29l8.3-1.6c1.7.9 3.6 1.4 5.7 1.4 6.6 0 12-5.4 12-12S22.6 3 16 3zm0 21.8c-1.8 0-3.5-.5-5-1.4l-.4-.2-3.7.7.7-3.6-.2-.4c-1-1.6-1.5-3.4-1.5-5.3 0-5.4 4.4-9.8 9.8-9.8s9.8 4.4 9.8 9.8-4.4 9.8-9.8 9.8zm5.4-7.3c-.3-.1-1.8-.9-2-1s-.5-.1-.7.1c-.2.3-.8 1-.9 1.1-.2.2-.3.2-.6.1-.3-.1-1.3-.5-2.4-1.5-.9-.8-1.5-1.8-1.7-2.1-.2-.3 0-.5.1-.6.1-.1.3-.3.4-.5.1-.2.2-.3.3-.5.1-.2 0-.4 0-.5-.1-.1-.7-1.6-.9-2.2-.2-.6-.5-.5-.7-.5h-.6c-.2 0-.5.1-.8.4-.3.3-1 1-1 2.5s1.1 2.9 1.2 3.1c.1.2 2.1 3.2 5 4.5.7.3 1.2.5 1.7.6.7.2 1.3.2 1.8.1.6-.1 1.8-.7 2-1.4.2-.7.2-1.3.2-1.4-.1-.2-.3-.2-.6-.3z" />
    </svg>
  );
}
