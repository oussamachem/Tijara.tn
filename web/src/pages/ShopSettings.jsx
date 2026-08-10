import { useEffect, useRef, useState } from 'react';
import { shopSettingsApi } from '../api/endpoints.js';
import { apiError } from '../api/client.js';
import { Card, Button, Alert, Spinner, Field, Input } from '../components/ui.jsx';

const imgUrl = (u) => (u ? `${import.meta.env.VITE_API_URL || ''}${u}` : null);

/** « Ma boutique » (propriétaire) : logo / photo de profil + lien partageable (bio Insta/FB/TikTok). */
export default function ShopSettings() {
  const [shop, setShop] = useState(null);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [copied, setCopied] = useState(false);
  const fileRef = useRef(null);

  // Réglages Goodex (transporteur)
  const [goodex, setGoodex] = useState(null);
  const [gForm, setGForm] = useState({ token: '', userId: '', baseUrl: '' });
  const [gBusy, setGBusy] = useState(false);
  const [gMsg, setGMsg] = useState('');

  const load = () => {
    shopSettingsApi.get()
      .then(({ data }) => setShop(data))
      .catch((e) => setError(apiError(e)))
      .finally(() => setLoading(false));
  };
  useEffect(load, []);

  useEffect(() => {
    shopSettingsApi.getGoodex()
      .then(({ data }) => { setGoodex(data); setGForm((f) => ({ ...f, userId: data.userId || '', baseUrl: data.baseUrl || '' })); })
      .catch(() => {});
  }, []);

  const saveGoodex = async (e) => {
    e.preventDefault();
    setGBusy(true); setGMsg(''); setError('');
    try {
      const { data } = await shopSettingsApi.updateGoodex({
        token: gForm.token.trim(), userId: gForm.userId.trim(), baseUrl: gForm.baseUrl.trim(),
      });
      setGoodex(data);
      setGForm((f) => ({ ...f, token: '' }));   // ne pas garder le token en clair dans le formulaire
      setGMsg('Réglages Goodex enregistrés.');
    } catch (err) { setError(apiError(err)); } finally { setGBusy(false); }
  };

  const onPick = async (e) => {
    const file = e.target.files?.[0];
    if (!file) return;
    setBusy(true); setError('');
    try {
      const { data } = await shopSettingsApi.uploadLogo(file);
      setShop(data);
    } catch (err) { setError(apiError(err)); }
    finally { setBusy(false); if (fileRef.current) fileRef.current.value = ''; }
  };

  const removeLogo = async () => {
    setBusy(true); setError('');
    try { const { data } = await shopSettingsApi.removeLogo(); setShop(data); }
    catch (err) { setError(apiError(err)); }
    finally { setBusy(false); }
  };

  if (loading) return <div className="flex justify-center py-20"><Spinner className="h-8 w-8" /></div>;
  if (!shop) return <Alert type="error">{error || 'Boutique introuvable'}</Alert>;

  const link = `${window.location.origin}/s/${shop.slug}`;
  const copy = async () => {
    try { await navigator.clipboard.writeText(link); setCopied(true); setTimeout(() => setCopied(false), 1800); }
    catch { /* clipboard indispo */ }
  };
  const share = async () => {
    if (navigator.share) { try { await navigator.share({ title: shop.name, url: link }); } catch { /* annulé */ } }
    else copy();
  };

  return (
    <div className="mx-auto max-w-2xl space-y-6">
      <h2 className="text-xl font-bold text-slate-800">Ma boutique</h2>
      <Alert type="error" onClose={() => setError('')}>{error}</Alert>

      {/* Logo / photo de profil */}
      <Card title="Photo de profil (logo)">
        <div className="flex items-center gap-5">
          <div className="flex h-24 w-24 shrink-0 items-center justify-center overflow-hidden rounded-2xl bg-brand-100 text-3xl font-black text-brand-700">
            {shop.logoUrl
              ? <img src={imgUrl(shop.logoUrl)} alt="logo" className="h-full w-full object-cover" />
              : (shop.name || '?').charAt(0).toUpperCase()}
          </div>
          <div className="space-y-2">
            <p className="text-sm text-slate-500">Affichée sur votre vitrine et dans le marketplace. PNG/JPG/WEBP, max 5 Mo.</p>
            <div className="flex flex-wrap gap-2">
              <input ref={fileRef} type="file" accept="image/png,image/jpeg,image/webp" onChange={onPick} className="hidden" id="logo-input" />
              <Button onClick={() => fileRef.current?.click()} disabled={busy}>
                {busy ? 'Envoi…' : (shop.logoUrl ? 'Changer le logo' : 'Ajouter un logo')}
              </Button>
              {shop.logoUrl && <Button variant="secondary" onClick={removeLogo} disabled={busy}>Retirer</Button>}
            </div>
          </div>
        </div>
      </Card>

      {/* Lien partageable */}
      <Card title="Lien de ma boutique">
        <p className="mb-3 text-sm text-slate-500">
          Partagez ce lien dans votre bio <b>Instagram</b>, <b>Facebook</b> ou <b>TikTok</b>. Vos clients
          arrivent directement sur votre vitrine (aucun compte requis pour parcourir).
        </p>
        <div className="flex flex-col gap-2 sm:flex-row">
          <input readOnly value={link} onClick={(e) => e.target.select()}
            className="min-w-0 flex-1 rounded-xl border border-slate-200 bg-slate-50 px-4 py-2.5 text-sm text-slate-700" />
          <div className="flex gap-2">
            <Button onClick={copy}>{copied ? '✓ Copié' : '📋 Copier'}</Button>
            <Button variant="secondary" onClick={share}>Partager</Button>
          </div>
        </div>
        <div className="mt-3">
          <a href={link} target="_blank" rel="noreferrer" className="text-sm font-semibold text-brand-600 hover:underline">
            Ouvrir ma vitrine ↗
          </a>
        </div>
      </Card>

      {/* Transporteur Goodex */}
      <Card title="Livraison — Goodex (transporteur)">
        <p className="mb-3 text-sm text-slate-500">
          Renseignez vos identifiants <b>Goodex</b> (affichés en haut de votre espace{' '}
          <a href="https://client.goodex.tn" target="_blank" rel="noreferrer" className="font-semibold text-brand-600 hover:underline">client.goodex.tn</a>).
          Ils permettent de créer les colis et d'imprimer les bordereaux depuis « Commandes en ligne ».
        </p>
        <form onSubmit={saveGoodex} className="space-y-3">
          <Alert type="success" onClose={() => setGMsg('')}>{gMsg}</Alert>
          <Field label="user_id (expéditeur)">
            <Input value={gForm.userId} onChange={(e) => setGForm((f) => ({ ...f, userId: e.target.value }))} placeholder="Ex. 990" inputMode="numeric" />
          </Field>
          <Field label={`Token${goodex?.configured ? ' (déjà configuré — laissez vide pour ne pas changer)' : ''}`}>
            <Input type="password" value={gForm.token} onChange={(e) => setGForm((f) => ({ ...f, token: e.target.value }))}
              placeholder={goodex?.configured ? '•••••••••• (inchangé)' : 'Collez votre token'} autoComplete="off" />
          </Field>
          <Field label="Base URL (optionnel)">
            <Input value={gForm.baseUrl} onChange={(e) => setGForm((f) => ({ ...f, baseUrl: e.target.value }))} placeholder="https://transport.goodex.tn" />
          </Field>
          <div className="flex items-center gap-3">
            <Button type="submit" disabled={gBusy}>{gBusy ? 'Enregistrement…' : 'Enregistrer'}</Button>
            <span className={`text-sm font-medium ${goodex?.configured ? 'text-emerald-600' : 'text-slate-400'}`}>
              {goodex?.configured ? '✓ Token configuré' : '○ Token non configuré'}
            </span>
          </div>
        </form>
      </Card>
    </div>
  );
}
