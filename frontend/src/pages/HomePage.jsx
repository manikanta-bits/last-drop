import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import LiveBadge from '../components/LiveBadge.jsx';
import ProductCard from '../components/ProductCard.jsx';
import useLiveProducts from '../hooks/useLiveProducts.js';
import useUserLocation from '../hooks/useUserLocation.js';
import { CATEGORIES, CATEGORY_ICON, CATEGORY_IMAGE, titleCase } from '../lib/format.js';
import { useAuth } from '../auth/useAuth.js';

export default function HomePage() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const { location } = useUserLocation();
  const [query, setQuery] = useState('');
  const { products, loading, error } = useLiveProducts('/api/products', {
    lat: location.lat,
    lng: location.lng,
    radiusKm: 20,
    sort: 'discount',
  });

  const search = (event) => {
    event.preventDefault();
    navigate(`/deals${query.trim() ? `?q=${encodeURIComponent(query.trim())}` : ''}`);
  };

  return (
    <main className="storefront">
      <section className="store-hero">
        <div className="store-hero-copy">
          <span className="eyebrow"><span className="eyebrow-dot" /> Good food. Great finds. Less waste.</span>
          <h1>Something delicious is <em>around the corner.</em></h1>
          <p>Discover fresh food from local shops at feel-good prices. Every rescued bite makes a difference.</p>
          <form className="hero-search" onSubmit={search}>
            <span aria-hidden="true">⌕</span>
            <input
              type="search"
              placeholder="What are you craving today?"
              aria-label="Search food or shop"
              value={query}
              onChange={(event) => setQuery(event.target.value)}
            />
            <button type="submit">Find food <span aria-hidden="true">→</span></button>
          </form>
          <div className="hero-note">
            <span className="hero-note-icon" aria-hidden="true">✦</span>
            <span>Fresh local finds, updated live</span>
            <LiveBadge />
          </div>
          {!user && (
            <p className="hero-signup">New to LastDrop? <Link to="/register">Join the good food movement</Link></p>
          )}
        </div>
        <div className="store-hero-art" role="img" aria-label="A colorful table of fresh food">
          <div className="hero-photo" />
          <div className="hero-sticker"><span>Save up to</span><strong>60%</strong><span>on local favorites</span></div>
          <div className="hero-float-card"><span className="float-icon">🥐</span><span><strong>Today's fresh finds</strong><small>Rescued nearby, just for you</small></span><span className="float-arrow">↗</span></div>
        </div>
      </section>

      <section className="category-section">
        <div className="section-heading">
          <div><span className="eyebrow">A little something for everyone</span><h2>Shop by category</h2></div>
          <Link className="text-link" to="/deals">See all deals <span aria-hidden="true">→</span></Link>
        </div>
        <div className="category-strip">
          {CATEGORIES.map((category) => (
            <Link className="category-tile" key={category} to={`/deals?category=${category}`}>
              <span className="category-image"><img src={CATEGORY_IMAGE[category]} alt="" loading="lazy" /><span>{CATEGORY_ICON[category]}</span></span>
              <strong>{titleCase(category)}</strong>
              <span className="category-arrow" aria-hidden="true">↗</span>
            </Link>
          ))}
        </div>
      </section>

      <section className="featured-section">
        <div className="section-heading">
          <div><span className="eyebrow">Freshly added around you</span><h2>Today's good finds</h2></div>
          <Link className="text-link" to="/deals">Explore all food <span aria-hidden="true">→</span></Link>
        </div>
        {error && <p className="alert alert-error">{error}</p>}
        {loading ? <p className="muted">Finding the freshest deals near you...</p> : products.length === 0 ? (
          <div className="card empty">No live deals nearby just yet. <Link to="/deals">Explore the marketplace</Link></div>
        ) : (
          <div className="grid storefront-grid">
            {products.slice(0, 4).map((product) => <ProductCard key={product.id} product={product} />)}
          </div>
        )}
      </section>

      <section className="rescue-banner">
        <div><span className="eyebrow">Small choice. Big difference.</span><h2>Good food deserves a second chance.</h2><p>Shop your neighborhood, save on the good stuff, and help keep perfectly good food on the table.</p></div>
        <Link className="btn btn-primary" to="/deals">Browse nearby food <span aria-hidden="true">→</span></Link>
      </section>
    </main>
  );
}
