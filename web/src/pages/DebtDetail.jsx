import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { debtsApi } from '../api/endpoints.js';
import { apiError } from '../api/client.js';
import { Button, Field, Input, Select, Card, Badge, Alert, Spinner, ConfirmDialog } from '../components/ui.jsx';
import { formatMoney, formatDate } from '../utils/format.js';

const STATUS = {
  PAID: { label: 'Payée', color: 'green' },
  PARTIAL: { label: 'Partiellement payée', color: 'amber' },
  UNPAID: { label: 'Non payée', color: 'red' },
};

export default function DebtDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [debt, setDebt] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [payment, setPayment] = useState({ amount: '', method: 'ESPECES' });
  const [paying, setPaying] = useState(false);
  const [confirmDel, setConfirmDel] = useState(false);
  const [deleting, setDeleting] = useState(false);

  const load = async () => {
    try { const { data } = await debtsApi.get(id); setDebt(data); }
    catch (err) { setError(apiError(err)); } finally { setLoading(false); }
  };
  useEffect(() => { load(); }, [id]);

  const submitPayment = async (e) => {
    e.preventDefault(); setPaying(true); setError('');
    try {
      const { data } = await debtsApi.pay(id, { amount: Number(payment.amount), method: payment.method });
      setDebt(data); setPayment({ amount: '', method: 'ESPECES' }); setNotice('Paiement enregistré.');
    } catch (err) { setError(apiError(err)); } finally { setPaying(false); }
  };

  const doDelete = async () => {
    setDeleting(true);
    try { await debtsApi.remove(id); navigate('/dettes'); }
    catch (err) { setError(apiError(err)); setConfirmDel(false); } finally { setDeleting(false); }
  };

  if (loading) return <div className="flex justify-center py-20"><Spinner className="h-8 w-8" /></div>;
  if (!debt) return <Alert type="error">{error || 'Dette introuvable'}</Alert>;
  const st = STATUS[debt.status] || { label: debt.status, color: 'slate' };

  return (
    <div className="space-y-5">
      <div className="flex items-center justify-between">
        <h2 className="text-xl font-bold text-slate-800">Dette #{debt.id} <Badge color={st.color}>{st.label}</Badge></h2>
        <Button variant="secondary" onClick={() => navigate(-1)}>← Retour</Button>
      </div>

      <Alert type="success" onClose={() => setNotice('')}>{notice}</Alert>
      <Alert type="error" onClose={() => setError('')}>{error}</Alert>

      <Card>
        <dl className="grid grid-cols-2 gap-y-2 text-sm md:grid-cols-4">
          <div><dt className="text-slate-500">Fournisseur</dt><dd className="font-medium">{debt.supplier.name}</dd></div>
          <div><dt className="text-slate-500">Téléphone</dt><dd className="font-medium">{debt.supplier.phone || '—'}</dd></div>
          <div><dt className="text-slate-500">Échéance</dt><dd className="font-medium">{debt.dueDate ? formatDate(debt.dueDate) : '—'}</dd></div>
          <div><dt className="text-slate-500">Réf. facture</dt><dd className="font-medium">{debt.invoiceReference || '—'}</dd></div>
          <div><dt className="text-slate-500">Produit lié</dt><dd className="font-medium">{debt.productName || '—'} <span className="text-xs text-slate-400">(descriptif)</span></dd></div>
          <div className="md:col-span-3"><dt className="text-slate-500">Description</dt><dd className="font-medium">{debt.description || '—'}</dd></div>
        </dl>
        <div className="mt-4 grid grid-cols-3 gap-4 text-center">
          <div className="rounded-lg bg-slate-50 p-3"><div className="text-xs text-slate-500">Total</div><div className="text-lg font-bold">{formatMoney(debt.total)}</div></div>
          <div className="rounded-lg bg-green-50 p-3"><div className="text-xs text-slate-500">Payé</div><div className="text-lg font-bold text-green-700">{formatMoney(debt.paid)}</div></div>
          <div className="rounded-lg bg-red-50 p-3"><div className="text-xs text-slate-500">Reste</div><div className="text-lg font-bold text-red-700">{formatMoney(debt.remaining)}</div></div>
        </div>
      </Card>

      <div className="grid grid-cols-1 gap-5 lg:grid-cols-2">
        <Card title="Paiements">
          {debt.payments.length === 0 ? (
            <p className="text-sm text-slate-400">Aucun paiement.</p>
          ) : (
            <table className="w-full text-sm">
              <thead><tr className="text-left text-slate-500"><th className="pb-2">Date</th><th className="pb-2">Mode</th><th className="pb-2 text-right">Montant</th></tr></thead>
              <tbody>
                {debt.payments.map((p) => (
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
          {Number(debt.remaining) <= 0 ? (
            <p className="text-sm text-green-700">Dette soldée. 👍</p>
          ) : (
            <form onSubmit={submitPayment} className="flex flex-wrap items-end gap-3">
              <Field label={`Montant (≤ ${formatMoney(debt.remaining)})`}>
                <Input type="number" step="0.01" min="0.01" max={debt.remaining} required
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

      <Card>
        <div className="flex items-center justify-between">
          <div className="text-sm text-slate-600">
            <b>Supprimer la dette</b> : possible uniquement si <b>aucun paiement</b> n'est enregistré (sinon l'historique est préservé).
          </div>
          <Button variant="danger" disabled={debt.payments.length > 0} onClick={() => setConfirmDel(true)}>Supprimer</Button>
        </div>
      </Card>

      <ConfirmDialog open={confirmDel} title="Supprimer la dette"
        message="Supprimer définitivement cette dette (sans paiement) ?"
        confirmLabel="Supprimer" onConfirm={doDelete} onCancel={() => setConfirmDel(false)} loading={deleting} />
    </div>
  );
}
