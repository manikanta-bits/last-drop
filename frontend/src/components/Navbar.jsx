import { Link, NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/useAuth.js';

export default function Navbar() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/');
  };

  return (
    <nav className="navbar">
      <Link to="/" className="logo">LASTDROP</Link>
      <div className="nav-links">
        {user ? (
          <>
            <NavLink to="/account">{user.fullName}</NavLink>
            <button type="button" className="btn btn-ghost" onClick={handleLogout}>Log out</button>
          </>
        ) : (
          <>
            <NavLink to="/login">Log in</NavLink>
            <NavLink to="/register" className="btn btn-primary">Sign up</NavLink>
          </>
        )}
      </div>
    </nav>
  );
}
