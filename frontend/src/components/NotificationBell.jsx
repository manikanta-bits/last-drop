import { useEffect, useState } from 'react';
import { NavLink } from 'react-router-dom';
import api from '../api/client.js';

// Bell icon with unread count. Checks the server every 15 seconds.
export default function NotificationBell() {
  const [count, setCount] = useState(0);

  useEffect(() => {
    let cancelled = false;
    const load = () => api.get('/api/notifications/unread-count')
      .then((r) => { if (!cancelled) setCount(r.data.count); })
      .catch(() => {});
    load();
    const id = setInterval(load, 15000);
    window.addEventListener('notifications-changed', load);
    return () => {
      cancelled = true;
      clearInterval(id);
      window.removeEventListener('notifications-changed', load);
    };
  }, []);

  return (
    <NavLink to="/notifications" className="bell" aria-label={`Notifications, ${count} unread`}>
      🔔{count > 0 && <span className="bell-count">{count > 9 ? '9+' : count}</span>}
    </NavLink>
  );
}
