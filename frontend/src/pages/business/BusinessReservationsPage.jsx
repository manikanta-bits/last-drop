import { useCallback, useEffect, useState } from 'react';
import api, { errorMessage } from '../../api/client.js';
import { useToast } from '../../components/useToast.js';
import { dateTime, money, titleCase } from '../../lib/format.js';

// Seller view of every reservation. "Mark collected" when the buyer picks up and pays.
export default function BusinessReservationsPage() {
  const toast = useToast();
  const [list, setList] = useState([]);
  const [filter, setFilter] = useState('RESERVED');

  const load = useCallback(() => api.get('/api/reservations/business').then((r) => setList(r.data)), []);
  useEffect(() => {
    load().catch((err) => toast.show(errorMessage(err), 'error'));
    const id = setInterval(() => load().catch(() => {}), 15000);
    return () => clearInterval(id);
  }, [load, toast]);

  const complete = async (id) => {
    try {
      await api.put(`/api/reservations/${id}/complete`);
      toast.show('Marked as collected', 'success');
      load();
    } catch (err) {
      toast.show(errorMessage(err), 'error');
    }
  };

  const shown = filter ? list.filter((r) => r.status === filter) : list;

  return (
    <main className="page">
      <div className="page-head">
        <h1>Orders &amp; reservations</h1>
        <div className="segmented">
          {['RESERVED', 'COMPLETED', 'CANCELLED', 'EXPIRED', ''].map((s) => (
            <button key={s} className={filter === s ? 'active' : ''} onClick={() => setFilter(s)}>{s ? titleCase(s) : 'All'}</button>
          ))}
        </div>
      </div>
      {shown.length === 0 ? <div className="card empty">Nothing here.</div> : shown.map((r) => (
        <div key={r.id} className="card row-card">
          <div>
            <strong>{r.quantity} × {r.productName}</strong> · {money(r.totalPrice)}
            <div className="small muted">{r.buyerName} ({titleCase(r.buyerRole)}) · {r.buyerEmail}</div>
            <div className="small muted">Reserved {dateTime(r.createdAt)} · pickup by {dateTime(r.pickupBy)}</div>
          </div>
          <div className="row-actions">
            <span className={`status status-${r.status.toLowerCase()}`}>{titleCase(r.status)}</span>
            {r.status === 'RESERVED' && <button className="btn btn-primary" onClick={() => complete(r.id)}>Mark collected</button>}
          </div>
        </div>
      ))}
    </main>
  );
}
