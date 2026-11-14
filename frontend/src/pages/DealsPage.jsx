import { useMemo, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import LiveBadge from '../components/LiveBadge.jsx';
import LocationBar from '../components/LocationBar.jsx';
import ProductCard from '../components/ProductCard.jsx';
import { useToast } from '../components/useToast.js';
import useLiveProducts from '../hooks/useLiveProducts.js';
import useUserLocation from '../hooks/useUserLocation.js';
import { CATEGORIES, CATEGORY_ICON, money, titleCase } from '../lib/format.js';

// Nearby deals: filters + live-updating product cards.
export default function DealsPage() {
  const toast = useToast();
  const [searchParams] = useSearchParams();
  const { location, useMyLocation, locating, error: locError } = useUserLocation();
  const [radius, setRadius] = useState(10);
  const [q, setQ] = useState(() => searchParams.get('q') || '');
  const [category, setCategory] = useState(() => searchParams.get('category') || '');
  const [maxPrice, setMaxPrice] = useState('');
  const [sort, setSort] = useState('');

  const params = useMemo(() => ({
    lat: location.lat, lng: location.lng, radiusKm: radius,
    q: q || undefined, category: category || undefined, maxPrice: maxPrice || undefined, sort: sort || undefined,
  }), [location.lat, location.lng, radius, q, category, maxPrice, sort]);

  const { products, loading, error } = useLiveProducts('/api/products', params, {
    onPriceDrop: (m) => toast.show(`Price drop: ${m.name} ${money(m.oldPrice)} → ${money(m.currentPrice)}`, 'drop'),
    onNewProduct: (m) => toast.show(`New deal: ${m.name} at ${m.businessName}`, 'success'),
  });

  return (
    <main className="page wide">
      <div className="page-head">
        <h1>Nearby deals</h1>
        <LiveBadge />
      </div>

      <LocationBar location={location} onUseMyLocation={useMyLocation} locating={locating} error={locError}
                   radius={radius} onRadius={setRadius} />

      <div className="filters">
        <input type="search" placeholder="Search food or shop..." value={q} onChange={(e) => setQ(e.target.value)} />
        <select value={category} onChange={(e) => setCategory(e.target.value)} aria-label="Category">
          <option value="">All categories</option>
          {CATEGORIES.map((c) => <option key={c} value={c}>{CATEGORY_ICON[c]} {titleCase(c)}</option>)}
        </select>
        <input type="number" min="1" placeholder="Max price ₹" value={maxPrice} onChange={(e) => setMaxPrice(e.target.value)} />
        <select value={sort} onChange={(e) => setSort(e.target.value)} aria-label="Sort">
          <option value="">Ending soonest</option>
          <option value="distance">Nearest</option>
          <option value="price">Lowest price</option>
          <option value="discount">Biggest discount</option>
        </select>
      </div>

      {error && <p className="alert alert-error">{error}</p>}
      {loading ? <p className="muted">Loading deals...</p> : products.length === 0 ? (
        <div className="card empty">No live deals here right now. Try a bigger radius.</div>
      ) : (
        <div className="grid">
          {products.map((p) => <ProductCard key={p.id} product={p} />)}
        </div>
      )}
    </main>
  );
}
