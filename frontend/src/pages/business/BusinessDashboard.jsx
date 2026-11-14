import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import api, { errorMessage } from '../../api/client.js';
import LiveBadge from '../../components/LiveBadge.jsx';
import Stat from '../../components/Stat.jsx';
import { useToast } from '../../components/useToast.js';
import { onReconnect, subscribe } from '../../lib/live.js';
import { mergeProductUpdate } from '../../hooks/useLiveProducts.js';
import { dateTime, money, titleCase } from '../../lib/format.js';

// Seller home: approval status, stats, and every listing with live prices and actions.
export default function BusinessDashboard() {
  const toast = useToast();
  const [shop, setShop] = useState(undefined);   // undefined = loading, null = no profile yet
  const [stats, setStats] = useState(null);
  const [products, setProducts] = useState([]);

  const load = useCallback(async () => {
    try {
      const s = await api.get('/api/businesses/my');
      setShop(s.data);
      const [d, p] = await Promise.all([api.get('/api/dashboard/business'), api.get('/api/products/mine')]);
      setStats(d.data);
      setProducts(p.data);
    } catch (err) {
      if (err.response?.status === 404) setShop(null);
      else toast.show(errorMessage(err), 'error');
    }
  }, [toast]);

  useEffect(() => { load(); }, [load]);

  // Live prices and stock for my own products
  useEffect(() => {
    const stopReconnect = onReconnect(load);
    const stop = subscribe('/topic/products', (msg) => {
      setProducts((list) => list.map((p) => (p.id === msg.productId && msg.version > p.version ? mergeProductUpdate(p, msg) : p)));
      if (msg.type === 'STOCK') api.get('/api/dashboard/business').then((d) => setStats(d.data)).catch(() => {});
    });
    return () => { stop(); stopReconnect(); };
  }, [load]);

  const act = async (id, action) => {
    if (action === 'cancel' && !window.confirm('Cancel this auction? Existing reservations stay valid.')) return;
    try {
      if (action === 'cancel') await api.delete(`/api/products/${id}`);
      else await api.put(`/api/products/${id}/${action}`);
      toast.show(`Auction ${action === 'cancel' ? 'cancelled' : action + 'd'}`, 'success');
      load();
    } catch (err) {
      toast.show(errorMessage(err), 'error');
    }
  };

  if (shop === undefined) return <main className="page"><p className="muted">Loading...</p></main>;
  if (shop === null) {
    return (
      <main className="page narrow">
        <div className="card">
          <h2>Welcome! Set up your shop</h2>
          <p className="muted">Add your shop name, address and map location. An admin will approve it so you can start listing products.</p>
          <Link to="/business/profile" className="btn btn-primary">Create shop profile</Link>
        </div>
      </main>
    );
  }

  return (
    <main className="page wide">
      <div className="page-head">
        <div>
          <h1>{shop.name}</h1>
          <span className={`status status-${shop.status.toLowerCase()}`}>{titleCase(shop.status)}</span>{' '}
          <Link className="small" to="/business/profile">Edit shop</Link>
        </div>
        <div className="head-actions">
          <LiveBadge />
          {shop.status === 'APPROVED' && <Link to="/business/products/new" className="btn btn-primary">+ Add product</Link>}
        </div>
      </div>

      {shop.status === 'PENDING' && <p className="alert alert-info">Your shop is waiting for admin approval. You can list products once it is approved.</p>}
      {shop.status === 'REJECTED' && <p className="alert alert-error">Your shop was not approved. Update your details to send it for review again.</p>}

      {stats && (
        <div className="stats">
          <Stat label="Active listings" value={stats.activeListings} />
          <Stat label="Total inventory listed" value={stats.totalInventoryListed} />
          <Stat label="Products sold (collected)" value={stats.productsSold} />
          <Stat label="Revenue recovered" value={money(stats.revenueRecovered)} tone="green" />
          <Stat label="Active reservations" value={stats.activeReservations} />
        </div>
      )}

      <div className="page-head">
        <h2>My auctions</h2>
        <Link to="/business/reservations">Orders &amp; reservations →</Link>
      </div>
      {products.length === 0 ? <div className="card empty">No products yet.</div> : (
        <div className="table-wrap card">
          <table>
            <thead>
              <tr><th>Product</th><th>Price now</th><th>Start → Min</th><th>Stock</th><th>Expires</th><th>Status</th><th /></tr>
            </thead>
            <tbody>
              {products.map((p) => (
                <tr key={p.id}>
                  <td><Link to={`/products/${p.id}`}>{p.name}</Link></td>
                  <td><strong key={p.flashAt || 0} className={p.flashAt ? 'flash' : ''}>{money(p.currentPrice)}</strong> <span className="discount">-{p.discountPercent}%</span></td>
                  <td className="small">{money(p.startPrice)} → {money(p.minPrice)}<br /><span className="muted">-{money(p.dropAmount)} / {p.dropIntervalMinutes} min</span></td>
                  <td>{p.quantityAvailable}/{p.quantityTotal}</td>
                  <td className="small">{dateTime(p.expiresAt)}</td>
                  <td><span className={`status status-${p.status.toLowerCase()}`}>{titleCase(p.status)}</span></td>
                  <td className="row-actions">
                    {p.status === 'ACTIVE' && <button className="btn btn-ghost" onClick={() => act(p.id, 'pause')}>Pause</button>}
                    {p.status === 'PAUSED' && <button className="btn btn-ghost" onClick={() => act(p.id, 'resume')}>Resume</button>}
                    {['ACTIVE', 'PAUSED', 'SOLD_OUT'].includes(p.status) && <button className="btn btn-ghost danger" onClick={() => act(p.id, 'cancel')}>Cancel</button>}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </main>
  );
}
