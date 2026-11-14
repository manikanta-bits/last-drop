import { useCallback, useEffect, useState } from 'react';
import api, { errorMessage } from '../api/client.js';

// Admin-only: shows which database server the backend is actually using (no password),
// so it can be matched against Railway / MySQL Workbench.
export default function SystemInfoPanel() {
  const [info, setInfo] = useState(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      setInfo((await api.get('/api/admin/system')).data);
      setError('');
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  const db = info?.database;
  return (
    <section className="card system-info">
      <div className="page-head">
        <h3>🛠️ System info</h3>
        <button type="button" className="btn btn-ghost" onClick={load} disabled={loading}>{loading ? 'Checking...' : 'Refresh'}</button>
      </div>
      {error && <p className="alert alert-error">{error}</p>}
      {info && (
        <div className="two-col">
          <div>
            <p className="field-label">Database the backend writes to</p>
            <dl className="details">
              <dt>Status</dt><dd>{db.connected ? <span className="ok">Connected</span> : <span className="error">Not connected ({db.error})</span>}</dd>
              <dt>Host</dt><dd><code>{db.host}</code></dd>
              <dt>Port</dt><dd><code>{db.port}</code></dd>
              <dt>Database</dt><dd><code>{db.currentDatabase}</code></dd>
              <dt>Server</dt><dd>{db.product}</dd>
              <dt>MySQL host name</dt><dd><code>{db.serverHostname}</code></dd>
              <dt>User</dt><dd><code>{db.user}</code></dd>
            </dl>
            <p className="muted small">Compare Host + Port with Railway → MySQL → Settings → Networking (TCP Proxy).</p>
          </div>
          <div>
            <p className="field-label">Rows stored</p>
            <dl className="details">
              {Object.entries(info.rows).map(([table, count]) => (
                <span key={table} className="contents"><dt>{table}</dt><dd>{count}</dd></span>
              ))}
            </dl>
            <p className="field-label" style={{ marginTop: 14 }}>Backend</p>
            <dl className="details">
              <dt>Server time (UTC)</dt><dd>{info.application.serverTimeUtc.replace('T', ' ').slice(0, 19)}</dd>
              <dt>Running for</dt><dd>{info.application.uptime}</dd>
              <dt>Java</dt><dd>{info.application.java}</dd>
            </dl>
          </div>
        </div>
      )}
    </section>
  );
}
