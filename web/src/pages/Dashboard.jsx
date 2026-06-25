import { useEffect, useState } from 'react';
import { dashboardApi, variantsApi } from '../api/endpoints.js';
import { apiError } from '../api/client.js';
import { Card, Spinner, Alert, Badge } from '../components/ui.jsx';
import { formatMoney } from '../utils/format.js';

function StatCard({ label, value, sub, accent = 'text-slate-800' }) {
  return (
    <div className="rounded-xl border border-slate-200 bg-white p-5 shadow-sm">
      <div className="text-sm text-slate-500">{label}</div>
      <div className={`mt-1 text-2xl font-bold ${accent}`}>{value}</div>
      {sub && <div className="mt-1 text-xs text-slate-400">{sub}</div>}
    </div>
  );
}

export default function Dashboard() {
  const [data, setData] = useState(null);
  const [lowStock, setLowStock] = useState([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    (async () => {
      try {
        const [dash, low] = await Promise.all([dashboardApi.get(), variantsApi.lowStock()]);
        setData(dash.data);
        setLowStock(low.data);
      } catch (err) {
        setError(apiError(err));
      } finally {
        setLoading(false);
      }
    })();
  }, []);

  if (loading) {
    return (
      <div className="flex justify-center py-20">
        <Spinner className="h-8 w-8" />
      </div>
    );
  }

  if (error) return <Alert type="error">{error}</Alert>;

  return (
    <div className="space-y-6">
      <h2 className="text-xl font-bold text-slate-800">Tableau de bord</h2>

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <StatCard label="Produits" value={data.totalProducts} />
        <StatCard label="Stock total" value={data.totalStock} sub="unités" />
        <StatCard
          label="Variantes sous seuil / rupture"
          value={data.lowStockVariants}
          sub={`sur ${data.lowStockProducts} produit(s)`}
          accent={data.lowStockVariants > 0 ? 'text-amber-600' : 'text-slate-800'}
        />
        <StatCard
          label="Ventes du jour"
          value={data.todaySalesCount}
          sub={`CA net : ${formatMoney(data.todayNetRevenue)}`}
        />
      </div>

      <div className="grid grid-cols-1 gap-4 lg:grid-cols-3">
        <Card title="Chiffre d'affaires du jour" className="lg:col-span-1">
          <dl className="space-y-2 text-sm">
            <div className="flex justify-between">
              <dt className="text-slate-500">Ventes (brut)</dt>
              <dd className="font-medium">{formatMoney(data.todayGrossRevenue)}</dd>
            </div>
            <div className="flex justify-between">
              <dt className="text-slate-500">Retours</dt>
              <dd className="font-medium text-red-600">- {formatMoney(data.todayReturnsValue)}</dd>
            </div>
            <div className="flex justify-between border-t border-slate-100 pt-2">
              <dt className="font-semibold text-slate-700">CA net</dt>
              <dd className="font-bold text-green-700">{formatMoney(data.todayNetRevenue)}</dd>
            </div>
          </dl>
        </Card>

        <Card title="Produits les plus vendus" className="lg:col-span-2">
          {data.topSellingProducts.length === 0 ? (
            <p className="text-sm text-slate-400">Aucune vente enregistrée.</p>
          ) : (
            <table className="w-full text-sm">
              <thead>
                <tr className="text-left text-slate-500">
                  <th className="pb-2">Produit</th>
                  <th className="pb-2">Référence</th>
                  <th className="pb-2 text-right">Qté vendue</th>
                  <th className="pb-2 text-right">CA</th>
                </tr>
              </thead>
              <tbody>
                {data.topSellingProducts.map((p) => (
                  <tr key={p.productId} className="border-t border-slate-100">
                    <td className="py-2">{p.name}</td>
                    <td className="py-2 text-slate-500">{p.reference}</td>
                    <td className="py-2 text-right font-medium">{p.quantitySold}</td>
                    <td className="py-2 text-right">{formatMoney(p.revenue)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </Card>
      </div>

      <Card title="Variantes sous le seuil d'alerte">
        {lowStock.length === 0 ? (
          <p className="text-sm text-slate-400">Aucune variante en alerte. 👍</p>
        ) : (
          <table className="w-full text-sm">
            <thead>
              <tr className="text-left text-slate-500">
                <th className="pb-2">Produit</th>
                <th className="pb-2">Déclinaison</th>
                <th className="pb-2">Référence</th>
                <th className="pb-2 text-right">Stock</th>
                <th className="pb-2 text-right">Seuil</th>
                <th className="pb-2 text-right">État</th>
              </tr>
            </thead>
            <tbody>
              {lowStock.map((v) => (
                <tr key={v.variantId} className="border-t border-slate-100">
                  <td className="py-2">{v.productName}</td>
                  <td className="py-2 text-slate-500">{v.colorName} · {v.size}</td>
                  <td className="py-2 font-mono text-xs text-slate-400">{v.variantReference}</td>
                  <td className="py-2 text-right font-medium">{v.quantity}</td>
                  <td className="py-2 text-right text-slate-500">{v.seuilAlerte}</td>
                  <td className="py-2 text-right">
                    {v.quantity === 0 ? <Badge color="red">Rupture</Badge> : <Badge color="amber">Faible</Badge>}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </Card>
    </div>
  );
}
