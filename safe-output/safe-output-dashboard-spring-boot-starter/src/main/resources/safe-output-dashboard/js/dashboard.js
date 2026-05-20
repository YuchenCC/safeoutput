(function (window, document) {
  const root = document.getElementById('app');

  async function render() {
    const route = window.location.hash.replace('#', '') || 'overview';
    if (route === 'log-suggestions') {
      await renderLogSuggestions();
    } else if (route === 'risk') {
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

  async function renderLogSuggestions() {
    const data = await window.SafeOutputDashboardApi.post('/log-suggestions', {});
    const suggestions = data.logRuleSuggestions || [];
    root.innerHTML = '<section class="hero"><h1>日志规则建议</h1><p>候选规则默认关闭，人工复核后再采纳。</p></section>'
      + '<section class="panel"><h2>建议</h2>' + suggestionTable(suggestions)
      + '<pre>' + escapeHtml(data.configSnippet || '') + '</pre></section>';
  }

  function suggestionTable(items) {
    if (!items.length) {
      return '<p>暂无日志规则建议。</p>';
    }
    return '<table><thead><tr><th>Key</th><th>建议类型</th><th>置信度</th></tr></thead><tbody>'
      + items.map(function (item) {
        return '<tr><td>' + escapeHtml(item.key) + '</td><td>' + escapeHtml(item.suggestedType)
          + '</td><td>' + escapeHtml(item.confidence) + '</td></tr>';
      }).join('') + '</tbody></table>';
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
