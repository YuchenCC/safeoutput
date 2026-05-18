(function (window) {
  async function request(path, options) {
    const response = await fetch(path, Object.assign({
      headers: { 'Content-Type': 'application/json' }
    }, options || {}));
    if (!response.ok) {
      throw new Error(path + ' -> HTTP ' + response.status);
    }
    return response.json();
  }
  window.SafeOutputApi = {
    get: function (path) { return request(path); },
    post: function (path, body) {
      return request(path, { method: 'POST', body: JSON.stringify(body || {}) });
    }
  };
})(window);
