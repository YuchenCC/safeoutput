(function (window) {
  function esc(value) {
    return String(value == null ? '' : value).replace(/[&<>"']/g, function (ch) {
      return ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[ch];
    });
  }
  async function render(root) {
    root.innerHTML = '<section class="hero"><div><h1>业务系统敏感数据治理驾驶舱</h1><p>以客户、订单、支付、工单和账户场景触发真实 Response 脱敏链路，展示接入后的治理效果。</p></div></section><div class="toolbar"><button class="primary" id="refresh-workbench">刷新摘要</button></div><div id="workbench-body"></div>';
    document.getElementById('refresh-workbench').onclick = load;
    await load();
  }
  async function load() {
    const body = document.getElementById('workbench-body');
    const data = await window.SafeOutputApi.get('/demo/workbench');
    const dashboard = await window.SafeOutputApi.get('/demo/report/dashboard');
    body.innerHTML = [
      '<div class="grid three">',
      metric('业务场景', data.summary.scenarioCount),
      metric('Response 脱敏', dashboard.responseCount),
      metric('高风险接口', dashboard.highRiskApiCount),
      '</div>',
      '<div class="grid auto">',
      data.scenarios.map(card).join(''),
      '</div>',
      '<div class="panel"><h2>风险摘要</h2><div class="grid two"><div class="chart-box"><canvas id="scene-chart"></canvas></div><pre>' + esc(JSON.stringify(dashboard.topRiskApis || [], null, 2)) + '</pre></div></div>'
    ].join('');
    window.SafeOutputCharts.doughnut('scene-chart', ['Response', 'Log', 'Manual'],
      [dashboard.responseCount || 0, dashboard.logCount || 0, dashboard.manualCount || 0]);
    Array.prototype.forEach.call(document.querySelectorAll('[data-endpoint]'), function (button) {
      button.onclick = async function () {
        const target = document.getElementById('result-' + button.dataset.id);
        const result = await window.SafeOutputApi.get(button.dataset.endpoint);
        target.textContent = JSON.stringify(result, null, 2);
      };
    });
  }
  function metric(label, value) {
    return '<div class="panel metric"><span>' + esc(label) + '</span><strong>' + esc(value) + '</strong></div>';
  }
  function card(item) {
    return '<div class="panel"><h2>' + esc(item.name) + '</h2><p><span class="badge">' + esc(item.responseShape) + '</span></p><p>' + esc(item.governance) + '</p><button data-id="' + esc(item.id) + '" data-endpoint="' + esc(item.endpoint) + '">触发场景</button><pre class="result" id="result-' + esc(item.id) + '"></pre></div>';
  }
  window.SafeOutputViews = window.SafeOutputViews || {};
  window.SafeOutputViews.workbench = render;
})(window);
