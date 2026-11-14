export const RADII = [2, 5, 10, 20];

// "Searching near: ...  [Use my location]  radius [2|5|10|20 km]"
export default function LocationBar({ location, onUseMyLocation, locating, error, radius, onRadius }) {
  return (
    <div className="location-bar">
      <div>
        <span className="muted small">Searching near</span>
        <div className="location-label">📍 {location.label || `${location.lat.toFixed(4)}, ${location.lng.toFixed(4)}`}</div>
      </div>
      <div className="location-actions">
        <button type="button" className="btn btn-ghost" onClick={onUseMyLocation} disabled={locating}>
          {locating ? 'Locating...' : 'Use my location'}
        </button>
        <div className="segmented" role="group" aria-label="Search radius">
          {RADII.map((r) => (
            <button key={r} type="button" className={radius === r ? 'active' : ''} onClick={() => onRadius(r)}>
              {r} km
            </button>
          ))}
        </div>
      </div>
      {error && <p className="alert alert-error full">{error}</p>}
    </div>
  );
}
