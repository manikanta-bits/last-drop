import { useState } from 'react';
import { Link, NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/useAuth.js';
import ConfirmDialog from './ConfirmDialog.jsx';
import NotificationBell from './NotificationBell.jsx';

// Links shown for each kind of account
const LINKS = {
  GUEST: [['/deals', 'Deals'], ['/map', 'Map'], ['/about', 'About']],
  BUYER: [['/deals', 'Deals'], ['/map', 'Map'], ['/reservations', 'My reservations']],
  NGO: [['/deals', 'Deals'], ['/map', 'Map'], ['/reservations', 'My reservations']],
  BUSINESS: [['/business', 'Dashboard'], ['/business/products/new', 'Add product'], ['/business/reservations', 'Orders'], ['/deals', 'Market']],
  ADMIN: [['/admin', 'Dashboard'], ['/admin/businesses', 'Approvals'], ['/admin/users', 'Users'], ['/admin/products', 'Auctions']],
};

export default function Navbar() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [open, setOpen] = useState(false);
  const [askLogout, setAskLogout] = useState(false);
  const links = LINKS[user?.role || 'GUEST'];

  const handleLogout = () => {
    setAskLogout(false);
    logout();
    setOpen(false);
    navigate('/');
  };

  return (
    <nav className="navbar">
      <Link to="/" className="logo" onClick={() => setOpen(false)}>LAST<span>DROP</span></Link>
      <button className="menu-toggle" aria-label="Menu" onClick={() => setOpen(!open)}>☰</button>
      <div className={`nav-links ${open ? 'open' : ''}`} onClick={() => setOpen(false)}>
        {links.map(([to, label]) => <NavLink key={to} to={to} end>{label}</NavLink>)}
        {user ? (
          <>
            <NotificationBell />
            <NavLink to="/account">{user.fullName}</NavLink>
            <button type="button" className="btn btn-ghost" onClick={() => setAskLogout(true)}>Log out</button>
          </>
        ) : (
          <>
            <NavLink to="/login">Log in</NavLink>
            <NavLink to="/register" className="btn btn-primary">Sign up</NavLink>
          </>
        )}
      </div>
      <ConfirmDialog
        open={askLogout}
        title="Log out of LASTDROP?"
        message="You will need to log in again to reserve deals."
        yesLabel="Yes, log out"
        noLabel="No, stay"
        onYes={handleLogout}
        onNo={() => setAskLogout(false)}
      />
    </nav>
  );
}
