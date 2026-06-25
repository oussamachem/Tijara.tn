import { useEffect, useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { salesApi, returnsApi, sellersApi } from '../api/endpoints.js';
import { apiError } from '../api/client.js';
import { Button, Field, Input, Select, Card, Alert, Spinner, Pagination } from '../components/ui.jsx';
import { formatMoney, formatDate } from '../utils/format.js';

const PAGE_SIZE = 10;

export default function SalesHistory() {
  const navigate = useNavigate();
  const [tab, setTab] = useState('sales');
  const [filters, setFilters] = useState({ from: '', to: '', sellerId: '' });
  const [page, setPage] = useState(0);
  const [data, setData] = useState({ content: [], totalPages: 0, totalElements: 0 });
  const [sellers, setSellers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    sellersApi.list().then((r) => setSellers(r.data)).catch(() => {});
  }, []);

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const params = { page, size: PAGE_SIZE };
      if (filters.from) params.from = filters.from;
      if (filters.to) params.to = filters.to;
      let res;
      if (tab === 'sales') {
        if (filters.sellerId) params.sellerId = filters.sellerId;
        res = await salesApi.history(params);
      } else {
        res = await returnsApi.history(params);
      }
      setData(res.data);
    } catch (err) {
      setError(apiError(err));
    } finally {
      setLoading(false);
    }
  }, [tab, page, filters]);

  useEffect(() => { load(); }, [load]);

  const switchTab = (t) => { setTab(t); setPage(0); };
  const submitFilters = (e) => { e.preventDefault(); setPage(0); load(); };
  const resetFilters = () => { setFilters({ from: '', to: '', sellerId: '' }); setPage(0); };

  return (
    <div className="space-y-5">
      <h2 className="text-xl font-bold text-slate-800">Historique</h2>

      {/* Onglets */}
      <div className="flex gap-2 border-b border-slate-200">
        <button
          onClick={() => switchTab('sales')}
          className={`-mb-px border-b-2 px-4 py-2 text-sm font-medium ${tab === 'sales' ? 'border-brand-600 text-brand-700' : 'border-transparent text-slate-500 hover:text-slate-700'}`}
        >
          Ventes
        </button>
        <button
          onClick={() => switchTab('returns')}
          className={`-mb-px border-b-2 px-4 py-2 text-sm font-medium ${tab === 'returns' ? 'border-brand-600 text-brand-700' : 'border-transparent text-slate-500 hover:text-slate-700'}`}
        >
          Retours
        </button>
      </div>

      <Alert type="error" onClose={() => setError('')}>{error}</Alert>

      {/* Filtres */}
      <Card>
        <form onSubmit={submitFilters} className="grid grid-cols-1 gap-3 md:grid-cols-4">
          <Field label="Du">
            <Input type="date" value={filters.from} onChange={(e) => setFilters({ ...filters, from: e.target.value })} />
          </Field>
          <Field label="Au">
            <Input type="date" value={filters.to} onChange={(e) => setFilters({ ...filters, to: e.target.value })} />
          </Field>
          {tab === 'sales' && (
            <Field label="Vendeur">
              <Select value={filters.sellerId} onChange={(e) => setFilters({ ...filters, sellerId: e.target.value })}>
                <option value="">Tous</option>
                {sellers.map((s) => (
                  <option key={s.id} value={s.id}>{s.fullName}</option>
                ))}
              </Select>
            </Field>
          )}
          <div className="flex items-end gap-2">
            <Button type="submit">Filtrer</Button>
            <Button type="button" variant="secondary" onClick={resetFilters}>Réinitialiser</Button>
          </div>
        </form>
      </Card>

      {/* Tableau */}
      <Card>
        {loading ? (
          <div className="flex justify-center py-10"><Spinner className="h-7 w-7" /></div>
        ) : data.content.length === 0 ? (
          <p className="py-6 text-center text-sm text-slate-400">Aucun résultat.</p>
        ) : tab === 'sales' ? (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="text-left text-slate-500">
                  <th className="pb-2">N°</th>
                  <th className="pb-2">Date</th>
                  <th className="pb-2">Vendeur</th>
                  <th className="pb-2">Paiement</th>
                  <th className="pb-2 text-right">Articles</th>
                  <th className="pb-2 text-right">Total</th>
                  <th className="pb-2"></th>
                </tr>
              </thead>
              <tbody>
                {data.content.map((s) => (
                  <tr
                    key={s.id}
                    className="cursor-pointer border-t border-slate-100 hover:bg-slate-50"
                    onClick={() => navigate(`/ventes/${s.id}`)}
                  >
                    <td className="py-2 font-mono text-xs">#{s.id}</td>
                    <td className="py-2">{formatDate(s.saleDate)}</td>
                    <td className="py-2">{s.sellerName}</td>
                    <td className="py-2 text-slate-500">{s.paymentMethod}</td>
                    <td className="py-2 text-right">{s.itemCount}</td>
                    <td className="py-2 text-right font-medium">{formatMoney(s.totalAmount)}</td>
                    <td className="py-2 text-right text-brand-600">Détail →</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="text-left text-slate-500">
                  <th className="pb-2">Date</th>
                  <th className="pb-2">Vente</th>
                  <th className="pb-2">Produit</th>
                  <th className="pb-2 text-right">Qté</th>
                  <th className="pb-2">Motif</th>
                </tr>
              </thead>
              <tbody>
                {data.content.map((r) => (
                  <tr key={r.id} className="border-t border-slate-100">
                    <td className="py-2">{formatDate(r.returnDate)}</td>
                    <td className="py-2 font-mono text-xs">#{r.saleId}</td>
                    <td className="py-2">{r.productName} <span className="text-slate-400">({r.colorName} · {r.size})</span></td>
                    <td className="py-2 text-right font-medium">{r.quantity}</td>
                    <td className="py-2 text-slate-500">{r.reason || '—'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
        <Pagination page={page} totalPages={data.totalPages} onChange={setPage} />
      </Card>
    </div>
  );
}
