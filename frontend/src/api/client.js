import axios from 'axios';

// One shared Axios instance. Every API call in the app goes through this,
// so the backend address is configured in exactly one place (.env file).
const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
});

export default api;
