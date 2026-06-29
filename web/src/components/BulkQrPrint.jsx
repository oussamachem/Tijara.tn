import { useEffect, useMemo, useState } from 'react';
import { createPortal } from 'react-dom';
import { variantsApi } from '../api/endpoints.js';
import { Button, Modal, Spinner, Alert, Field, Input, Select } from './ui.jsx';

const SHOP_NAME = import.meta.env.VITE_SHOP_NAME ?? 'Smart Boutique';
const LARGE_THRESHOLD = 200;       // au-dela : avertissement (garde-fou, non bloquant)
const SETTINGS_KEY = 'qrPrintSettings';

// Defaut : etiquette thermique 50 x 40 mm (rouleau CD410-U), ajustable et memorise.
const DEFAULTS = { mode: 'thermal', w: 50, h: 40 };
function loadSettings() {
  try { return { ...DEFAULTS, ...JSON.parse(localStorage.getItem(SETTINGS_KEY) || '{}') }; }
  catch { return { ...DEFAULTS }; }
}

/**
 * Impression des QR : 1 etiquette par UNITE en stock.
 * - Mode THERMIQUE (defaut) : 1 etiquette = 1 page (@page a la taille du rouleau, saut apres
 *   chaque QR) -> toutes sortent en chaine dans un seul job, etiquette par etiquette.
 * - Mode FEUILLE A4 : grille multi-etiquettes pour imprimante bureautique.
 * Le QR encode toujours la reference variante (scan-vente inchange) ; 1 fetch QR par variante.
 */
export default function BulkQrPrint({ product, onClose }) {
  const [qrByVariant, setQrByVariant] = useState({}); // variantId -> blob URL
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [settings, setSettings] = useState(loadSettings);
  const { mode, w, h } = settings;
  const setSetting = (patch) => setSettings((s) => {
    const n = { ...s, ...patch };
    try { localStorage.setItem(SETTINGS_KEY, JSON.stringify(n)); } catch { /* quota/private */ }
    return n;
  });

  const variants = useMemo(() => (product?.variants ?? []).filter((v) => v.quantity > 0), [product]);
  const total = useMemo(() => variants.reduce((s, v) => s + v.quantity, 0), [variants]);

  useEffect(() => {
    if (!product) return undefined;
    let cancelled = false;
    const urls = [];
    setLoading(true); setError('');
    (async () => {
      try {
        const map = {};
        for (const v of variants) {            // 1 fetch PAR VARIANTE (jamais par unite)
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
    return () => { cancelled = true; urls.forEach((u) => URL.revokeObjectURL(u)); };
  }, [product, variants]);

  if (!product) return null;

  // Liste des etiquettes : chaque variante repetee `quantity` fois (exemplaires identiques).
  const labels = [];
  for (const v of variants) for (let i = 0; i < v.quantity; i += 1) labels.push({ key: `${v.id}-${i}`, v });

  // -------- Mise en page THERMIQUE (adaptee a la taille de l'etiquette) --------
  const showAttrs = h >= 25;          // couleur.taille seulement si la hauteur le permet
  const showName = h >= 35;           // nom produit seulement sur grande etiquette
  const textLines = 1 + (showAttrs ? 1 : 0) + (showName ? 1 : 0);
  const qrMm = Math.max(8, Math.min(w - 4, h - (textLines * 4 + 3)));  // carre centre + quiet zone
  const refPt = Math.max(5, Math.min(10, w / 6));

  const ThermalLabel = ({ v }) => {
    const attrs = [v.colorName, v.size].filter(Boolean).join(' · ');   // champs null omis
    return (
      <div className="qr-thermal-label" style={{ width: `${w}mm`, height: `${h}mm` }}>
        {qrByVariant[v.id]
          ? <img src={qrByVariant[v.id]} alt="" style={{ width: `${qrMm}mm`, height: `${qrMm}mm` }} />
          : <div style={{ width: `${qrMm}mm`, height: `${qrMm}mm` }} />}
        <div className="qr-t-ref" style={{ fontSize: `${refPt}pt` }}>{v.reference}</div>
        {showAttrs && attrs && <div className="qr-t-attrs" style={{ fontSize: `${refPt - 1.5}pt` }}>{attrs}</div>}
        {showName && <div className="qr-t-name" style={{ fontSize: `${refPt - 2}pt` }}>{product.name}</div>}
      </div>
    );
  };
  const thermalLabels = labels.map((l) => <ThermalLabel key={l.key} v={l.v} />);

  // -------- Mise en page FEUILLE A4 (grille) --------
  const SheetLabel = ({ v }) => {
    const attrs = [v.colorName, v.size].filter(Boolean).join(' · ');
    return (
      <div className="qr-label">
        <div className="qr-shop">{SHOP_NAME}</div>
        {qrByVariant[v.id] ? <img src={qrByVariant[v.id]} alt="" className="qr-img" /> : <div className="qr-img" />}
        <div className="qr-ref">{v.reference}</div>
        <div className="qr-name">{product.name}</div>
        {attrs && <div className="qr-attrs">{attrs}</div>}
      </div>
    );
  };
  const sheet = <div className="qr-sheet">{labels.map((l) => <SheetLabel key={l.key} v={l.v} />)}</div>;

  // @page dynamique selon le mode (injecte ; s'applique meme si #root est masque a l'impression).
  const pageStyle = mode === 'thermal'
    ? `@media print { @page { size: ${w}mm ${h}mm; margin: 0; } .qr-thermal-portal { display: block !important; } }`
    : `@media print { @page { size: A4; margin: 10mm; } }`;

  const doPrint = () => window.print();   // images deja chargees (apercu) -> pas de QR blanc

  return (
    <>
      <style>{pageStyle}</style>
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

            <div className="flex flex-wrap items-end gap-3">
              <Field label="Mode d'impression">
                <Select value={mode} onChange={(e) => setSetting({ mode: e.target.value })} className="w-56">
                  <option value="thermal">Thermique — 1 étiquette / page</option>
                  <option value="sheet">Feuille A4 — grille</option>
                </Select>
              </Field>
              {mode === 'thermal' && (
                <>
                  <Field label="Largeur (mm)">
                    <Input type="number" min="20" max="108" value={w}
                      onChange={(e) => setSetting({ w: Number(e.target.value) || DEFAULTS.w })} className="w-24" />
                  </Field>
                  <Field label="Hauteur (mm)">
                    <Input type="number" min="15" max="200" value={h}
                      onChange={(e) => setSetting({ h: Number(e.target.value) || DEFAULTS.h })} className="w-24" />
                  </Field>
                </>
              )}
              <div className="ml-auto text-sm text-slate-600">
                <b>{total}</b> étiquette(s)
                {total > LARGE_THRESHOLD && (
                  <span className="ml-2 font-medium text-amber-600">⚠️ Volume élevé — {total} pages.</span>
                )}
              </div>
            </div>

            <div className="max-h-[55vh] overflow-y-auto rounded-lg border border-slate-200 bg-slate-50 p-3">
              {mode === 'thermal' ? (
                <div className="flex flex-wrap gap-2">
                  {labels.map((l) => (
                    <div key={l.key} className="rounded border border-dashed border-slate-300">
                      <ThermalLabel v={l.v} />
                    </div>
                  ))}
                </div>
              ) : sheet}
            </div>
          </div>
        )}
      </Modal>

      {/* Portails d'impression hors #root : reveles uniquement a l'impression, selon le mode. */}
      {!loading && total > 0 && mode === 'thermal'
        && createPortal(<div className="qr-thermal-portal">{thermalLabels}</div>, document.body)}
      {!loading && total > 0 && mode === 'sheet'
        && createPortal(<div className="qr-print-portal">{sheet}</div>, document.body)}
    </>
  );
}
