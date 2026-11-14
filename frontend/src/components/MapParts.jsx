import L from 'leaflet';
import { useEffect } from 'react';
import { Circle, MapContainer, Marker, TileLayer, useMap, useMapEvents } from 'react-leaflet';
import { money } from '../lib/format.js';

// Leaflet + OpenStreetMap building blocks shared by the deals map and the shop-location picker.

export function priceIcon(price, urgent) {
  return L.divIcon({
    className: '',
    html: `<div class="map-pin ${urgent ? 'urgent' : ''}">${money(price).replace('.00', '')}</div>`,
    iconSize: [70, 28],
    iconAnchor: [35, 28],
  });
}

export const youIcon = L.divIcon({ className: '', html: '<div class="map-you"></div>', iconSize: [18, 18], iconAnchor: [9, 9] });

export function BaseMap({ center, zoom = 13, children, className = 'map' }) {
  return (
    <MapContainer center={[center.lat, center.lng]} zoom={zoom} className={className} scrollWheelZoom>
      <TileLayer
        attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
        url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
      />
      {children}
    </MapContainer>
  );
}

// Keeps the map centred when the chosen location changes
export function Recenter({ lat, lng }) {
  const map = useMap();
  useEffect(() => {
    map.setView([lat, lng]);
  }, [lat, lng, map]);
  return null;
}

// Calls onPick({lat, lng}) when the map is clicked
export function ClickToPick({ onPick }) {
  useMapEvents({ click: (e) => onPick({ lat: e.latlng.lat, lng: e.latlng.lng }) });
  return null;
}

export function YouAreHere({ location, radiusKm }) {
  return (
    <>
      <Marker position={[location.lat, location.lng]} icon={youIcon} />
      {radiusKm && (
        <Circle center={[location.lat, location.lng]} radius={radiusKm * 1000}
                pathOptions={{ color: '#16a36a', fillOpacity: 0.06, weight: 1 }} />
      )}
    </>
  );
}
