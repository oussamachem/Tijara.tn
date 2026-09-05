import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { salesApi } from '../api/endpoints.js';
import { apiError } from '../api/client.js';
import { Button, Card, Alert, Spinner } from '../components/ui.jsx';
import { formatMoney, formatDate } from '../utils/format.js';

export default function SaleDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [sale, setSale] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    (async () => {
      try {
        const { data } = await salesApi.detail(id);
        setSale(data);
      } catch (err) {
        setError(apiError(err));
      } finally {
        setLoading(false);
      }
    })();
  }, [id]);

  if (loading) return <div className="flex justify-center py-20"><Spinner className="h-8 w-8" /></div>;
  if (error) return <Alert type="error">{error}</Alert>;
  if (!sale) return null;

  return (
    <div className="space-y-5">
      <div className="flex items-center justify-between">
        <h2 className="text-xl font-bold text-slate-800">Vente #{sale.id}</h2>
        <Button variant="secondary" onClick={() => navigate(-1)}>← Retour</Button>
      </div>

      <Card>
        <dl className="grid grid-cols-2 gap-y-2 text-sm md:grid-cols-4">
          <div>
            <dt className="text-slate-500">Date</dt>
            <dd className="font-medium">{formatDate(sale.saleDate)}</dd>
          </div>
          <div>
            <dt className="text-slate-500">Vendeur</dt>
            <dd className="font-medium">{sale.sellerName}</dd>
          </div>
          <div>
            <dt className="text-slate-500">Paiement</dt>
            <dd className="font-medium">{sale.paymentMethod}</dd>
          </div>
          <div>
            <dt className="text-slate-500">Total</dt>
            <dd className="font-bold text-green-700">{formatMoney(sale.totalAmount)}</dd>
          </div>
        </dl>
      </Card>

      <Card title="Articles">
        <div className="overflow-x-auto">
        <table className="w-full min-w-[480px] text-sm">
          <thead>
            <tr className="text-left text-slate-500">
              <th className="pb-2">Produit</th>
              <th className="pb-2">Déclinaison</th>
              <th className="pb-2">Référence</th>
              <th className="pb-2 text-right">Qté</th>
              <th className="pb-2 text-right">Prix unitaire</th>
              <th className="pb-2 text-right">Total ligne</th>
            </tr>
          </thead>
          <tbody>
            {sale.items.map((it) => (
              <tr key={it.id} className="border-t border-slate-100">
                <td className="py-2">{it.productName}</td>
                <td className="py-2 text-slate-500">{it.colorName} · {it.size}</td>
                <td className="py-2 font-mono text-xs text-slate-500">{it.variantReference}</td>
                <td className="py-2 text-right">{it.quantity}</td>
                <td className="py-2 text-right">{formatMoney(it.unitPrice)}</td>
                <td className="py-2 text-right font-medium">{formatMoney(it.totalPrice)}</td>
              </tr>
            ))}
          </tbody>
          <tfoot>
            <tr className="border-t border-slate-200">
              <td colSpan={5} className="py-2 text-right text-slate-500">Sous-total</td>
              <td className="py-2 text-right">{formatMoney(sale.subtotal)}</td>
            </tr>
            <tr>
              <td colSpan={5} className="py-1 text-right text-slate-500">Remise</td>
              <td className="py-1 text-right text-red-600">- {formatMoney(sale.discount)}</td>
            </tr>
            <tr>
              <td colSpan={5} className="py-1 text-right font-semibold text-slate-700">Total</td>
              <td className="py-1 text-right font-bold text-green-700">{formatMoney(sale.totalAmount)}</td>
            </tr>
          </tfoot>
        </table>
        </div>
      </Card>
    </div>
  );
}
