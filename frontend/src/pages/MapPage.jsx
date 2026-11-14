import { useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { Marker, Popup } from 'react-leaflet';
import LiveBadge from '../components/LiveBadge.jsx';
import LocationBar from '../components/LocationBar.jsx';
import { BaseMap, ClickToPick, priceIcon, Recenter, YouAreHere } from '../components/MapParts.jsx';
import useLiveProducts from '../hooks/useLiveProducts.js';
import useUserLocation from '../hooks/useUserLocation.js';
import { money } from '../lib/format.js';

const HOUR = 60 * 60 * 1000;

// "Deals radar": live deals on an OpenStreetMap map. Tap the map to search from another spot.
export default function MapPage() {
  const { location, setLocation, useMyLocation, locating, error } = useUserLocation();
  const [radius, setRadius] = useState(5);
  const params = useMemo(() => ({ lat: location.lat, lng: location.lng, radiusKm: radius }), [location.lat, location.lng, radius]);
  const { products } = useLiveProducts('/api/products', params);

  // One marker per shop, showing its cheapest deal
  const shops = useMemo(() => {
    const byShop = new Map();
    for (const p of products) {
      const entry = byShop.get(p.business.id) || { business: p.business, distanceKm: p.distanceKm, items: [] };
      entry.items.push(p);
      byShop.set(p.business.id, entry);
    }
    return [...byShop.values()];
  }, [products]);

  return (
    <main className="page wide">
      <div className="page-head">
        <h1>Deals map</h1>
        <LiveBadge />
      </div>
      <LocationBar location={location} onUseMyLocation={useMyLocation} locating={locating} error={error}
                   radius={radius} onRadius={setRadius} />
      <p className="muted small">Tap anywhere on the map to search from that spot. {shops.length} shops with live deals.</p>

      <BaseMap center={location} zoom={radius > 10 ? 11 : 13}>
        <Recenter lat={location.lat} lng={location.lng} />
        <ClickToPick onPick={(pos) => setLocation({ ...pos, label: 'Chosen on map' })} />
        <YouAreHere location={location} radiusKm={radius} />
        {shops.map(({ business, distanceKm, items }) => {
          const cheapest = items.reduce((a, b) => (Number(a.currentPrice) <= Number(b.currentPrice) ? a : b));
          const urgent = items.some((p) => new Date(p.expiresAt).getTime() - Date.now() < HOUR);
          return (
            <Marker key={business.id} position={[Number(business.latitude), Number(business.longitude)]}
                    icon={priceIcon(cheapest.currentPrice, urgent)}>
              <Popup>
                <strong>{business.name}</strong>
                <div className="muted small">{business.address} · {distanceKm} km</div>
                <ul className="popup-list">
                  {items.map((p) => (
                    <li key={p.id}>
                      <Link to={`/products/${p.id}`}>{p.name}</Link> <strong>{money(p.currentPrice)}</strong>{' '}
                      <span className="discount">-{p.discountPercent}%</span>
                    </li>
                  ))}
                </ul>
              </Popup>
            </Marker>
          );
        })}
      </BaseMap>
    </main>
  );
}
