import { useState } from 'react';
import { Link } from 'react-router-dom';
import useNow from '../hooks/useNow.js';
import { CATEGORY_ICON, CATEGORY_IMAGE, clock, money, productImage, titleCase } from '../lib/format.js';

// Urgency colour by time left: under 1 hour = warning, under 15 minutes = danger
export function urgency(msLeft) {
  if (msLeft <= 15 * 60 * 1000) return 'danger';
  if (msLeft <= 60 * 60 * 1000) return 'warning';
  return 'normal';
}

export function ProductImage({ product, className = '' }) {
  const [imageIndex, setImageIndex] = useState(0);
  const images = [...new Set([product.imageUrl, productImage(product), CATEGORY_IMAGE[product.category]].filter(Boolean))];
  const image = images[imageIndex];

  if (!image) {
    return <div className={`product-img placeholder ${className}`} aria-hidden="true">{CATEGORY_ICON[product.category] || '🍽️'}</div>;
  }

  return (
    <div className={`product-photo ${className}`}>
      <img src={image} alt={product.name} loading="lazy" onError={() => {
        setImageIndex((current) => current + 1);
      }} />
      <span className="photo-category">{CATEGORY_ICON[product.category]} {titleCase(product.category)}</span>
    </div>
  );
}

// The price block. "flashAt" changes when a live price drop arrives, which replays the highlight animation.
export function LivePrice({ product, large = false }) {
  return (
    <div className={`price-block ${large ? 'large' : ''}`}>
      <span key={product.flashAt || 0} className={`current-price ${product.flashAt ? 'flash' : ''}`}>
        {money(product.currentPrice)}
      </span>
      <span className="original-price">{money(product.originalPrice)}</span>
      <span className="discount">-{product.discountPercent}%</span>
      {product.previousPrice && (
        <div className="drop-note">
          {money(product.previousPrice)} → {money(product.currentPrice)}
        </div>
      )}
    </div>
  );
}

export default function ProductCard({ product }) {
  const now = useNow();
  const msLeft = new Date(product.expiresAt).getTime() - now;
  const level = urgency(msLeft);
  const live = product.status === 'ACTIVE' && msLeft > 0 && product.quantityAvailable > 0;

  return (
    <article className={`product-card ${level === 'danger' ? 'urgent' : ''}`}>
      <Link to={`/products/${product.id}`} className="card-link">
        <ProductImage product={product} />
        <div className="product-body">
          <div className="product-top">
            <h3>{product.name}</h3>
            <span className={`status status-${product.status.toLowerCase()}`}>{titleCase(product.status)}</span>
          </div>
          <p className="muted small">
            {product.business.name}
            {product.distanceKm != null && ` · ${product.distanceKm} km`}
          </p>
          <LivePrice product={product} />
          <div className="product-meta">
            <span className={`countdown ${level}`}>⏱ {live ? `${clock(msLeft)} remaining` : 'Ended'}</span>
            <span className="muted small">{product.quantityAvailable} left</span>
          </div>
        </div>
      </Link>
      <Link to={`/products/${product.id}`} className={`btn btn-primary btn-block ${live ? '' : 'disabled'}`}>
        {live ? 'Reserve' : 'View'}
      </Link>
    </article>
  );
}
