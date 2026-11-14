import { Navigate, Route, Routes } from 'react-router-dom';
import ProtectedRoute from './auth/ProtectedRoute.jsx';
import { HOME_BY_ROLE, useAuth } from './auth/useAuth.js';
import Navbar from './components/Navbar.jsx';
import AboutPage from './pages/AboutPage.jsx';
import AccountPage from './pages/AccountPage.jsx';
import AdminBusinessesPage from './pages/admin/AdminBusinessesPage.jsx';
import AdminDashboard from './pages/admin/AdminDashboard.jsx';
import AdminProductsPage from './pages/admin/AdminProductsPage.jsx';
import AdminUsersPage from './pages/admin/AdminUsersPage.jsx';
import AddProductPage from './pages/business/AddProductPage.jsx';
import BusinessDashboard from './pages/business/BusinessDashboard.jsx';
import BusinessProfilePage from './pages/business/BusinessProfilePage.jsx';
import BusinessReservationsPage from './pages/business/BusinessReservationsPage.jsx';
import DealsPage from './pages/DealsPage.jsx';
import HomePage from './pages/HomePage.jsx';
import LoginPage from './pages/LoginPage.jsx';
import MapPage from './pages/MapPage.jsx';
import NotificationsPage from './pages/NotificationsPage.jsx';
import ProductPage from './pages/ProductPage.jsx';
import RegisterPage from './pages/RegisterPage.jsx';
import ReservationsPage from './pages/ReservationsPage.jsx';

function Home() {
  const { user } = useAuth();
  return user ? <Navigate to={HOME_BY_ROLE[user.role]} replace /> : <HomePage />;
}

const buyer = ['BUYER', 'NGO'];

export default function App() {
  return (
    <>
      <Navbar />
      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/about" element={<AboutPage />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
        <Route path="/deals" element={<DealsPage />} />
        <Route path="/map" element={<MapPage />} />
        <Route path="/products/:id" element={<ProductPage />} />

        <Route path="/account" element={<ProtectedRoute><AccountPage /></ProtectedRoute>} />
        <Route path="/notifications" element={<ProtectedRoute><NotificationsPage /></ProtectedRoute>} />
        <Route path="/reservations" element={<ProtectedRoute roles={buyer}><ReservationsPage /></ProtectedRoute>} />

        <Route path="/business" element={<ProtectedRoute roles={['BUSINESS']}><BusinessDashboard /></ProtectedRoute>} />
        <Route path="/business/profile" element={<ProtectedRoute roles={['BUSINESS']}><BusinessProfilePage /></ProtectedRoute>} />
        <Route path="/business/products/new" element={<ProtectedRoute roles={['BUSINESS']}><AddProductPage /></ProtectedRoute>} />
        <Route path="/business/reservations" element={<ProtectedRoute roles={['BUSINESS']}><BusinessReservationsPage /></ProtectedRoute>} />

        <Route path="/admin" element={<ProtectedRoute roles={['ADMIN']}><AdminDashboard /></ProtectedRoute>} />
        <Route path="/admin/businesses" element={<ProtectedRoute roles={['ADMIN']}><AdminBusinessesPage /></ProtectedRoute>} />
        <Route path="/admin/users" element={<ProtectedRoute roles={['ADMIN']}><AdminUsersPage /></ProtectedRoute>} />
        <Route path="/admin/products" element={<ProtectedRoute roles={['ADMIN']}><AdminProductsPage /></ProtectedRoute>} />

        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </>
  );
}
