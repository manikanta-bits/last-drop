import { useLiveStatus } from '../lib/live.js';

// Green dot = live price updates connected; grey = reconnecting.
export default function LiveBadge() {
  const live = useLiveStatus();
  return (
    <span className={`live-badge ${live ? 'on' : 'off'}`} title={live ? 'Receiving live prices' : 'Reconnecting...'}>
      <span className="dot" /> {live ? 'Live' : 'Reconnecting'}
    </span>
  );
}
