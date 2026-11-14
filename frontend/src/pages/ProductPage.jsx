import { useCallback, useEffect, useRef, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import api, { errorMessage } from '../api/client.js';
import { useAuth } from '../auth/useAuth.js';
import LiveBadge from '../components/LiveBadge.jsx';
import { LivePrice, ProductImage, urgency } from '../components/ProductCard.jsx';
import { useToast } from '../components/useToast.js';
import { mergeProductUpdate } from '../hooks/useLiveProducts.js';
import useNow from '../hooks/useNow.js';
import useUserLocation from '../hooks/useUserLocation.js';
import { clock, dateTime, money, titleCase } from '../lib/format.js';
import { onReconnect, subscribe } from '../lib/live.js';

// The live auction page: price updates in real time, price history, and the Reserve form.
export default function ProductPage() {
  const { id } = useParams();
  const { user } = useAuth();
  const navigate = useNavigate();
  const toast = useToast();
  const now = useNow();
  const { location } = useUserLocation();
  const [product, setProduct] = useState(null);
  const [history, setHistory] = useState([]);
  const [error, setError] = useState('');
  const [qty, setQty] = useState(1);
  const [reserving, setReserving] = useState(false);
  const [reservation, setReservation] = useState(null);
  const lastVersion = useRef(0);

  // Initial sync: always start from the server's current price
  const load = useCallback(async () => {
    try {
      const [p, h] = await Promise.all([
        api.get(`/api/products/${id}`, { params: { lat: location.lat, lng: location.lng } }),
        api.get(`/api/products/${id}/history`),
      ]);
      lastVersion.current = p.data.version;
      setProduct(p.data);
      setHistory(h.data);
    } catch (err) {
      setError(errorMessage(err));
    }
  }, [id, location.lat, location.lng]);

  useEffect(() => { load(); }, [load]);

  useEffect(() => {
    const stopReconnect = onReconnect(load);
    const stop = subscribe(`/topic/products/${id}`, (msg) => {
      // Ignore duplicates and anything older than what we already show
      if (msg.version <= lastVersion.current) return;
      lastVersion.current = msg.version;
      setProduct((p) => (p ? mergeProductUpdate(p, msg) : p));
      if (msg.type === 'PRICE_DROP') {
        setHistory((h) => [...h, { oldPrice: msg.oldPrice, newPrice: msg.currentPrice, changedAt: msg.sentAt }]);
        toast.show(`Price dropped: ${money(msg.oldPrice)} → ${money(msg.currentPrice)}`, 'drop');
      }
    });
    return () => { stop(); stopReconnect(); };
  }, [id, load, toast]);

  if (error) return <main className="page"><p className="alert alert-error">{error}</p><Link to="/deals">Back to deals</Link></main>;
  if (!product) return <main className="page"><p className="muted">Loading...</p></main>;

  const msLeft = new Date(product.expiresAt).getTime() - now;
  const nextDropMs = product.nextDropAt ? new Date(product.nextDropAt).getTime() - now : null;
  const live = product.status === 'ACTIVE' && msLeft > 0 && product.quantityAvailable > 0;
  const canReserve = user && (user.role === 'BUYER' || user.role === 'NGO');

  const reserve = async (e) => {
    e.preventDefault();
    setReserving(true);
    try {
      const r = await api.post('/api/reservations', { productId: product.id, quantity: Number(qty) });
      setReservation(r.data);
      toast.show('Reserved! Pay at pickup.', 'success');
      window.dispatchEvent(new Event('notifications-changed'));
    } catch (err) {
      toast.show(errorMessage(err), 'error');
      load();
    } finally {
      setReserving(false);
    }
  };

  return (
    <main className="page">
      <Link to="/deals" className="muted small">← All deals</Link>
      <div className="product-page">
        <ProductImage product={product} className="hero-img" />
        <section className="card">
          <div className="page-head">
            <span className={`status status-${product.status.toLowerCase()}`}>{titleCase(product.status)}</span>
            <LiveBadge />
          </div>
          <h1 className="product-title">{product.name}</h1>
          <p className="muted">{product.description}</p>
          <LivePrice product={product} large />

          <div className={`countdown big ${urgency(msLeft)}`}>⏱ {msLeft > 0 ? `${clock(msLeft)} remaining` : 'Auction ended'}</div>
          <p className="muted small">
            {nextDropMs != null && live
              ? <>Next drop of {money(product.dropAmount)} in <strong>{clock(nextDropMs)}</strong> · never below {money(product.minPrice)}</>
              : product.status === 'ACTIVE' ? <>Lowest price reached: {money(product.minPrice)}</> : null}
          </p>
          <p><strong>{product.quantityAvailable}</strong> of {product.quantityTotal} left</p>

          {reservation ? (
            <div className="alert alert-info">
              <strong>Reserved {reservation.quantity} × {reservation.productName}</strong> for {money(reservation.totalPrice)}.<br />
              Collect from {reservation.businessName}, {reservation.businessAddress} before {dateTime(reservation.pickupBy)}.<br />
              Cash on pickup - this demo takes no online payment.{' '}
              <Link to="/reservations">My reservations</Link>
            </div>
          ) : !live ? (
            <p className="alert alert-error">This deal is no longer available.</p>
          ) : canReserve ? (
            <form className="reserve-form" onSubmit={reserve}>
              <label className="field inline">
                <span className="field-label">Quantity</span>
                <input type="number" min="1" max={product.quantityAvailable} value={qty} onChange={(e) => setQty(e.target.value)} />
              </label>
              <button className="btn btn-primary" disabled={reserving}>
                {reserving ? 'Reserving...' : `Reserve at ${money(product.currentPrice * qty)}`}
              </button>
              <p className="muted small full">The price is locked when you reserve. Pay cash when you collect.</p>
            </form>
          ) : user ? (
            <p className="muted">Only buyer and NGO accounts can reserve.</p>
          ) : (
            <button className="btn btn-primary" onClick={() => navigate('/login', { state: { from: `/products/${product.id}` } })}>
              Log in to reserve
            </button>
          )}
        </section>
      </div>

      <div className="two-col">
        <section className="card">
          <h3>Pickup location</h3>
          <p><strong>{product.business.name}</strong> ({titleCase(product.business.businessType)})</p>
          <p className="muted">{product.business.address}</p>
          {product.distanceKm != null && <p className="muted small">{product.distanceKm} km from your search location</p>}
          {product.business.phone && <p className="muted small">📞 {product.business.phone}</p>}
          <a className="small" target="_blank" rel="noreferrer"
             href={`https://www.openstreetmap.org/?mlat=${product.business.latitude}&mlon=${product.business.longitude}#map=17/${product.business.latitude}/${product.business.longitude}`}>
            Open in map
          </a>
        </section>
        <section className="card">
          <h3>Price history</h3>
          <ul className="history">
            <li><span>Start</span><span>{money(product.startPrice)}</span><span className="muted small">{dateTime(product.auctionStart)}</span></li>
            {history.map((h, i) => (
              <li key={i}><span className="drop">↓ {money(h.oldPrice)}</span><span>{money(h.newPrice)}</span><span className="muted small">{dateTime(h.changedAt)}</span></li>
            ))}
          </ul>
        </section>
      </div>
    </main>
  );
}
