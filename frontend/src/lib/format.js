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
export const CATEGORY_IMAGE = {
  BAKERY: 'https://images.unsplash.com/photo-1509440159596-0249088772ff?auto=format&fit=crop&w=900&q=85',
  DAIRY: 'https://images.unsplash.com/photo-1550583724-b2692b85b150?auto=format&fit=crop&w=900&q=85',
  PRODUCE: 'https://images.unsplash.com/photo-1540420773420-3366772f4999?auto=format&fit=crop&w=900&q=85',
  MEALS: 'https://images.unsplash.com/photo-1547592180-85f173990554?auto=format&fit=crop&w=900&q=85',
  GROCERY: 'https://images.unsplash.com/photo-1542838132-92c53300491e?auto=format&fit=crop&w=900&q=85',
  BEVERAGES: 'https://images.unsplash.com/photo-1544145945-f90425340c7e?auto=format&fit=crop&w=900&q=85',
};

const PRODUCT_PHOTOS = [
  [['idli'], '1589301760014-d929f3979dbc'],
  [['biryani'], '1589302168068-964664d93dc0'],
  [['paneer butter masala'], '1631452180519-c014fe946bc7'],
  [['dosa batter'], '1668236543090-82eba5ee5976'],
  [['croissant'], '1555507036-ab1f4038808a'],
  [['blueberry muffin', 'muffin'], '1607958996333-41aef7caefaa'],
  [['focaccia'], '1619452357216-e88ca8119eeb'],
  [['cinnamon roll'], '1509365465985-25d11c17e812'],
  [['red velvet'], '1586985289688-ca3cf47d3e6e'],
  [['fruit tart', 'tart'], '1488477181946-6428a0291777'],
  [['multigrain bun', 'bun'], '1558961363-fa8fdf82db35'],
  [['veg puff', 'puff'], '1601050690597-df0568f70950'],
  [['sourdough'], '1613396874083-2d5fbe59ae79'],
  [['cake', 'pastry'], '1578985545062-69928b1d9587'],
  [['spinach'], '1576045057995-568f588f82fb'],
  [['whole wheat bread'], '1534620808146-d33bb39128b2'],
  [['bread loaf'], '1509440159596-0249088772ff'],
  [['banana'], '1571771894821-ce9b6c11b08e'],
  [['fresh paneer', 'paneer ('], '1589985270826-4b7bb135bc9d'],
  [['milk'], '1563636619-e9143da7973b'],
  [['salad greens', 'mixed salad'], '1512621776951-a57141f2eefd'],
  [['yogurt'], '1570197788417-0e82375c9371'],
  [['orange juice'], '1600271886742-f049cd451bba'],
  [['tomato'], '1546094096-0df4bcaaa337'],
  [['egg'], '1506976785307-8732e854ad03'],
];

export function productImage(product) {
  const searchText = `${product.name || ''} ${product.description || ''}`.toLowerCase();
  const match = PRODUCT_PHOTOS.find(([terms]) => terms.some((term) => searchText.includes(term)));
  return match
    ? `https://images.unsplash.com/photo-${match[1]}?auto=format&fit=crop&w=900&q=85`
    : null;
}

export const BUSINESS_TYPES = ['BAKERY', 'GROCERY', 'SUPERMARKET', 'RESTAURANT', 'DISTRIBUTOR'];

export const titleCase = (s) => (s ? s.charAt(0) + s.slice(1).toLowerCase().replace(/_/g, ' ') : '');
