(function (window, document) {
  const root = document.getElementById('app');

  async function render() {
    const route = window.location.hash.replace('#', '') || 'overview';
    if (route === 'risk') {
      await renderRisk();
    } else {
      await renderOverview();
    }
    Array.prototype.forEach.call(document.querySelectorAll('[data-route]'), function (link) {
      link.classList.toggle('active', link.dataset.route === route);
    });
  }

  async function renderOverview() {
    const dashboard = await window.SafeOutputDashboardApi.post('/overview', {});
    root.innerHTML = '<section class="hero"><h1>实时概览</h1><p>当前进程聚合快照，不展示敏感原文。</p></section>'
      + '<section class="panel"><h2>统计</h2><p>总脱敏次数：' + escapeHtml(dashboard.totalCount)
      + '，Response：' + escapeHtml(dashboard.responseCount)
      + '，Log：' + escapeHtml(dashboard.logCount)
      + '，Manual：' + escapeHtml(dashboard.manualCount)
      + '，失败：' + escapeHtml(dashboard.failureCount) + '</p></section>';
  }

  async function renderRisk() {
    const risk = await window.SafeOutputDashboardApi.post('/response-risk', {});
    const summary = risk.responseRiskSummary || {};
    root.innerHTML = '<section class="hero"><h1>接口风险</h1><p>基于 Response 聚合指标生成。</p></section>'
      + '<section class="panel"><h2>摘要</h2><p>接口数：' + escapeHtml(summary.apiCount || 0)
      + '，高风险：' + escapeHtml(summary.highRiskApiCount || 0)
      + '，Ignore：' + escapeHtml(summary.ignoredApiCount || 0)
      + '，慢接口：' + escapeHtml(summary.slowApiCount || 0) + '</p></section>';
  }

  function escapeHtml(value) {
    return String(value == null ? '' : value).replace(/[&<>"']/g, function (ch) {
      return ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[ch];
    });
  }

  window.addEventListener('hashchange', render);
  render().catch(function (error) {
    root.innerHTML = '<section class="hero"><h1>请求失败</h1><p>' + escapeHtml(error.message) + '</p></section>';
  });
})(window, document);
