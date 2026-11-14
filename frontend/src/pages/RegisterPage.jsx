import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { errorMessage, fieldErrors } from '../api/client.js';
import { HOME_BY_ROLE, useAuth } from '../auth/useAuth.js';
import FormField from '../components/FormField.jsx';

const ACCOUNT_TYPES = [
  { value: 'BUYER', title: 'Individual', text: 'Find discounted food near you' },
  { value: 'NGO', title: 'NGO', text: 'Rescue food for people in need' },
  { value: 'BUSINESS', title: 'Business', text: 'Sell surplus stock before it expires' },
];

export default function RegisterPage() {
  const { register } = useAuth();
  const navigate = useNavigate();

  const [form, setForm] = useState({ role: 'BUYER', fullName: '', email: '', phone: '', password: '' });
  const [error, setError] = useState('');
  const [errors, setErrors] = useState({});
  const [submitting, setSubmitting] = useState(false);

  const update = (field) => (event) => setForm({ ...form, [field]: event.target.value });

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError('');
    setErrors({});
    setSubmitting(true);
    try {
      const user = await register(form);
      navigate(user.role === 'BUSINESS' ? '/business/profile' : HOME_BY_ROLE[user.role], { replace: true });
    } catch (err) {
      setError(errorMessage(err));
      setErrors(fieldErrors(err));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <main className="page narrow">
      <div className="card">
        <h2>Create your account</h2>
        {error && <p className="alert alert-error">{error}</p>}

        <form onSubmit={handleSubmit} noValidate>
          <span className="field-label">Account type</span>
          <div className="role-picker">
            {ACCOUNT_TYPES.map((type) => (
              <label key={type.value} className={`role-option ${form.role === type.value ? 'selected' : ''}`}>
                <input type="radio" name="role" value={type.value} checked={form.role === type.value} onChange={update('role')} />
                <strong>{type.title}</strong>
                <small>{type.text}</small>
              </label>
            ))}
          </div>

          <FormField label={form.role === 'BUYER' ? 'Full name' : 'Contact person or organisation name'} error={errors.fullName}>
            <input value={form.fullName} onChange={update('fullName')} autoComplete="name" required />
          </FormField>
          <FormField label="Email" error={errors.email}>
            <input type="email" value={form.email} onChange={update('email')} autoComplete="email" required />
          </FormField>
          <FormField label="Phone (optional)" error={errors.phone}>
            <input type="tel" value={form.phone} onChange={update('phone')} autoComplete="tel" />
          </FormField>
          <FormField label="Password (8+ characters, a letter and a number)" error={errors.password}>
            <input type="password" value={form.password} onChange={update('password')} autoComplete="new-password" required />
          </FormField>

          {form.role === 'BUSINESS' && (
            <p className="alert alert-info">
              After signing up you will add your shop details. An administrator approves new shops before they can list products.
            </p>
          )}

          <button type="submit" className="btn btn-primary btn-block" disabled={submitting}>
            {submitting ? 'Creating account...' : 'Create account'}
          </button>
        </form>

        <p className="muted center">
          Already registered? <Link to="/login">Log in</Link>
        </p>
      </div>
    </main>
  );
}
