import { Component } from 'react';

// If any page crashes while drawing, show a friendly message instead of a blank white screen.
export default class ErrorBoundary extends Component {
  constructor(props) {
    super(props);
    this.state = { error: null };
  }

  static getDerivedStateFromError(error) {
    return { error };
  }

  componentDidCatch(error, info) {
    console.error('LASTDROP page error:', error, info?.componentStack);
  }

  render() {
    if (!this.state.error) return this.props.children;
    return (
      <main className="page narrow">
        <div className="card center">
          <h2>Something went wrong 😕</h2>
          <p className="muted">This page hit an unexpected problem. Reloading usually fixes it.</p>
          <button type="button" className="btn btn-primary" onClick={() => window.location.assign('/')}>
            Back to home
          </button>
        </div>
      </main>
    );
  }
}
