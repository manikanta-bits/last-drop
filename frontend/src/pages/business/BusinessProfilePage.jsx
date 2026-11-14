import { useEffect, useState } from 'react';
import { Marker } from 'react-leaflet';
import { useNavigate } from 'react-router-dom';
import api, { errorMessage, fieldErrors } from '../../api/client.js';
import FormField from '../../components/FormField.jsx';
import { BaseMap, ClickToPick, Recenter, youIcon } from '../../components/MapParts.jsx';
import { useToast } from '../../components/useToast.js';
import { DEFAULT_LOCATION } from '../../hooks/useUserLocation.js';
import { BUSINESS_TYPES, titleCase } from '../../lib/format.js';

// Create or edit the shop profile, including its location (tap the map).
export default function BusinessProfilePage() {
  const navigate = useNavigate();
  const toast = useToast();
  const [exists, setExists] = useState(false);
  const [form, setForm] = useState({ name: '', businessType: 'BAKERY', address: '', phone: '', latitude: '', longitude: '' });
  const [errors, setErrors] = useState({});
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    api.get('/api/businesses/my').then((r) => {
      setExists(true);
      const b = r.data;
      setForm({ name: b.name, businessType: b.businessType, address: b.address, phone: b.phone || '', latitude: b.latitude, longitude: b.longitude });
    }).catch(() => {});
  }, []);

  const update = (field) => (e) => setForm({ ...form, [field]: e.target.value });
  const setPoint = ({ lat, lng }) => setForm((f) => ({ ...f, latitude: lat.toFixed(6), longitude: lng.toFixed(6) }));
  const useGps = () => navigator.geolocation?.getCurrentPosition(
    (pos) => setPoint({ lat: pos.coords.latitude, lng: pos.coords.longitude }),
    () => toast.show('Location permission denied - tap the map instead', 'error'),
  );

  const save = async (e) => {
    e.preventDefault();
    setSaving(true);
    setErrors({});
    try {
      const body = { ...form, latitude: form.latitude === '' ? null : Number(form.latitude), longitude: form.longitude === '' ? null : Number(form.longitude) };
      if (exists) await api.put('/api/businesses/my', body);
      else await api.post('/api/businesses', body);
      toast.show(exists ? 'Shop updated' : 'Shop created - waiting for admin approval', 'success');
      navigate('/business');
    } catch (err) {
      setErrors(fieldErrors(err));
      toast.show(errorMessage(err), 'error');
    } finally {
      setSaving(false);
    }
  };

  const hasPoint = form.latitude !== '' && form.longitude !== '';
  const center = hasPoint ? { lat: Number(form.latitude), lng: Number(form.longitude) } : DEFAULT_LOCATION;

  return (
    <main className="page">
      <h1>{exists ? 'Edit shop' : 'Create your shop'}</h1>
      <form className="card" onSubmit={save} noValidate>
        <div className="two-col">
          <div>
            <FormField label="Shop name" error={errors.name}><input value={form.name} onChange={update('name')} /></FormField>
            <FormField label="Business type" error={errors.businessType}>
              <select value={form.businessType} onChange={update('businessType')}>
                {BUSINESS_TYPES.map((t) => <option key={t} value={t}>{titleCase(t)}</option>)}
              </select>
            </FormField>
            <FormField label="Full address" error={errors.address}><input value={form.address} onChange={update('address')} /></FormField>
            <FormField label="Phone" error={errors.phone}><input type="tel" value={form.phone} onChange={update('phone')} /></FormField>
          </div>
          <div>
            <span className="field-label">Location - tap the map at your shop</span>
            <BaseMap center={center} zoom={14} className="map small-map">
              <Recenter lat={center.lat} lng={center.lng} />
              <ClickToPick onPick={setPoint} />
              {hasPoint && <Marker position={[center.lat, center.lng]} icon={youIcon} />}
            </BaseMap>
            <div className="small muted">
              {hasPoint ? `Lat ${form.latitude}, Lng ${form.longitude}` : 'No location chosen yet'}{' '}
              <button type="button" className="link-button" onClick={useGps}>Use my current location</button>
            </div>
            {(errors.latitude || errors.longitude) && <span className="field-error">{errors.latitude || errors.longitude}</span>}
          </div>
        </div>
        <button className="btn btn-primary" disabled={saving}>{saving ? 'Saving...' : exists ? 'Save changes' : 'Create shop'}</button>
      </form>
    </main>
  );
}
