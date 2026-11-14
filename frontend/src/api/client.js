import axios from 'axios';

export const TOKEN_KEY = 'lastdrop_token';

// One shared Axios instance. Every API call in the app goes through this,
// so the backend address is configured in exactly one place (.env file).
const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
});

// Before every request: attach the login token if we have one.
api.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY);
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// After every response: if the backend says "401 Please log in" for a request
// that carried a token, the token has expired - forget it and go to the login page.
api.interceptors.response.use(
  (response) => response,
  (error) => {
    const hadToken = Boolean(error.config?.headers?.Authorization);
    if (error.response?.status === 401 && hadToken) {
      localStorage.removeItem(TOKEN_KEY);
      if (window.location.pathname !== '/login') {
        window.location.assign('/login?expired=1');
      }
    }
    return Promise.reject(error);
  },
);

// Turns any API error into a sentence we can show the user.
export function errorMessage(error) {
  if (error.response?.data?.message) return error.response.data.message;
  if (error.request && !error.response) return 'Cannot reach the server. Is the backend running?';
  return 'Something went wrong. Please try again.';
}

// Field-level messages from the backend, e.g. { email: "Enter a valid email address" }
export function fieldErrors(error) {
  return error.response?.data?.fieldErrors || {};
}

export default api;
