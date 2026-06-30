import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { creditsApi } from '../api/endpoints.js';
import { apiError } from '../api/client.js';
import { Button, Field, Input, Select, Card, Badge, Alert, Spinner, ConfirmDialog } from '../components/ui.jsx';
import { formatMoney, formatDate } from '../utils/format.js';

const STATUS = {
  PAID: { label: 'Payé', color: 'green' },
  PARTIAL: { label: 'Partiel', color: 'amber' },
  UNPAID: { label: 'Non payé', color: 'red' },
  CANCELLED: { label: 'Annulé', color: 'slate' },
};

export default function CreditDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [credit, setCredit] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');

  const [payment, setPayment] = useState({ amount: '', method: 'ESPECES' });
  const [paying, setPaying] = useState(false);
  const [confirmCancel, setConfirmCancel] = useState(false);
  const [cancelling, setCancelling] = useState(false);

  const load = async () => {
    try {
      const { data } = await creditsApi.get(id);
      setCredit(data);
    } catch (err) {
      setError(apiError(err));
    } finally {
      setLoading(false);
    }
  };
  useEffect(() => { load(); }, [id]);

  const submitPayment = async (e) => {
    e.preventDefault();
    setPaying(true);
    setError('');
    try {
      const { data } = await creditsApi.pay(id, { amount: Number(payment.amount), method: payment.method });
      setCredit(data);
      setPayment({ amount: '', method: 'ESPECES' });
      setNotice('Paiement enregistré.');
    } catch (err) {
      // 400 si > reste ou <= 0
      setError(apiError(err));
    } finally {
      setPaying(false);
    }
  };

  const doCancel = async () => {
    setCancelling(true);
    try {
      const { data } = await creditsApi.cancel(id);
      setCredit(data);
      setConfirmCancel(false);
      setNotice('Crédit annulé : stock réintégré, vente reversée.');
    } catch (err) {
      setError(apiError(err));
      setConfirmCancel(false);
    } finally {
      setCancelling(false);
    }
  };

  if (loading) return <div className="flex justify-center py-20"><Spinner className="h-8 w-8" /></div>;
  if (!credit) return <Alert type="error">{error || 'Crédit introuvable'}</Alert>;

  const st = STATUS[credit.status] || { label: credit.status, color: 'slate' };

  return (
    <div className="space-y-5">
      <div className="flex items-center justify-between">
        <h2 className="text-xl font-bold text-slate-800">Crédit #{credit.id} <Badge color={st.color}>{st.label}</Badge></h2>
        <Button variant="secondary" onClick={() => navigate(-1)}>← Retour</Button>
      </div>

      <Alert type="success" onClose={() => setNotice('')}>{notice}</Alert>
      <Alert type="error" onClose={() => setError('')}>{error}</Alert>

      <Card>
        <dl className="grid grid-cols-2 gap-y-2 text-sm md:grid-cols-4">
          <div><dt className="text-slate-500">Client</dt><dd className="font-medium">{credit.customer.name}</dd></div>
          <div><dt className="text-slate-500">Téléphone</dt><dd className="font-medium">{credit.customer.phone || '—'}</dd></div>
          <div><dt className="text-slate-500">Échéance</dt><dd className="font-medium">{credit.dueDate ? formatDate(credit.dueDate) : '—'}</dd></div>
          <div><dt className="text-slate-500">Vente</dt><dd className="font-medium">#{credit.saleId}</dd></div>
        </dl>
        <div className="mt-4 grid grid-cols-3 gap-4 text-center">
          <div className="rounded-lg bg-slate-50 p-3"><div className="text-xs text-slate-500">Total</div><div className="text-lg font-bold">{formatMoney(credit.total)}</div></div>
          <div className="rounded-lg bg-green-50 p-3"><div className="text-xs text-slate-500">Payé</div><div className="text-lg font-bold text-green-700">{formatMoney(credit.paid)}</div></div>
          <div className="rounded-lg bg-amber-50 p-3"><div className="text-xs text-slate-500">Reste</div><div className="text-lg font-bold text-amber-700">{formatMoney(credit.remaining)}</div></div>
        </div>
      </Card>

      <Card title="Articles">
        <table className="w-full text-sm">
          <thead>
            <tr className="text-left text-slate-500">
              <th className="pb-2">Produit</th><th className="pb-2">Référence</th>
              <th className="pb-2 text-right">Qté</th><th className="pb-2 text-right">Prix unit.</th><th className="pb-2 text-right">Total</th>
            </tr>
          </thead>
          <tbody>
            {credit.items.map((it) => (
              <tr key={it.id} className="border-t border-slate-100">
                <td className="py-2">{it.productName} <span className="text-slate-400">({it.colorName} · {it.size})</span></td>
                <td className="py-2 font-mono text-xs text-slate-500">{it.variantReference}</td>
                <td className="py-2 text-right">{it.quantity}</td>
                <td className="py-2 text-right">{formatMoney(it.unitPrice)}</td>
                <td className="py-2 text-right font-medium">{formatMoney(it.totalPrice)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </Card>

      <div className="grid grid-cols-1 gap-5 lg:grid-cols-2">
        <Card title="Paiements">
          {credit.payments.length === 0 ? (
            <p className="text-sm text-slate-400">Aucun paiement.</p>
          ) : (
            <table className="w-full text-sm">
              <thead><tr className="text-left text-slate-500"><th className="pb-2">Date</th><th className="pb-2">Mode</th><th className="pb-2 text-right">Montant</th></tr></thead>
              <tbody>
                {credit.payments.map((p) => (
                  <tr key={p.id} className="border-t border-slate-100">
                    <td className="py-2">{formatDate(p.createdAt)}</td>
                    <td className="py-2 text-slate-500">{p.method || '—'}</td>
                    <td className="py-2 text-right font-medium text-green-700">{formatMoney(p.amount)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </Card>

        <Card title="Enregistrer un paiement">
          {credit.cancelled ? (
            <p className="text-sm text-slate-400">Crédit annulé — aucun paiement possible.</p>
          ) : Number(credit.remaining) <= 0 ? (
            <p className="text-sm text-green-700">Crédit soldé. 👍</p>
          ) : (
            <form onSubmit={submitPayment} className="flex flex-wrap items-end gap-3">
              <Field label={`Montant (≤ ${formatMoney(credit.remaining)})`}>
                <Input type="number" step="0.01" min="0.01" max={credit.remaining} required
                  value={payment.amount} onChange={(e) => setPayment({ ...payment, amount: e.target.value })} className="w-32" />
              </Field>
              <Field label="Mode">
                <Select value={payment.method} onChange={(e) => setPayment({ ...payment, method: e.target.value })}>
                  <option value="ESPECES">Espèces</option><option value="CARTE">Carte</option><option value="MIXTE">Mixte</option>
                </Select>
              </Field>
              <Button type="submit" disabled={paying}>{paying ? '…' : 'Enregistrer'}</Button>
            </form>
          )}
        </Card>
      </div>

      {!credit.cancelled && (
        <Card>
          <div className="flex items-center justify-between">
            <div className="text-sm text-slate-600">
              <b>Annuler le crédit</b> : réintègre le stock et reverse la vente (traçable). Irréversible.
            </div>
            <Button variant="danger" onClick={() => setConfirmCancel(true)}>Annuler le crédit</Button>
          </div>
        </Card>
      )}

      <ConfirmDialog open={confirmCancel} title="Annuler le crédit"
        message="Le stock des articles sera réintégré et la vente reversée (CA corrigé). Continuer ?"
        confirmLabel="Annuler le crédit" onConfirm={doCancel} onCancel={() => setConfirmCancel(false)} loading={cancelling} />
    </div>
  );
}
