import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import api, { errorMessage } from '../api/client.js';
import Stat from '../components/Stat.jsx';
import { useToast } from '../components/useToast.js';
import { dateTime, money, titleCase } from '../lib/format.js';

// Buyer / NGO dashboard: savings, impact estimate and reservation history.
export default function ReservationsPage() {
  const toast = useToast();
  const [list, setList] = useState([]);
  const [stats, setStats] = useState(null);

  const load = useCallback(async () => {
    const [r, d] = await Promise.all([api.get('/api/reservations/my'), api.get('/api/dashboard/buyer')]);
    setList(r.data);
    setStats(d.data);
  }, []);

  useEffect(() => { load().catch(() => toast.show('Could not load reservations', 'error')); }, [load, toast]);

  const cancel = async (id) => {
    if (!window.confirm('Cancel this reservation? The item goes back on sale.')) return;
    try {
      await api.put(`/api/reservations/${id}/cancel`);
      toast.show('Reservation cancelled', 'info');
      load();
    } catch (err) {
      toast.show(errorMessage(err), 'error');
    }
  };

  return (
    <main className="page">
      <h1>My reservations</h1>
      {stats && (
        <div className="stats">
          <Stat label="Active reservations" value={stats.activeReservations} />
          <Stat label="Total savings" value={money(stats.totalSavings)} tone="green" />
          <Stat label="Items rescued" value={stats.impact.itemsRescued} />
          <Stat label="Food saved (estimate)" value={`${stats.impact.estimatedFoodKg} kg`} note={stats.impact.assumptions} />
        </div>
      )}

      {list.length === 0 ? (
        <div className="card empty">No reservations yet. <Link to="/deals">Find a deal</Link></div>
      ) : list.map((r) => (
        <div key={r.id} className="card row-card">
          <div>
            <Link to={`/products/${r.productId}`}><strong>{r.quantity} × {r.productName}</strong></Link>
            <div className="muted small">{r.businessName} · {r.businessAddress}</div>
            <div className="small">
              {money(r.totalPrice)} <span className="muted">(saved {money(r.savings)})</span> · reserved {dateTime(r.createdAt)}
            </div>
            {r.status === 'RESERVED' && <div className="small warning-text">Collect before {dateTime(r.pickupBy)} · Cash on pickup</div>}
          </div>
          <div className="row-actions">
            <span className={`status status-${r.status.toLowerCase()}`}>{titleCase(r.status)}</span>
            {r.status === 'RESERVED' && <button className="btn btn-ghost" onClick={() => cancel(r.id)}>Cancel</button>}
          </div>
        </div>
      ))}
    </main>
  );
}
