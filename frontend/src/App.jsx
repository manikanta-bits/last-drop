import { useEffect, useState } from 'react';
import api from './api/client.js';

// Phase 1 page: proves the React frontend can reach the Spring Boot backend.
export default function App() {
  const [health, setHealth] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    api.get('/api/health')
      .then((response) => setHealth(response.data))
      .catch(() => setError('Cannot reach the backend. Is it running on port 8080?'));
  }, []);

  return (
    <main className="page">
      <header className="brand">
        <h1>LASTDROP</h1>
        <p>Save Food. Reduce Waste. Discover Deals.</p>
      </header>

      <section className="card">
        <h2>Backend status</h2>
        {!health && !error && <p className="muted">Checking...</p>}
        {health && (
          <p className="ok">
            Connected: {health.service} is {health.status}
            <br />
            <span className="muted">Server time: {health.serverTime}</span>
          </p>
        )}
        {error && <p className="error">{error}</p>}
      </section>
    </main>
  );
}
