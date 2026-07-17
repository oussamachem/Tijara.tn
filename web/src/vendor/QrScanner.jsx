import { useEffect, useRef, useState } from 'react';
import { BrowserMultiFormatReader } from '@zxing/browser';

/**
 * Scanner QR via la caméra du navigateur (getUserMedia + @zxing/browser). Décodage en continu ;
 * `onDetected(text)` est appelé UNE fois au premier code lu, puis le flux est arrêté.
 *
 * Contrainte navigateur : getUserMedia exige un contexte SÉCURISÉ (https:// ou localhost). En LAN
 * http direct, la caméra est bloquée — on l'indique et on propose la saisie manuelle en repli.
 */
export default function QrScanner({ open, onClose, onDetected, title = 'Scanner un QR code' }) {
  const videoRef = useRef(null);
  const controlsRef = useRef(null);
  const firedRef = useRef(false);
  const [error, setError] = useState('');
  const [manual, setManual] = useState('');

  useEffect(() => {
    if (!open) return undefined;
    firedRef.current = false;
    setError('');

    if (!window.isSecureContext) {
      setError("La caméra nécessite une connexion sécurisée (https ou localhost). Utilisez la saisie manuelle, ou accédez à l'app en HTTPS (tunnel).");
      return undefined;
    }

    const reader = new BrowserMultiFormatReader();
    let cancelled = false;

    reader
      .decodeFromConstraints({ video: { facingMode: 'environment' } }, videoRef.current, (result, _err, controls) => {
        if (!controlsRef.current) controlsRef.current = controls;
        if (result && !firedRef.current && !cancelled) {
          firedRef.current = true;
          controls.stop();
          onDetected(result.getText());
        }
      })
      .catch((e) => {
        const name = e?.name || '';
        if (name === 'NotAllowedError') setError("Accès caméra refusé. Autorisez la caméra dans le navigateur, ou saisissez le code manuellement.");
        else if (name === 'NotFoundError') setError("Aucune caméra détectée. Saisissez le code manuellement.");
        else setError("Caméra indisponible. Saisissez le code manuellement.");
      });

    return () => {
      cancelled = true;
      try { controlsRef.current?.stop(); } catch { /* noop */ }
      controlsRef.current = null;
    };
  }, [open, onDetected]);

  if (!open) return null;

  const submitManual = (e) => {
    e.preventDefault();
    const code = manual.trim();
    if (code) { setManual(''); onDetected(code); }
  };

  return (
    <div className="fixed inset-0 z-50 flex flex-col bg-black/90">
      <div className="flex items-center justify-between px-4 py-3 text-white">
        <span className="font-semibold">{title}</span>
        <button onClick={onClose} className="rounded-lg px-3 py-1.5 text-sm font-medium text-white/80 hover:bg-white/10">Fermer ✕</button>
      </div>

      <div className="relative flex flex-1 items-center justify-center overflow-hidden">
        {/* eslint-disable-next-line jsx-a11y/media-has-caption */}
        <video ref={videoRef} className="h-full w-full object-cover" muted playsInline />
        {!error && (
          <div className="pointer-events-none absolute inset-0 flex flex-col items-center justify-center">
            <div className="h-56 w-56 rounded-2xl border-4 border-white/90" />
            <p className="mt-4 text-sm font-medium text-white">Visez le QR code du produit</p>
          </div>
        )}
        {error && (
          <div className="absolute inset-x-4 top-6 rounded-xl bg-rose-600/95 p-4 text-sm text-white">{error}</div>
        )}
      </div>

      <form onSubmit={submitManual} className="safe-bottom bg-slate-900 p-4">
        <p className="mb-2 text-xs font-medium text-slate-300">Saisie manuelle (référence de la variante)</p>
        <div className="flex gap-2">
          <input
            value={manual}
            onChange={(e) => setManual(e.target.value)}
            placeholder="Ex. TSHIRT-BLEU-M"
            className="min-w-0 flex-1 rounded-xl border border-slate-700 bg-slate-800 px-4 py-3 text-sm text-white outline-none placeholder:text-slate-500"
          />
          <button type="submit" className="shrink-0 rounded-xl bg-brand-600 px-4 py-3 text-sm font-semibold text-white hover:bg-brand-700">
            Valider
          </button>
        </div>
      </form>
    </div>
  );
}
