import { useCallback, useEffect, useState } from 'react';
import api, { errorMessage } from '../../api/client.js';
import { useToast } from '../../components/useToast.js';
import { dateTime, titleCase } from '../../lib/format.js';

export default function AdminBusinessesPage() {
  const toast = useToast();
  const [list, setList] = useState([]);
  const [filter, setFilter] = useState('PENDING');

  const load = useCallback(() => api.get('/api/admin/businesses', { params: { status: filter || undefined } }).then((r) => setList(r.data)), [filter]);
  useEffect(() => { load().catch((err) => toast.show(errorMessage(err), 'error')); }, [load, toast]);

  const decide = async (id, action) => {
    try {
      await api.put(`/api/admin/businesses/${id}/${action}`);
      toast.show(action === 'approve' ? 'Business approved' : 'Business rejected', 'success');
      load();
    } catch (err) {
      toast.show(errorMessage(err), 'error');
    }
  };

  return (
    <main className="page">
      <div className="page-head">
        <h1>Business approvals</h1>
        <div className="segmented">
          {['PENDING', 'APPROVED', 'REJECTED', ''].map((s) => (
            <button key={s} className={filter === s ? 'active' : ''} onClick={() => setFilter(s)}>{s ? titleCase(s) : 'All'}</button>
          ))}
        </div>
      </div>
      {list.length === 0 ? <div className="card empty">No businesses here.</div> : list.map((b) => (
        <div key={b.id} className="card row-card">
          <div>
            <strong>{b.name}</strong> <span className="muted small">({titleCase(b.businessType)})</span>
            <div className="small">{b.address}</div>
            <div className="small muted">Owner: {b.ownerName} · {b.ownerEmail} · {b.phone || 'no phone'}</div>
            <div className="small muted">
              Registered {dateTime(b.createdAt)} ·{' '}
              <a target="_blank" rel="noreferrer" href={`https://www.openstreetmap.org/?mlat=${b.latitude}&mlon=${b.longitude}#map=17/${b.latitude}/${b.longitude}`}>check location</a>
            </div>
          </div>
          <div className="row-actions">
            <span className={`status status-${b.status.toLowerCase()}`}>{titleCase(b.status)}</span>
            {b.status !== 'APPROVED' && <button className="btn btn-primary" onClick={() => decide(b.id, 'approve')}>Approve</button>}
            {b.status !== 'REJECTED' && <button className="btn btn-ghost danger" onClick={() => decide(b.id, 'reject')}>Reject</button>}
          </div>
        </div>
      ))}
    </main>
  );
}
