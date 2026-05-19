(function (window, document) {
  const root = document.getElementById('app');
  const routes = ['dashboard', 'workbench', 'lab', 'logs'];
  async function render() {
    const hashRoute = location.hash.replace('#', '').split('/')[0];
    if (hashRoute === 'guide' || location.hash.indexOf('#workbench/integration') === 0) {
      history.replaceState(null, '', '#workbench');
    }
    const currentHashRoute = location.hash.replace('#', '').split('/')[0];
    const route = routes.indexOf(currentHashRoute) >= 0 ? currentHashRoute : 'dashboard';
    if (!location.hash) {
      history.replaceState(null, '', '#dashboard');
    }
    Array.prototype.forEach.call(document.querySelectorAll('[data-route]'), function (link) {
      const sameRoute = link.dataset.route === route;
      const exactHash = link.getAttribute('href') === location.hash;
      const routeRoot = route !== 'workbench' && sameRoute;
      link.classList.toggle('active', routeRoot || (sameRoute && exactHash));
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
