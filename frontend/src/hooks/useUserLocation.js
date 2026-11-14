import { useCallback, useState } from 'react';

// Where the buyer is searching from. Saved in the browser so it survives refreshes.
// Default: central Hyderabad (where the demo shops are).
export const DEFAULT_LOCATION = { lat: 17.4435, lng: 78.3772, label: 'Hitech City, Hyderabad (default)' };
const KEY = 'lastdrop_location';

function load() {
  try {
    return JSON.parse(localStorage.getItem(KEY)) || DEFAULT_LOCATION;
  } catch {
    return DEFAULT_LOCATION;
  }
}

export default function useUserLocation() {
  const [location, setLocationState] = useState(load);
  const [locating, setLocating] = useState(false);
  const [error, setError] = useState('');

  const setLocation = useCallback((loc) => {
    localStorage.setItem(KEY, JSON.stringify(loc));
    setLocationState(loc);
    setError('');
  }, []);

  // Ask the browser for GPS position. If refused, the user can pick a spot on the map instead.
  const useMyLocation = useCallback(() => {
    if (!navigator.geolocation) {
      setError('Your browser does not support location. Tap the map to choose a spot.');
      return;
    }
    setLocating(true);
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        setLocating(false);
        setLocation({ lat: pos.coords.latitude, lng: pos.coords.longitude, label: 'My current location' });
      },
      () => {
        setLocating(false);
        setError('Location permission denied. Tap the map to choose your location manually.');
      },
      { enableHighAccuracy: true, timeout: 10000 },
    );
  }, [setLocation]);

  return { location, setLocation, useMyLocation, locating, error };
}
