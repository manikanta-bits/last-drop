import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api, { errorMessage, fieldErrors } from '../../api/client.js';
import FormField from '../../components/FormField.jsx';
import { useToast } from '../../components/useToast.js';
import { CATEGORIES, CATEGORY_ICON, money, titleCase } from '../../lib/format.js';

// <input type="datetime-local"> needs "YYYY-MM-DDTHH:mm" in local time
function localInputValue(date) {
  const pad = (n) => String(n).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

export default function AddProductPage() {
  const navigate = useNavigate();
  const toast = useToast();
  const [form, setForm] = useState({
    name: '', description: '', category: 'BAKERY', imageUrl: '', quantity: 5,
    originalPrice: '', startPrice: '', minPrice: '', dropAmount: '', dropIntervalMinutes: 60,
    expiresAt: localInputValue(new Date(Date.now() + 6 * 60 * 60 * 1000)),
  });
  const [errors, setErrors] = useState({});
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);

  const update = (field) => (e) => setForm({ ...form, [field]: e.target.value });

  const submit = async (e) => {
    e.preventDefault();
    setSaving(true);
    setErrors({});
    setError('');
    try {
      const body = {
        ...form,
        quantity: Number(form.quantity),
        dropIntervalMinutes: Number(form.dropIntervalMinutes),
        expiresAt: new Date(form.expiresAt).toISOString(),   // send UTC to the server
      };
      const r = await api.post('/api/products', body);
      toast.show('Auction started!', 'success');
      navigate(`/products/${r.data.id}`);
    } catch (err) {
      setError(errorMessage(err));
      setErrors(fieldErrors(err));
    } finally {
      setSaving(false);
    }
  };

  return (
    <main className="page">
      <h1>Add a product</h1>
      <form className="card" onSubmit={submit} noValidate>
        {error && <p className="alert alert-error">{error}</p>}
        <div className="two-col">
          <div>
            <FormField label="Product name" error={errors.name}><input value={form.name} onChange={update('name')} /></FormField>
            <FormField label="Description" error={errors.description}><textarea rows="3" value={form.description} onChange={update('description')} /></FormField>
            <FormField label="Category" error={errors.category}>
              <select value={form.category} onChange={update('category')}>
                {CATEGORIES.map((c) => <option key={c} value={c}>{CATEGORY_ICON[c]} {titleCase(c)}</option>)}
              </select>
            </FormField>
            <FormField label="Image link (optional, https://...)" error={errors.imageUrl}><input value={form.imageUrl} onChange={update('imageUrl')} /></FormField>
            <FormField label="Quantity" error={errors.quantity}><input type="number" min="1" value={form.quantity} onChange={update('quantity')} /></FormField>
          </div>
          <div>
            <FormField label="Original shop price (₹)" error={errors.originalPrice}><input type="number" step="0.01" value={form.originalPrice} onChange={update('originalPrice')} /></FormField>
            <FormField label="Auction starting price (₹)" error={errors.startPrice}><input type="number" step="0.01" value={form.startPrice} onChange={update('startPrice')} /></FormField>
            <FormField label="Minimum price (₹) - it never goes lower" error={errors.minPrice}><input type="number" step="0.01" value={form.minPrice} onChange={update('minPrice')} /></FormField>
            <FormField label="Drop by (₹)" error={errors.dropAmount}><input type="number" step="0.01" value={form.dropAmount} onChange={update('dropAmount')} /></FormField>
            <FormField label="Drop every" error={errors.dropIntervalMinutes}>
              <select value={form.dropIntervalMinutes} onChange={update('dropIntervalMinutes')}>
                <option value="15">15 minutes</option>
                <option value="30">30 minutes</option>
                <option value="60">1 hour</option>
                <option value="120">2 hours</option>
              </select>
            </FormField>
            <FormField label="Expires at (your local time)" error={errors.expiresAt}><input type="datetime-local" value={form.expiresAt} onChange={update('expiresAt')} /></FormField>
          </div>
        </div>
        {form.startPrice && form.minPrice && form.dropAmount && (
          <p className="alert alert-info">
            The price starts at {money(form.startPrice)} and drops by {money(form.dropAmount)} every{' '}
            {form.dropIntervalMinutes >= 60 ? `${form.dropIntervalMinutes / 60} hour(s)` : `${form.dropIntervalMinutes} minutes`}
            , never going below {money(form.minPrice)}. The server applies every drop.
          </p>
        )}
        <button className="btn btn-primary" disabled={saving}>{saving ? 'Starting...' : 'Start auction'}</button>
      </form>
    </main>
  );
}
