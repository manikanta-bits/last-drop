import { useState } from 'react';
import { Link, useLocation, useNavigate, useSearchParams } from 'react-router-dom';
import { errorMessage, fieldErrors } from '../api/client.js';
import { useAuth } from '../auth/useAuth.js';
import FormField from '../components/FormField.jsx';

// One-click demo logins for judges (accounts come from database/03-sample-data.sql)
const DEMO_ACCOUNTS = [
  { label: 'Buyer', email: 'buyer@lastdrop.demo' },
  { label: 'NGO', email: 'ngo@lastdrop.demo' },
  { label: 'Business', email: 'business1@lastdrop.demo' },
  { label: 'Admin', email: 'admin@lastdrop.demo' },
];
const DEMO_PASSWORD = 'Demo@123';

export default function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [searchParams] = useSearchParams();

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState(searchParams.get('expired') ? 'Your session expired. Please log in again.' : '');
  const [errors, setErrors] = useState({});
  const [submitting, setSubmitting] = useState(false);

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError('');
    setErrors({});
    setSubmitting(true);
    try {
      await login(email, password);
      navigate(location.state?.from || '/account', { replace: true });
    } catch (err) {
      setError(errorMessage(err));
      setErrors(fieldErrors(err));
    } finally {
      setSubmitting(false);
    }
  };

  const fillDemo = (demoEmail) => {
    setEmail(demoEmail);
    setPassword(DEMO_PASSWORD);
  };

  return (
    <main className="page narrow">
      <div className="card">
        <h2>Log in</h2>
        {error && <p className="alert alert-error">{error}</p>}

        <form onSubmit={handleSubmit} noValidate>
          <FormField label="Email" error={errors.email}>
            <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} autoComplete="email" required />
          </FormField>
          <FormField label="Password" error={errors.password}>
            <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} autoComplete="current-password" required />
          </FormField>
          <button type="submit" className="btn btn-primary btn-block" disabled={submitting}>
            {submitting ? 'Logging in...' : 'Log in'}
          </button>
        </form>

        <p className="muted center">
          New here? <Link to="/register">Create an account</Link>
        </p>
      </div>

      <div className="card demo-box">
        <p className="field-label">Demo accounts (password {DEMO_PASSWORD})</p>
        <div className="demo-buttons">
          {DEMO_ACCOUNTS.map((demo) => (
            <button key={demo.email} type="button" className="btn btn-ghost" onClick={() => fillDemo(demo.email)}>
              {demo.label}
            </button>
          ))}
        </div>
      </div>
    </main>
  );
}
