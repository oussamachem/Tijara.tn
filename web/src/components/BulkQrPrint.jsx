import { useEffect, useMemo, useState } from 'react';
import { createPortal } from 'react-dom';
import { variantsApi } from '../api/endpoints.js';
import { Button, Modal, Spinner, Alert } from './ui.jsx';

const SHOP_NAME = import.meta.env.VITE_SHOP_NAME ?? 'Smart Boutique';
const LARGE_THRESHOLD = 200;     // au-dela : on previent l'admin (garde-fou, non bloquant)
const LABELS_PER_PAGE = 24;      // ~4 colonnes x 6 lignes (estimation pages)

/**
 * Impression en masse : UNE etiquette par UNITE en stock, pour un produit.
 * Pour chaque variante (stock > 0) on recupere son QR PNG UNE SEULE FOIS, puis on
 * repete l'image `quantity` fois. Le QR encode la reference variante (inchange) :
 * le scan de vente continue de fonctionner.
 */
export default function BulkQrPrint({ product, onClose }) {
  const [qrByVariant, setQrByVariant] = useState({}); // variantId -> blob URL
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  // On n'imprime que les variantes ayant du stock.
  const variants = useMemo(
    () => (product?.variants ?? []).filter((v) => v.quantity > 0),
    [product],
  );
  const total = useMemo(() => variants.reduce((s, v) => s + v.quantity, 0), [variants]);

  useEffect(() => {
    if (!product) return undefined;
    let cancelled = false;
    const urls = [];
    setLoading(true);
    setError('');
    (async () => {
      try {
        const map = {};
        // 1 fetch PAR VARIANTE (jamais par unite) — efficacite §0.5.
        for (const v of variants) {
          const { data } = await variantsApi.qrCode(v.id);
          const url = URL.createObjectURL(data);
          urls.push(url);
          map[v.id] = url;
        }
        if (!cancelled) setQrByVariant(map);
      } catch {
        if (!cancelled) setError('Erreur lors du chargement des QR Codes.');
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    // Nettoyage des blobs (pas de fuite memoire).
    return () => {
      cancelled = true;
      urls.forEach((u) => URL.revokeObjectURL(u));
    };
  }, [product, variants]);

  if (!product) return null;

  // Liste des etiquettes : chaque variante repetee `quantity` fois (exemplaires identiques).
  const labels = [];
  for (const v of variants) {
    for (let i = 0; i < v.quantity; i += 1) {
      labels.push({ key: `${v.id}-${i}`, v });
    }
  }

  const Label = ({ v }) => {
    const attrs = [v.colorName, v.size].filter(Boolean).join(' · '); // champs null omis proprement
    return (
      <div className="qr-label">
        <div className="qr-shop">{SHOP_NAME}</div>
        {qrByVariant[v.id]
          ? <img src={qrByVariant[v.id]} alt="" className="qr-img" />
          : <div className="qr-img" />}
        <div className="qr-ref">{v.reference}</div>
        <div className="qr-name">{product.name}</div>
        {attrs && <div className="qr-attrs">{attrs}</div>}
      </div>
    );
  };

  const sheet = (
    <div className="qr-sheet">
      {labels.map((l) => <Label key={l.key} v={l.v} />)}
    </div>
  );

  // Les images de l'apercu sont deja chargees (fetch termine) -> pas de QR blanc a l'impression.
  const doPrint = () => window.print();

  return (
    <>
      <Modal
        open
        onClose={onClose}
        title={`Imprimer les QR — ${product.reference}`}
        wide
        footer={(
          <>
            <Button variant="secondary" onClick={onClose}>Fermer</Button>
            <Button onClick={doPrint} disabled={loading || total === 0}>🖨️ Imprimer ({total})</Button>
          </>
        )}
      >
        {loading ? (
          <div className="flex flex-col items-center gap-2 py-8">
            <Spinner className="h-7 w-7" />
            <span className="text-sm text-slate-500">Chargement des QR Codes…</span>
          </div>
        ) : total === 0 ? (
          <p className="py-6 text-center text-sm text-slate-400">Aucune unité en stock à imprimer.</p>
        ) : (
          <div className="space-y-3">
            <Alert type="error">{error}</Alert>
            <div className="text-sm text-slate-600">
              <b>{total}</b> étiquette(s) — 1 par unité en stock, sur {variants.length} variante(s).
              {total > LARGE_THRESHOLD && (
                <span className="ml-2 font-medium text-amber-600">
                  ⚠️ Volume élevé (~{Math.ceil(total / LABELS_PER_PAGE)} pages) — vérifiez avant d'imprimer.
                </span>
              )}
            </div>
            <div className="max-h-[55vh] overflow-y-auto rounded-lg border border-slate-200 bg-slate-50 p-3">
              {sheet}
            </div>
          </div>
        )}
      </Modal>

      {/* Portail d'impression hors #root : revele uniquement par @media print. */}
      {!loading && total > 0 && createPortal(
        <div className="qr-print-portal">{sheet}</div>,
        document.body,
      )}
    </>
  );
}
