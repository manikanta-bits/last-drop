import { useEffect, useState } from 'react';

const STEPS = [
  { icon: '✓', title: 'Order placed', note: 'Your rescue is confirmed' },
  { icon: '▣', title: 'Being packed', note: 'The shop gets your order ready' },
  { icon: '⌁', title: 'Rider on the way', note: 'A rider collects your package' },
  { icon: '⌂', title: 'Delivered', note: 'Enjoy your rescued food' },
];

function storageKey(reservationId) {
  return `lastdrop_demo_journey_${reservationId}`;
}

export default function OrderJourney({ reservationId, autoPlay = false }) {
  const [stage, setStage] = useState(() => {
    const saved = Number(localStorage.getItem(storageKey(reservationId)));
    return Number.isInteger(saved) && saved >= 0 && saved < STEPS.length ? saved : 0;
  });
  const [playing, setPlaying] = useState(autoPlay);

  useEffect(() => {
    localStorage.setItem(storageKey(reservationId), String(stage));
  }, [reservationId, stage]);

  useEffect(() => {
    if (!playing || stage >= STEPS.length - 1) return undefined;
    const timer = window.setTimeout(() => setStage((current) => Math.min(current + 1, STEPS.length - 1)), 1350);
    return () => window.clearTimeout(timer);
  }, [playing, stage]);

  const play = () => {
    setStage(0);
    setPlaying(true);
  };
  const previewing = playing && stage < STEPS.length - 1;

  return (
    <section className="order-journey" aria-label="Demo delivery progress">
      <div className="journey-heading">
        <div><span className="journey-demo-tag">DEMO PREVIEW</span><h3>See the delivery journey</h3></div>
        <button type="button" className="link-button journey-play" onClick={play} disabled={previewing}>
          {previewing ? 'Previewing...' : stage > 0 ? 'Replay' : 'Play preview'}
        </button>
      </div>
      <p className="journey-disclaimer">Illustration only. This order remains pickup-only; no rider is dispatched.</p>
      <ol className="journey-steps">
        {STEPS.map((step, index) => (
          <li key={step.title} className={`${index < stage ? 'done' : ''} ${index === stage ? 'current' : ''}`}>
            <span className="journey-marker" aria-hidden="true">{index < stage ? '✓' : step.icon}</span>
            <span className="journey-step-copy"><strong>{step.title}</strong><small>{step.note}</small></span>
          </li>
        ))}
      </ol>
    </section>
  );
}
