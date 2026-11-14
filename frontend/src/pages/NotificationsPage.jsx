import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import api from '../api/client.js';
import { dateTime, titleCase } from '../lib/format.js';

const ICON = { PRICE_DROP: '📉', NEW_DEAL: '🆕', RESERVATION: '✅', ORDER_STATUS: '📦', AUCTION_EXPIRED: '⌛', ACCOUNT: '👤' };

export default function NotificationsPage() {
  const [list, setList] = useState([]);

  const load = useCallback(() => api.get('/api/notifications').then((r) => setList(r.data)), []);
  useEffect(() => { load(); }, [load]);

  const changed = () => window.dispatchEvent(new Event('notifications-changed'));
  const markRead = async (id) => { await api.put(`/api/notifications/${id}/read`); await load(); changed(); };
  const markAll = async () => { await api.put('/api/notifications/read-all'); await load(); changed(); };
  const unread = list.filter((n) => !n.read).length;

  return (
    <main className="page">
      <div className="page-head">
        <h1>Notifications</h1>
        {unread > 0 && <button className="btn btn-ghost" onClick={markAll}>Mark all read ({unread})</button>}
      </div>
      {list.length === 0 ? <div className="card empty">No notifications yet.</div> : list.map((n) => (
        <div key={n.id} className={`card row-card ${n.read ? 'read' : 'unread'}`}>
          <div>
            <span className="small muted">{ICON[n.type]} {titleCase(n.type)} · {dateTime(n.createdAt)}</span>
            <div>{n.message}</div>
            {n.productId && <Link className="small" to={`/products/${n.productId}`}>View product</Link>}
          </div>
          {!n.read && <button className="btn btn-ghost" onClick={() => markRead(n.id)}>Mark read</button>}
        </div>
      ))}
    </main>
  );
}
