import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import api, { errorMessage } from '../../api/client.js';
import Stat from '../../components/Stat.jsx';
import SystemInfoPanel from '../../components/SystemInfoPanel.jsx';
import { useToast } from '../../components/useToast.js';
import { money } from '../../lib/format.js';

// Platform statistics + the demo-mode switch for judges.
export default function AdminDashboard() {
  const toast = useToast();
  const [d, setD] = useState(null);

  const load = useCallback(() => api.get('/api/admin/dashboard').then((r) => setD(r.data)), []);
  useEffect(() => {
    load().catch((err) => toast.show(errorMessage(err), 'error'));
    const id = setInterval(() => load().catch(() => {}), 10000);
    return () => clearInterval(id);
  }, [load, toast]);

  const toggleDemo = async () => {
    try {
      const r = await api.put('/api/admin/demo-mode', { enabled: !d.demoMode });
      toast.show(r.data.enabled ? `Demo mode ON: prices drop every ${r.data.dropEverySeconds} seconds` : 'Real auction mode: normal intervals', 'success');
      load();
    } catch (err) {
      toast.show(errorMessage(err), 'error');
    }
  };

  if (!d) return <main className="page"><p className="muted">Loading...</p></main>;

  return (
    <main className="page wide">
      <h1>Admin dashboard</h1>

      <section className={`card demo-panel ${d.demoMode ? 'on' : ''}`}>
        <div>
          <h3>{d.demoMode ? '⚡ DEMO MODE - prices drop every 10 seconds' : 'Real auction mode'}</h3>
          <p className="muted small">
            {d.demoMode
              ? 'For demonstrations only. Every active auction drops by its drop amount every 10 seconds.'
              : 'Each auction drops at its own interval (for example every hour). Switch on demo mode to show judges falling prices within seconds.'}
          </p>
        </div>
        <button className={`btn ${d.demoMode ? 'btn-ghost' : 'btn-primary'}`} onClick={toggleDemo}>
          {d.demoMode ? 'Switch to real mode' : 'Start demo mode'}
        </button>
      </section>

      <h2>Platform</h2>
      <div className="stats">
        <Stat label="Registered users" value={d.totalUsers} note={`${d.buyers} buyers · ${d.ngos} NGOs · ${d.businessUsers} businesses`} />
        <Stat label="Approved businesses" value={d.approvedBusinesses} />
        <Stat label="Waiting for approval" value={d.pendingBusinesses} tone={d.pendingBusinesses ? 'warning' : undefined} />
        <Stat label="Listed products" value={d.totalProducts} />
        <Stat label="Active auctions" value={d.activeAuctions} tone="green" />
        <Stat label="Sold out / Expired" value={`${d.soldOut} / ${d.expired}`} />
        <Stat label="Active reservations" value={d.activeReservations} />
        <Stat label="Completed pickups" value={d.completedReservations} />
      </div>

      <h2>Impact <span className="muted small">(estimates)</span></h2>
      <div className="stats">
        <Stat label="Items rescued" value={d.impact.itemsRescued} tone="green" />
        <Stat label="Estimated buyer savings" value={money(d.estimatedBuyerSavings)} />
        <Stat label="Estimated food waste prevented" value={`${d.impact.estimatedFoodKg} kg`} />
        <Stat label="Estimated CO₂e avoided" value={`${d.impact.estimatedCo2Kg} kg`} />
      </div>
      <p className="muted small">{d.impact.assumptions}</p>

      <SystemInfoPanel />

      <div className="admin-links">
        <Link className="card" to="/admin/businesses">🏪 Business approvals {d.pendingBusinesses > 0 && <span className="bell-count">{d.pendingBusinesses}</span>}</Link>
        <Link className="card" to="/admin/users">👥 User management</Link>
        <Link className="card" to="/admin/products">📦 Products &amp; auctions</Link>
      </div>
    </main>
  );
}
