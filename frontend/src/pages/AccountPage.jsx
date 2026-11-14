import { useAuth } from '../auth/useAuth.js';

const ROLE_NAMES = { BUYER: 'Individual buyer', NGO: 'NGO', BUSINESS: 'Business', ADMIN: 'Administrator' };

export default function AccountPage() {
  const { user } = useAuth();

  return (
    <main className="page narrow">
      <div className="card">
        <h2>My account</h2>
        <dl className="details">
          <dt>Name</dt><dd>{user.fullName}</dd>
          <dt>Email</dt><dd>{user.email}</dd>
          <dt>Phone</dt><dd>{user.phone || '-'}</dd>
          <dt>Account type</dt><dd><span className="badge">{ROLE_NAMES[user.role]}</span></dd>
        </dl>
      </div>
    </main>
  );
}
