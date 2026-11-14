import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import api, { errorMessage } from '../../api/client.js';
import LiveBadge from '../../components/LiveBadge.jsx';
import { useToast } from '../../components/useToast.js';
import { mergeProductUpdate } from '../../hooks/useLiveProducts.js';
import { dateTime, money, titleCase } from '../../lib/format.js';
import { onReconnect, subscribe } from '../../lib/live.js';

// Auction monitoring: every product with live prices; remove fraudulent listings.
export default function AdminProductsPage() {
  const toast = useToast();
  const [list, setList] = useState([]);
  const [filter, setFilter] = useState('ACTIVE');

  const load = useCallback(() => api.get('/api/admin/products').then((r) => setList(r.data)), []);
  useEffect(() => { load().catch((err) => toast.show(errorMessage(err), 'error')); }, [load, toast]);
  useEffect(() => {
    const stopReconnect = onReconnect(load);
    const stop = subscribe('/topic/products', (msg) => {
      if (msg.type === 'NEW') { load(); return; }
      setList((l) => l.map((p) => (p.id === msg.productId && msg.version > p.version ? mergeProductUpdate(p, msg) : p)));
    });
    return () => { stop(); stopReconnect(); };
  }, [load]);

  const remove = async (p) => {
    if (!window.confirm(`Remove "${p.name}"? Open reservations will be cancelled.`)) return;
    try {
      await api.put(`/api/admin/products/${p.id}/remove`);
      toast.show('Listing removed', 'success');
      load();
    } catch (err) {
      toast.show(errorMessage(err), 'error');
    }
  };

  const shown = filter ? list.filter((p) => p.status === filter) : list;

  return (
    <main className="page wide">
      <div className="page-head">
        <h1>Products &amp; auctions <LiveBadge /></h1>
        <div className="segmented">
          {['ACTIVE', 'PAUSED', 'SOLD_OUT', 'EXPIRED', 'CANCELLED', 'REMOVED', ''].map((s) => (
            <button key={s} className={filter === s ? 'active' : ''} onClick={() => setFilter(s)}>{s ? titleCase(s) : 'All'}</button>
          ))}
        </div>
      </div>
      <div className="table-wrap card">
        <table>
          <thead><tr><th>Product</th><th>Shop</th><th>Price</th><th>Stock</th><th>Expires</th><th>Status</th><th /></tr></thead>
          <tbody>
            {shown.map((p) => (
              <tr key={p.id}>
                <td><Link to={`/products/${p.id}`}>{p.name}</Link></td>
                <td className="small">{p.business.name}</td>
                <td><strong key={p.flashAt || 0} className={p.flashAt ? 'flash' : ''}>{money(p.currentPrice)}</strong> <span className="muted small">min {money(p.minPrice)}</span></td>
                <td>{p.quantityAvailable}/{p.quantityTotal}</td>
                <td className="small">{dateTime(p.expiresAt)}</td>
                <td><span className={`status status-${p.status.toLowerCase()}`}>{titleCase(p.status)}</span></td>
                <td>{p.status !== 'REMOVED' && <button className="btn btn-ghost danger" onClick={() => remove(p)}>Remove</button>}</td>
              </tr>
            ))}
          </tbody>
        </table>
        {shown.length === 0 && <p className="muted center">Nothing here.</p>}
      </div>
    </main>
  );
}
