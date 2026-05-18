(function (window) {
  function esc(value) {
    return String(value == null ? '' : value).replace(/[&<>"']/g, function (ch) {
      return ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[ch];
    });
  }

  async function render(root) {
    root.innerHTML = [
      '<section class="hero dashboard-hero"><div><h1>治理 Dashboard</h1><p>集中查看实时风险摘要、脱敏场景分布、报告导出文件和单报告治理明细。</p></div></section>',
      '<div class="toolbar no-print"><button class="primary" id="export-report">导出报告</button><button id="refresh-dashboard">刷新 Dashboard</button><button class="report-print-actions" id="print-report">打印当前报告</button></div>',
      '<div id="dashboard-body"></div>'
    ].join('');
    document.getElementById('export-report').onclick = async function () {
      await window.SafeOutputApi.get('/demo/report/export');
      await load();
    };
    document.getElementById('refresh-dashboard').onclick = load;
    document.getElementById('print-report').onclick = function () { window.print(); };
    await load();
  }

  async function load() {
    const dashboard = await window.SafeOutputApi.get('/demo/report/dashboard');
    const files = await window.SafeOutputApi.get('/demo/report/files');
    const counts = dashboard.maskTypeCounts || {};
    document.getElementById('dashboard-body').innerHTML = [
      '<div class="grid three">',
      metric('总脱敏次数', dashboard.totalCount),
      metric('Response 脱敏', dashboard.responseCount),
      metric('高风险接口', dashboard.highRiskApiCount),
      '</div>',
      '<div class="grid two">',
      '<div class="panel"><h2>实时场景分布</h2><div class="chart-box"><canvas id="live-scene-chart"></canvas></div></div>',
      '<div class="panel"><h2>实时类型分布</h2><div class="chart-box"><canvas id="live-type-chart"></canvas></div></div>',
      '</div>',
      '<div class="grid two">',
      '<div class="panel"><h2>风险摘要</h2><pre>' + esc(JSON.stringify(dashboard.topRiskApis || [], null, 2)) + '</pre></div>',
      '<div class="panel"><h2>治理信号</h2><pre>' + esc(JSON.stringify({ suggestionCount: dashboard.suggestionCount, averageElapsedNanos: dashboard.averageElapsedNanos, sceneTrend: dashboard.sceneTrend }, null, 2)) + '</pre></div>',
      '</div>',
      '<div class="panel"><h2>报告文件 <span class="badge">' + esc(files.count) + '</span></h2><table><thead><tr><th>文件</th><th>大小</th><th>修改时间</th><th></th></tr></thead><tbody>',
      (files.files || []).map(function (file) {
        return '<tr><td>' + esc(file.name) + '</td><td>' + esc(file.size) + '</td><td>' + esc(new Date(file.modifiedAt).toLocaleString()) + '</td><td><button data-report="' + esc(file.name) + '">查看</button></td></tr>';
      }).join(''),
      '</tbody></table></div><div id="report-detail"></div>'
    ].join('');
    window.SafeOutputCharts.doughnut('live-scene-chart', ['Response', 'Log', 'Manual'],
      [dashboard.responseCount || 0, dashboard.logCount || 0, dashboard.manualCount || 0]);
    window.SafeOutputCharts.bars('live-type-chart', Object.keys(counts), Object.keys(counts).map(function (key) { return counts[key]; }));
    Array.prototype.forEach.call(document.querySelectorAll('[data-report]'), function (button) {
      button.onclick = function () { showReport(button.dataset.report); };
    });
    if (files.files && files.files.length) {
      await showReport(files.files[0].name);
    }
  }

  async function showReport(name) {
    const report = await window.SafeOutputApi.get('/demo/report/files/' + encodeURIComponent(name) + '/dashboard');
    const counts = report.maskTypeCounts || {};
    document.getElementById('report-detail').innerHTML = '<div class="report-focus"><div class="panel-head"><div><h2>报告明细</h2><p>' + esc(report.filename || name) + '</p></div></div>' +
      '<div class="grid three">' +
      metric('总脱敏次数', report.totalCount) + metric('Response', report.responseCount) + metric('Log / Manual', (report.logCount || 0) + ' / ' + (report.manualCount || 0)) +
      '</div><div class="grid two"><div class="panel"><h2>报告场景分布</h2><div class="chart-box"><canvas id="report-scene-chart"></canvas></div></div><div class="panel"><h2>报告类型 Top</h2><div class="chart-box"><canvas id="report-type-chart"></canvas></div></div></div>' +
      '<div class="grid two"><div class="panel"><h2>高风险接口</h2><pre>' + esc(JSON.stringify(report.topRiskApis || [], null, 2)) + '</pre></div><div class="panel"><h2>Ignore 风险</h2><pre>' + esc(JSON.stringify(report.ignoredRiskApis || [], null, 2)) + '</pre></div></div>' +
      '<div class="panel"><h2>日志规则建议</h2><pre>' + esc(JSON.stringify(report.logRuleSuggestions || [], null, 2)) + '</pre></div><div class="panel"><h2>性能指标</h2><pre>' + esc(JSON.stringify({ averageElapsedNanos: report.averageElapsedNanos, maxElapsedNanos: report.maxElapsedNanos }, null, 2)) + '</pre></div></div>';
    window.SafeOutputCharts.doughnut('report-scene-chart', ['Response', 'Log', 'Manual'], [report.responseCount || 0, report.logCount || 0, report.manualCount || 0]);
    window.SafeOutputCharts.bars('report-type-chart', Object.keys(counts), Object.keys(counts).map(function (key) { return counts[key]; }));
  }

  function metric(label, value) {
    return '<div class="panel metric"><span>' + esc(label) + '</span><strong>' + esc(value || 0) + '</strong></div>';
  }

  window.SafeOutputViews = window.SafeOutputViews || {};
  window.SafeOutputViews.dashboard = render;
  window.SafeOutputViews.reports = render;
})(window);
