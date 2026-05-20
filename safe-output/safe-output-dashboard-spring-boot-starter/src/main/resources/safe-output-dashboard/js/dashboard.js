(function (window, document) {
  const root = document.getElementById('app');

  async function render() {
    const health = await window.SafeOutputDashboardApi.post('/health', {});
    root.innerHTML = '<section class="hero"><h1>治理 Dashboard</h1><p>当前入口：'
      + escapeHtml(health.pathPrefix) + '</p></section>';
    Array.prototype.forEach.call(document.querySelectorAll('[data-route]'), function (link) {
      link.classList.toggle('active', link.dataset.route === 'overview');
    });
  }

  function escapeHtml(value) {
    return String(value == null ? '' : value).replace(/[&<>"']/g, function (ch) {
      return ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[ch];
    });
  }

  render().catch(function (error) {
    root.innerHTML = '<section class="hero"><h1>请求失败</h1><p>' + escapeHtml(error.message) + '</p></section>';
  });
})(window, document);
