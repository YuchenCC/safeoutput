(function (window, document) {
  const root = document.getElementById('app');
  const routes = ['workbench', 'guide', 'lab', 'logs', 'reports'];
  async function render() {
    const route = routes.indexOf(location.hash.replace('#', '')) >= 0 ? location.hash.replace('#', '') : 'workbench';
    if (!location.hash) {
      history.replaceState(null, '', '#workbench');
    }
    Array.prototype.forEach.call(document.querySelectorAll('[data-route]'), function (link) {
      link.classList.toggle('active', link.dataset.route === route);
    });
    try {
      await window.SafeOutputViews[route](root);
    } catch (error) {
      root.innerHTML = '<section class="hero"><div><h1>请求失败</h1><p>' + String(error.message || error) + '</p></div></section>';
    }
  }
  window.addEventListener('hashchange', render);
  render();
})(window, document);
