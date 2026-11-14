import { useState } from 'react';

// Demo accounts loaded by database/03-sample-data.sql. Same password for all.
export const DEMO_PASSWORD = 'Demo@123';
const DEMO_ACCOUNTS = [
  { role: 'Buyer', icon: '🛍️', email: 'buyer@lastdrop.demo', what: 'Browse deals, reserve, track savings' },
  { role: 'NGO', icon: '🤝', email: 'ngo@lastdrop.demo', what: 'Rescue food in bulk' },
  { role: 'Business', icon: '🏪', email: 'business1@lastdrop.demo', what: 'Golden Crust Bakery - list auctions' },
  { role: 'Admin', icon: '🛡️', email: 'admin@lastdrop.demo', what: 'Approvals, stats, demo mode' },
];

/** Shows every demo login. "Use" fills the form; "Log in" signs in straight away. */
export default function DemoAccounts({ onFill, onLogin }) {
  const [busy, setBusy] = useState('');
  const [copied, setCopied] = useState('');

  const copy = async (text) => {
    try {
      await navigator.clipboard.writeText(text);
      setCopied(text);
      setTimeout(() => setCopied(''), 1200);
    } catch {
      setCopied('');
    }
  };

  return (
    <section className="card demo-box">
      <div className="demo-head">
        <h3>🎟️ Demo credentials</h3>
        <span className="muted small">Password for all: <code className="pill" onClick={() => copy(DEMO_PASSWORD)}>{DEMO_PASSWORD}</code>{copied === DEMO_PASSWORD && ' copied!'}</span>
      </div>
      <ul className="demo-list">
        {DEMO_ACCOUNTS.map((d) => (
          <li key={d.email}>
            <span className="demo-icon">{d.icon}</span>
            <div className="demo-info">
              <strong>{d.role}</strong>
              <code onClick={() => copy(d.email)} title="Click to copy">{d.email}</code>
              <span className="muted small">{d.what}</span>
            </div>
            <div className="demo-actions">
              <button type="button" className="btn btn-ghost" onClick={() => onFill(d.email, DEMO_PASSWORD)}>Use</button>
              <button type="button" className="btn btn-primary" disabled={!!busy}
                      onClick={async () => { setBusy(d.email); try { await onLogin(d.email, DEMO_PASSWORD); } finally { setBusy(''); } }}>
                {busy === d.email ? '...' : 'Log in'}
              </button>
            </div>
          </li>
        ))}
      </ul>
    </section>
  );
}
