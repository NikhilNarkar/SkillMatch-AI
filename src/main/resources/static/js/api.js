window.Api = (function () {
  const TOKEN_KEY = 'sm_token';
  const USER_KEY = 'sm_user';

  function saveSession(token, user) {
    localStorage.setItem(TOKEN_KEY, token);
    localStorage.setItem(USER_KEY, JSON.stringify(user || {}));
  }

  function getToken() {
    return localStorage.getItem(TOKEN_KEY);
  }

  function getUser() {
    try { return JSON.parse(localStorage.getItem(USER_KEY) || '{}'); } catch { return null; }
  }

  function isLoggedIn() { return !!getToken(); }

  function logout() {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
  }

  async function request(path, options = {}) {
    const headers = options.headers || {};
    headers['Content-Type'] = 'application/json';
    const token = getToken();
    if (token) headers['Authorization'] = `Bearer ${token}`;
    const res = await fetch(path, { ...options, headers });
    if (!res.ok) {
      let msg = `HTTP ${res.status}`;
      try { const j = await res.json(); msg = j.message || JSON.stringify(j); } catch {}
      throw new Error(msg);
    }
    const ct = res.headers.get('content-type') || '';
    if (ct.includes('application/json')) return res.json();
    return res.text();
  }

  async function login({ email, password }) {
    const data = await request('/api/v1/auth/login', {
      method: 'POST',
      body: JSON.stringify({ email, password })
    });
    if (data?.token) saveSession(data.token, data);
    return data;
  }

  async function register(payload) {
    const data = await request('/api/v1/auth/register', {
      method: 'POST',
      body: JSON.stringify(payload)
    });
    if (data?.token) saveSession(data.token, data);
    return data;
  }

  return { request, login, register, isLoggedIn, getUser, logout };
})();





