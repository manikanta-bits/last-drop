import { useEffect, useRef, useState } from 'react';
import api, { errorMessage } from '../api/client.js';
import { useAuth } from '../auth/useAuth.js';

// Loads Google's sign-in script once for the whole app
let scriptPromise;
function loadGoogleScript() {
  scriptPromise ??= new Promise((resolve, reject) => {
    const script = document.createElement('script');
    script.src = 'https://accounts.google.com/gsi/client';
    script.async = true;
    script.onload = resolve;
    script.onerror = () => reject(new Error('Could not load Google sign-in'));
    document.head.appendChild(script);
  });
  return scriptPromise;
}

/**
 * The official "Sign in with Google" button.
 * The backend tells us its Google client id (/api/auth/config). If none is set, a short note is shown instead.
 * role = account type used if this Google account is new (sign-up).
 */
export default function GoogleButton({ role = 'BUYER', text = 'continue_with', onSuccess, onError }) {
  const { loginWithGoogle } = useAuth();
  const box = useRef(null);
  // Latest props kept in refs, so Google is set up only once per page
  const latest = useRef({ role, onSuccess, onError });
  const [state, setState] = useState('loading');   // loading | ready | off | failed

  useEffect(() => { latest.current = { role, onSuccess, onError }; }, [role, onSuccess, onError]);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const { data } = await api.get('/api/auth/config');
        if (!data.googleEnabled) { if (!cancelled) setState('off'); return; }
        await loadGoogleScript();
        if (cancelled) return;
        window.google.accounts.id.initialize({
          client_id: data.googleClientId,
          callback: async ({ credential }) => {
            try {
              const user = await loginWithGoogle(credential, latest.current.role);
              latest.current.onSuccess?.(user);
            } catch (err) {
              latest.current.onError?.(errorMessage(err));
            }
          },
        });
        window.google.accounts.id.renderButton(box.current, {
          theme: 'outline', size: 'large', shape: 'pill', text, width: 320,
        });
        setState('ready');
      } catch {
        if (!cancelled) setState('failed');
      }
    })();
    return () => { cancelled = true; };
  }, [loginWithGoogle, text]);

  return (
    <div className="google-area">
      <div className="divider"><span>or</span></div>
      <div ref={box} className="google-button" />
      {state === 'off' && (
        <p className="muted small center">Google sign-in becomes available when the server has a GOOGLE_CLIENT_ID.</p>
      )}
      {state === 'failed' && <p className="muted small center">Google sign-in could not load. Check your internet connection.</p>}
    </div>
  );
}
