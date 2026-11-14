// Small display helpers used across the app.

export const money = (value) =>
  value == null ? '-' : `₹${Number(value).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;

export const dateTime = (iso) =>
  iso ? new Date(iso).toLocaleString('en-IN', { dateStyle: 'medium', timeStyle: 'short' }) : '-';

// 4h 35m 20s -> "04:35:20"
export function clock(ms) {
  if (ms <= 0) return '00:00:00';
  const total = Math.floor(ms / 1000);
  const h = String(Math.floor(total / 3600)).padStart(2, '0');
  const m = String(Math.floor((total % 3600) / 60)).padStart(2, '0');
  const s = String(total % 60).padStart(2, '0');
  return `${h}:${m}:${s}`;
}

export const CATEGORIES = ['BAKERY', 'DAIRY', 'PRODUCE', 'MEALS', 'GROCERY', 'BEVERAGES'];
export const CATEGORY_ICON = { BAKERY: '🥐', DAIRY: '🥛', PRODUCE: '🥬', MEALS: '🍛', GROCERY: '🛒', BEVERAGES: '🧃' };
export const BUSINESS_TYPES = ['BAKERY', 'GROCERY', 'SUPERMARKET', 'RESTAURANT', 'DISTRIBUTOR'];

export const titleCase = (s) => (s ? s.charAt(0) + s.slice(1).toLowerCase().replace(/_/g, ' ') : '');
