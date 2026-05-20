(function (window) {
  function basePath() {
    return window.location.pathname.replace(/\/index\.html$/, '').replace(/\/$/, '');
  }

  async function request(path, options) {
    const response = await fetch(basePath() + '/api' + path, Object.assign({
      headers: { 'Content-Type': 'application/json' }
    }, options || {}));
    if (!response.ok) {
      throw new Error(path + ' -> HTTP ' + response.status);
    }
    return response.json();
  }

  window.SafeOutputDashboardApi = {
    post: function (path, body) {
      return request(path, { method: 'POST', body: JSON.stringify(body || {}) });
    }
  };
})(window);
