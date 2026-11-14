import { useEffect } from 'react';

// A Yes / No pop-up. Press Esc or click outside to answer No.
export default function ConfirmDialog({ open, title, message, yesLabel = 'Yes', noLabel = 'No', onYes, onNo }) {
  useEffect(() => {
    if (!open) return undefined;
    const onKey = (e) => { if (e.key === 'Escape') onNo(); };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [open, onNo]);

  if (!open) return null;
  return (
    <div className="modal-backdrop" onClick={onNo}>
      <div className="modal" role="alertdialog" aria-modal="true" aria-labelledby="confirm-title" onClick={(e) => e.stopPropagation()}>
        <div className="modal-icon">👋</div>
        <h3 id="confirm-title">{title}</h3>
        {message && <p className="muted">{message}</p>}
        <div className="modal-actions">
          <button type="button" className="btn btn-ghost" onClick={onNo} autoFocus>{noLabel}</button>
          <button type="button" className="btn btn-primary" onClick={onYes}>{yesLabel}</button>
        </div>
      </div>
    </div>
  );
}
