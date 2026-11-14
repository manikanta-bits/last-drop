import { createContext, useContext } from 'react';

// A "context" shares data with every page without passing it down by hand.
// It holds: who is logged in, and the login / register / logout actions.
// The values are provided by <AuthProvider> (see AuthProvider.jsx).
export const AuthContext = createContext(null);

// Where each account type lands after logging in
export const HOME_BY_ROLE = { BUYER: '/deals', NGO: '/deals', BUSINESS: '/business', ADMIN: '/admin' };

// Any component can call useAuth() to get { user, loading, login, register, logout }
export function useAuth() {
  return useContext(AuthContext);
}
