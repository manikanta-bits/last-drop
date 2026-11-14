import { useCallback, useMemo, useState } from 'react';
import { ToastContext } from './useToast.js';

// Small pop-up messages in the corner (price drops, new deals, confirmations).
export function ToastProvider({ children }) {
  const [toasts, setToasts] = useState([]);

  const show = useCallback((text, kind = 'info') => {
    const id = Math.random().toString(36).slice(2);
    setToasts((list) => [...list.slice(-3), { id, text, kind }]);
    setTimeout(() => setToasts((list) => list.filter((t) => t.id !== id)), 4500);
  }, []);

  const value = useMemo(() => ({ show }), [show]);

  return (
    <ToastContext.Provider value={value}>
      {children}
      <div className="toasts" aria-live="polite">
        {toasts.map((t) => (
          <div key={t.id} className={`toast toast-${t.kind}`}>{t.text}</div>
        ))}
      </div>
    </ToastContext.Provider>
  );
}
