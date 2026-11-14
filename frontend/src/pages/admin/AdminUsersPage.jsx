import { useCallback, useEffect, useState } from 'react';
import api, { errorMessage } from '../../api/client.js';
import { useAuth } from '../../auth/useAuth.js';
import { useToast } from '../../components/useToast.js';
import { dateTime, titleCase } from '../../lib/format.js';

export default function AdminUsersPage() {
  const toast = useToast();
  const { user: me } = useAuth();
  const [list, setList] = useState([]);
  const [q, setQ] = useState('');

  const load = useCallback(() => api.get('/api/admin/users').then((r) => setList(r.data)), []);
  useEffect(() => { load().catch((err) => toast.show(errorMessage(err), 'error')); }, [load, toast]);

  const toggle = async (u) => {
    try {
      await api.put(`/api/admin/users/${u.id}/enabled`, { enabled: !u.enabled });
      toast.show(u.enabled ? `${u.fullName} disabled` : `${u.fullName} enabled`, 'success');
      load();
    } catch (err) {
      toast.show(errorMessage(err), 'error');
    }
  };

  const shown = list.filter((u) => `${u.fullName} ${u.email} ${u.role}`.toLowerCase().includes(q.toLowerCase()));

  return (
    <main className="page wide">
      <div className="page-head">
        <h1>Users ({list.length})</h1>
        <input type="search" placeholder="Search name, email, role" value={q} onChange={(e) => setQ(e.target.value)} />
      </div>
      <div className="table-wrap card">
        <table>
          <thead><tr><th>Name</th><th>Email</th><th>Role</th><th>Joined</th><th>Status</th><th /></tr></thead>
          <tbody>
            {shown.map((u) => (
              <tr key={u.id}>
                <td>{u.fullName}</td>
                <td className="small">{u.email}</td>
                <td><span className="badge">{titleCase(u.role)}</span></td>
                <td className="small">{dateTime(u.createdAt)}</td>
                <td>{u.enabled ? <span className="ok">Active</span> : <span className="error">Disabled</span>}</td>
                <td>{u.id !== me.id && <button className={`btn btn-ghost ${u.enabled ? 'danger' : ''}`} onClick={() => toggle(u)}>{u.enabled ? 'Disable' : 'Enable'}</button>}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </main>
  );
}
