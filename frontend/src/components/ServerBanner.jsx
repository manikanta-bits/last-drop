import { useEffect, useState } from 'react';
import api from '../api/client.js';

// A strip at the top of every page when the backend can't be reached,
// so visitors understand why deals and logins are not loading.
export default function ServerBanner() {
  const [down, setDown] = useState(false);

  useEffect(() => {
    let cancelled = false;
    const check = () => api.get('/api/health')
      .then(() => { if (!cancelled) setDown(false); })
      .catch(() => { if (!cancelled) setDown(true); });
    check();
    const id = setInterval(check, 30000);
    return () => { cancelled = true; clearInterval(id); };
  }, []);

  if (!down) return null;
  return (
    <div className="server-banner" role="status">
      ⚠️ The LASTDROP server is not reachable right now, so live deals and logins are unavailable.
      {import.meta.env.PROD && !import.meta.env.VITE_API_BASE_URL && (
        <span className="small"> (Site owner: set VITE_API_BASE_URL to the backend address and redeploy.)</span>
      )}
    </div>
  );
}
