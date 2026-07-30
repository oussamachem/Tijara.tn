import { useEffect, useState } from 'react';
import { notificationsApi } from '../api/endpoints';
import { apiError } from '../api/client';
import Header from '../components/Header.jsx';
import { Spinner, ErrorNote, EmptyState } from '../components/ui.jsx';
import { formatDate } from '../lib/format';

/** Notifications in-app (ex. « commande prête »). L'ouverture de la page marque tout comme lu. */
export default function Notifications({ onSeen }) {
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    notificationsApi
      .list()
      .then(({ data }) => {
        setItems(data);
        if (data.some((n) => !n.read)) {
          notificationsApi.markAllRead().then(() => onSeen?.()).catch(() => {});
        }
      })
      .catch((e) => setError(apiError(e)))
      .finally(() => setLoading(false));
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  return (
    <div>
      <Header title="Notifications" back />
      <div className="space-y-3 p-4">
        <ErrorNote message={error} />
        {loading ? (
          <Spinner label="Chargement…" />
        ) : items.length === 0 ? (
          <EmptyState icon="🔔" title="Aucune notification" sub="Vous serez prévenu ici quand une commande change de statut." />
        ) : (
          items.map((n) => (
            <div key={n.id} className={`rounded-2xl border p-4 shadow-card ${n.read ? 'border-slate-100 bg-white' : 'border-brand-200 bg-brand-50'}`}>
              <div className="flex items-start justify-between gap-2">
                <span className="font-semibold text-slate-800">{n.title}</span>
                {!n.read && <span className="mt-1 h-2 w-2 shrink-0 rounded-full bg-brand-600" />}
              </div>
              {n.body && <p className="mt-0.5 text-sm text-slate-600">{n.body}</p>}
              <p className="mt-1 text-xs text-slate-400">{formatDate(n.createdAt)}</p>
            </div>
          ))
        )}
      </div>
    </div>
  );
}
