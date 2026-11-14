import { Client } from '@stomp/stompjs';
import { useEffect, useState } from 'react';

// One shared WebSocket (STOMP) connection for the whole app.
// The server pushes product changes on /topic/products and /topic/products/{id}.
// The client reconnects automatically every 3 seconds if the connection drops.

function socketUrl() {
  const api = import.meta.env.VITE_API_BASE_URL;
  if (api) return api.replace(/^http/, 'ws').replace(/\/$/, '') + '/ws';
  const protocol = window.location.protocol === 'https:' ? 'wss' : 'ws';
  return `${protocol}://${window.location.host}/ws`;   // via the Vite proxy in development
}

let connected = false;
const statusListeners = new Set();
const connectListeners = new Set();
const topics = new Map(); // topic -> { callbacks: Set, subscription }

const client = new Client({
  brokerURL: socketUrl(),
  reconnectDelay: 3000,
  heartbeatIncoming: 10000,
  heartbeatOutgoing: 10000,
  onConnect: () => {
    setConnected(true);
    // (Re)subscribe to every topic someone is listening to
    for (const [topic, entry] of topics) {
      entry.subscription = subscribeRaw(topic, entry.callbacks);
    }
    connectListeners.forEach((fn) => fn());
  },
  onWebSocketClose: () => setConnected(false),
  onStompError: () => setConnected(false),
});
client.activate();

function setConnected(value) {
  connected = value;
  statusListeners.forEach((fn) => fn(value));
}

function subscribeRaw(topic, callbacks) {
  return client.subscribe(topic, (frame) => {
    const data = JSON.parse(frame.body);
    callbacks.forEach((cb) => cb(data));
  });
}

// Listen to a topic. Returns a function that stops listening.
export function subscribe(topic, callback) {
  let entry = topics.get(topic);
  if (!entry) {
    entry = { callbacks: new Set(), subscription: null };
    topics.set(topic, entry);
    if (connected) entry.subscription = subscribeRaw(topic, entry.callbacks);
  }
  entry.callbacks.add(callback);
  return () => {
    entry.callbacks.delete(callback);
    if (entry.callbacks.size === 0) {
      entry.subscription?.unsubscribe();
      topics.delete(topic);
    }
  };
}

// Run a function every time the socket (re)connects - used to re-sync data after a drop-out.
export function onReconnect(callback) {
  connectListeners.add(callback);
  return () => connectListeners.delete(callback);
}

// React hook: true while the live connection is up.
export function useLiveStatus() {
  const [isConnected, setIsConnected] = useState(connected);
  useEffect(() => {
    statusListeners.add(setIsConnected);
    return () => statusListeners.delete(setIsConnected);
  }, []);
  return isConnected;
}
