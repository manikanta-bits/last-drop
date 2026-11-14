import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import api from '../api/client.js';
import { useAuth } from '../auth/useAuth.js';

export default function HomePage() {
  const { user } = useAuth();
  const [serverUp, setServerUp] = useState(null);

  useEffect(() => {
    api.get('/api/health')
      .then(() => setServerUp(true))
      .catch(() => setServerUp(false));
  }, []);

  let serverText = <span>checking...</span>;
  if (serverUp === true) serverText = <span className="ok">online</span>;
  if (serverUp === false) serverText = <span className="error">offline</span>;

  return (
    <main className="page">
      <section className="hero">
        <h1>Save Food. Reduce Waste. Discover Deals.</h1>
        <p>
          Local bakeries, grocers and restaurants list food close to its expiry time.
          The price drops every hour until someone nearby rescues it.
        </p>
        {!user && (
          <div className="hero-actions">
            <Link to="/register" className="btn btn-primary">Create an account</Link>
            <Link to="/login" className="btn btn-light">Log in</Link>
          </div>
        )}
      </section>

      <section className="steps">
        <div className="card">
          <h3>1. Shops list surplus food</h3>
          <p className="muted">They set a starting price, a minimum price and an expiry time.</p>
        </div>
        <div className="card">
          <h3>2. The price falls</h3>
          <p className="muted">A Dutch auction lowers the price automatically, live for everyone.</p>
        </div>
        <div className="card">
          <h3>3. You reserve and pick up</h3>
          <p className="muted">Reserve at the current price and pay cash when you collect.</p>
        </div>
      </section>

      <p className="server-status muted">Server: {serverText}</p>
    </main>
  );
}
