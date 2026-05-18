(function (window) {
  function esc(value) {
    return String(value == null ? '' : value).replace(/[&<>"']/g, function (ch) {
      return ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[ch];
    });
  }
  async function render(root) {
    root.innerHTML = '<section class="hero"><div><h1>报告文件中心</h1><p>导出、列出和查看安全目录内的 JSON 聚合报告。单报告视图提供浏览器打印版式。</p></div></section><div class="toolbar no-print"><button class="primary" id="export-report">导出报告</button><button id="refresh-reports">刷新列表</button><button class="report-print-actions" id="print-report">打印当前报告</button></div><div id="reports-body"></div>';
    document.getElementById('export-report').onclick = async function () { await window.SafeOutputApi.get('/demo/report/export'); await load(); };
    document.getElementById('refresh-reports').onclick = load;
    document.getElementById('print-report').onclick = function () { window.print(); };
    await load();
  }
  async function load() {
    const files = await window.SafeOutputApi.get('/demo/report/files');
    document.getElementById('reports-body').innerHTML = '<div class="panel"><h2>报告列表 <span class="badge">' + esc(files.count) + '</span></h2><table><thead><tr><th>文件</th><th>大小</th><th>修改时间</th><th></th></tr></thead><tbody>' + (files.files || []).map(function (file) {
      return '<tr><td>' + esc(file.name) + '</td><td>' + esc(file.size) + '</td><td>' + esc(new Date(file.modifiedAt).toLocaleString()) + '</td><td><button data-report="' + esc(file.name) + '">查看</button></td></tr>';
    }).join('') + '</tbody></table></div><div id="report-detail"></div>';
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
    document.getElementById('report-detail').innerHTML = '<div class="grid three">' +
      metric('总脱敏次数', report.totalCount) + metric('Response', report.responseCount) + metric('Log / Manual', (report.logCount || 0) + ' / ' + (report.manualCount || 0)) +
      '</div><div class="grid two"><div class="panel"><h2>场景分布</h2><div class="chart-box"><canvas id="report-scene-chart"></canvas></div></div><div class="panel"><h2>类型 Top</h2><div class="chart-box"><canvas id="report-type-chart"></canvas></div></div></div>' +
      '<div class="grid two"><div class="panel"><h2>高风险接口</h2><pre>' + esc(JSON.stringify(report.topRiskApis || [], null, 2)) + '</pre></div><div class="panel"><h2>Ignore 风险</h2><pre>' + esc(JSON.stringify(report.ignoredRiskApis || [], null, 2)) + '</pre></div></div>' +
      '<div class="panel"><h2>日志规则建议</h2><pre>' + esc(JSON.stringify(report.logRuleSuggestions || [], null, 2)) + '</pre></div><div class="panel"><h2>性能指标</h2><pre>' + esc(JSON.stringify({ averageElapsedNanos: report.averageElapsedNanos, maxElapsedNanos: report.maxElapsedNanos }, null, 2)) + '</pre></div>';
    window.SafeOutputCharts.doughnut('report-scene-chart', ['Response', 'Log', 'Manual'], [report.responseCount || 0, report.logCount || 0, report.manualCount || 0]);
    window.SafeOutputCharts.bars('report-type-chart', Object.keys(counts), Object.keys(counts).map(function (key) { return counts[key]; }));
  }
  function metric(label, value) {
    return '<div class="panel metric"><span>' + esc(label) + '</span><strong>' + esc(value || 0) + '</strong></div>';
  }
  window.SafeOutputViews = window.SafeOutputViews || {};
  window.SafeOutputViews.reports = render;
})(window);
