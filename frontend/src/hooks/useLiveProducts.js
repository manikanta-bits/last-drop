import { useCallback, useEffect, useRef, useState } from 'react';
import api from '../api/client.js';
import { onReconnect, subscribe } from '../lib/live.js';

// Fields a WebSocket message can change on a product
function merge(product, msg) {
  return {
    ...product,
    currentPrice: msg.currentPrice,
    discountPercent: msg.discountPercent,
    quantityAvailable: msg.quantityAvailable,
    status: msg.status,
    lastDropAt: msg.lastDropAt,
    nextDropAt: msg.nextDropAt,
    expiresAt: msg.expiresAt,
    version: msg.version,
    previousPrice: msg.type === 'PRICE_DROP' ? msg.oldPrice : product.previousPrice,
    flashAt: msg.type === 'PRICE_DROP' ? Date.now() : product.flashAt,
  };
}

/**
 * Loads products from the API, then keeps them up to date from WebSocket messages.
 * - Messages with a version we already have are ignored (no duplicates, no going backwards).
 * - After a reconnect the list is reloaded so nothing missed while offline is lost.
 * onPriceDrop(msg) is called for each new price drop (used for alerts).
 */
export default function useLiveProducts(url, params, { onPriceDrop, onNewProduct } = {}) {
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const callbacks = useRef({ onPriceDrop, onNewProduct });
  callbacks.current = { onPriceDrop, onNewProduct };
  const paramsKey = JSON.stringify(params || {});

  const load = useCallback(async () => {
    try {
      const response = await api.get(url, { params: JSON.parse(paramsKey) });
      setProducts(Array.isArray(response.data) ? response.data : []);
      setError('');
    } catch {
      setError('Could not load deals. Please try again.');
    } finally {
      setLoading(false);
    }
  }, [url, paramsKey]);

  useEffect(() => {
    load();
  }, [load]);

  useEffect(() => {
    const stopReconnect = onReconnect(load);
    const stop = subscribe('/topic/products', (msg) => {
      if (msg.type === 'NEW') {
        callbacks.current.onNewProduct?.(msg);
        load();
        return;
      }
      let isNewDrop = false;
      setProducts((list) => list.map((p) => {
        if (p.id !== msg.productId || msg.version <= p.version) return p;
        if (msg.type === 'PRICE_DROP') isNewDrop = true;
        return merge(p, msg);
      }));
      if (isNewDrop) callbacks.current.onPriceDrop?.(msg);
    });
    return () => {
      stop();
      stopReconnect();
    };
  }, [load]);

  return { products, loading, error, reload: load };
}

export { merge as mergeProductUpdate };
