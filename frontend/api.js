const API = {
  async request(path, options = {}) {
    const res = await fetch('/api' + path, {
      credentials: 'same-origin',
      headers: { 'Content-Type': 'application/json', ...(options.headers || {}) },
      ...options,
    });
    const text = await res.text();
    let data = null;
    if (text) {
      try { data = JSON.parse(text); } catch { data = text; }
    }
    if (!res.ok) {
      const err = new Error((data && data.error) || ('请求失败 HTTP ' + res.status));
      err.status = res.status;
      err.code = data && data.code;
      throw err;
    }
    return data;
  },
  get(path) { return this.request(path); },
  post(path, body) {
    return this.request(path, { method: 'POST', body: JSON.stringify(body || {}) });
  },
};
