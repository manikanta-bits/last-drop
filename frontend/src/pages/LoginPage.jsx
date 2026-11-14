import { useCallback, useState } from 'react';
import { Link, useLocation, useNavigate, useSearchParams } from 'react-router-dom';
import { errorMessage, fieldErrors } from '../api/client.js';
import { HOME_BY_ROLE, useAuth } from '../auth/useAuth.js';
import DemoAccounts from '../components/DemoAccounts.jsx';
import FormField from '../components/FormField.jsx';
import GoogleButton from '../components/GoogleButton.jsx';

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

  const goHome = useCallback(
    (user) => navigate(location.state?.from || HOME_BY_ROLE[user.role], { replace: true }),
    [navigate, location.state],
  );

  const doLogin = async (loginEmail, loginPassword) => {
    setError('');
    setErrors({});
    try {
      goHome(await login(loginEmail, loginPassword));
    } catch (err) {
      setError(errorMessage(err));
      setErrors(fieldErrors(err));
    }
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    setSubmitting(true);
    await doLogin(email, password);
    setSubmitting(false);
  };

  return (
    <main className="page auth-page">
      <div className="card auth-card">
        <h2>Welcome back 👋</h2>
        <p className="muted">Log in to reserve deals before the price drops further.</p>
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

        <GoogleButton text="signin_with" onSuccess={goHome} onError={setError} />

        <p className="muted center">
          New here? <Link to="/register">Create an account</Link>
        </p>
      </div>

      <DemoAccounts onFill={(e, p) => { setEmail(e); setPassword(p); }} onLogin={doLogin} />
    </main>
  );
}
