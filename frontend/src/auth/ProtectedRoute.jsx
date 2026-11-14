import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from './useAuth.js';

// Wrap a page with this to require login, and optionally specific roles:
//   <ProtectedRoute roles={['ADMIN']}><AdminPage /></ProtectedRoute>
// The backend checks roles too - this only keeps people off pages they can't use.
export default function ProtectedRoute({ roles, children }) {
  const { user, loading } = useAuth();
  const location = useLocation();

  if (loading) {
    return <p className="page muted">Loading...</p>;
  }
  if (!user) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  }
  if (roles && !roles.includes(user.role)) {
    return (
      <main className="page">
        <div className="card">
          <h2>Access denied</h2>
          <p className="muted">This page is not available for your account type.</p>
        </div>
      </main>
    );
  }
  return children;
}
