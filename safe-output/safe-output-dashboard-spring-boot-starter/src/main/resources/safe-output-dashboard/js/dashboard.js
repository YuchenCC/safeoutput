(function (window, document) {
  const root = document.getElementById('app');
  const state = {
    activeTab: 'realtime',
    files: [],
    selectedReport: ''
  };

  function esc(value) {
    return String(value == null ? '' : value).replace(/[&<>"']/g, function (ch) {
      return ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[ch];
    });
  }

  async function render() {
    root.innerHTML = [
      '<section class="hero dashboard-hero"><div><h1>治理 Dashboard</h1><p>区分当前进程内存聚合的实时数据与已导出 JSON 报告快照，所有分析都基于真实聚合字段拆解。</p></div></section>',
      '<div class="toolbar no-print"><button id="refresh-dashboard">刷新当前视图</button><button class="report-print-actions" id="print-report">打印历史报告</button></div>',
      '<div class="dashboard-tabs no-print">',
      tabButton('realtime', '实时数据', '内存聚合快照'),
      tabButton('history', '历史报告', '导出文件快照'),
      '</div>',
      '<div id="dashboard-body"></div>'
    ].join('');
    document.getElementById('refresh-dashboard').onclick = load;
    document.getElementById('print-report').onclick = function () { window.print(); };
    bindTabs();
    await load();
  }

  async function load() {
    syncTabs();
    if (state.activeTab === 'history') {
      await renderHistory();
      return;
    }
    await renderRealtime();
  }

  async function renderRealtime() {
    const dashboard = await window.SafeOutputDashboardApi.post('/overview', {});
    const counts = dashboard.maskTypeCounts || {};
    document.getElementById('print-report').disabled = true;
    document.getElementById('dashboard-body').innerHTML = [
      '<section class="dashboard-section-head"><span class="badge medium">实时数据</span><h2>当前进程内存快照</h2><p>来自 MaskMetricsCollector 当前内存聚合，不读取历史文件，不包含敏感原文。</p></section>',
      '<div class="grid dashboard-metrics">',
      metric('总脱敏次数', dashboard.totalCount, 'Response / Log / Manual 聚合命中'),
      metric('覆盖接口', dashboard.apiCount, '已进入 Response 风险画像的接口数'),
      metric('脱敏类型', Object.keys(counts).length, '当前快照命中的类型数'),
      metric('日志建议', dashboard.suggestionCount, '未配置 key 的聚合线索'),
      '</div>',
      '<div class="grid dashboard-metrics">',
      metric('平均耗时', window.SafeOutputFormat.nanosToMs(dashboard.averageElapsedNanos), '单次脱敏平均成本'),
      metric('最大耗时', window.SafeOutputFormat.nanosToMs(dashboard.maxElapsedNanos), '当前内存峰值'),
      metric('Ignore 接口', dashboard.ignoredApiCount, '明文豁免但仍进入统计'),
      metric('失败次数', dashboard.failureCount, 'fail-open 事件聚合'),
      '</div>',
      '<div class="grid two">',
      '<div class="panel"><h2>实时场景分布</h2><div class="chart-box"><canvas id="live-scene-chart"></canvas></div></div>',
      '<div class="panel"><h2>敏感类型 Top</h2><div class="chart-box"><canvas id="live-type-chart"></canvas></div></div>',
      '</div>',
      '<div class="panel api-stat-panel"><div class="panel-head"><div><h2>API 脱敏统计</h2><p>按脱敏字段数和调用次数排序，展示接口维度的聚合命中情况。</p></div></div>' + riskTable(dashboard.topRiskApis || []) + '</div>',
      '<div class="grid two dashboard-action-grid">',
      '<div class="panel"><div class="panel-head"><div><h2>日志规则建议</h2><p>来自日志 fallback 线索聚合，候选规则默认人工复核。</p></div></div>' + suggestionTable(dashboard.logRuleSuggestions || []) + '</div>',
      '<div class="panel"><h2>明文豁免接口</h2>' + riskTable(dashboard.ignoredRiskApis || []) + '</div>',
      '</div>',
      '<div class="panel dashboard-follow-panel"><h2>性能与异常拆解</h2>' + healthBreakdown(dashboard) + '</div>',
    ].join('');
    window.SafeOutputCharts.doughnut('live-scene-chart', ['Response', 'Log', 'Manual'],
      [dashboard.responseCount || 0, dashboard.logCount || 0, dashboard.manualCount || 0]);
    window.SafeOutputCharts.bars('live-type-chart', Object.keys(counts), Object.keys(counts).map(function (key) { return counts[key]; }));
  }

  async function renderHistory() {
    const files = await window.SafeOutputDashboardApi.post('/reports/list', {});
    state.files = files.files || [];
    if (!state.selectedReport && state.files.length) {
      state.selectedReport = state.files[0].name;
    }
    if (state.selectedReport && !containsReport(state.selectedReport)) {
      state.selectedReport = state.files.length ? state.files[0].name : '';
    }
    document.getElementById('print-report').disabled = !state.selectedReport;
    document.getElementById('dashboard-body').innerHTML = [
      '<section class="dashboard-section-head"><span class="badge">历史报告</span><h2>报告文件快照</h2><p>选择已导出的 JSON 报告后，页面只基于该文件中的聚合字段拆解展示。</p></section>',
      '<div class="toolbar no-print"><button class="primary" id="export-report">导出报告</button><span class="badge">' + esc(files.count || 0) + ' 份报告</span></div>',
      '<div class="history-layout">',
      '<div class="panel report-picker-panel"><div class="panel-head"><div><h2>报告文件</h2><p>从已导出的报告快照中选择一份查看聚合明细。</p></div></div>' + reportFileSelect(state.files) + '</div>',
      '<div id="report-detail"></div>',
      '</div>'
    ].join('');
    document.getElementById('export-report').onclick = async function () {
      await window.SafeOutputDashboardApi.post('/reports/export', {});
      state.selectedReport = '';
      await renderHistory();
    };
    bindReportSelect();
    if (state.selectedReport) {
      await showReport(state.selectedReport);
    } else {
      document.getElementById('report-detail').innerHTML = '<div class="panel"><div class="empty-note">当前暂无历史报告。点击“导出报告”后，这里会展示报告快照拆解。</div></div>';
    }
  }

  async function showReport(name) {
    state.selectedReport = name;
    markSelectedReportSelect();
    const report = await window.SafeOutputDashboardApi.post('/reports/view', { filename: name });
    const counts = report.maskTypeCounts || {};
    document.getElementById('print-report').disabled = false;
    document.getElementById('report-detail').innerHTML = [
      '<div class="report-focus"><div class="panel-head"><div><h2>报告明细</h2><p>' + esc(report.filename || name) + '</p></div><span class="badge">报告快照</span></div>',
      '<div class="grid dashboard-metrics">',
      metric('总脱敏次数', report.totalCount, '该报告快照中的总命中'),
      metric('Response', report.responseCount, '接口响应脱敏命中'),
      metric('Log / Manual', (report.logCount || 0) + ' / ' + (report.manualCount || 0), '日志与主动脱敏命中'),
      metric('最大耗时', window.SafeOutputFormat.nanosToMs(report.maxElapsedNanos), '报告记录的单次峰值'),
      '</div>',
      '<div class="grid two"><div class="panel"><h2>报告场景分布</h2><div class="chart-box"><canvas id="report-scene-chart"></canvas></div></div><div class="panel"><h2>报告类型 Top</h2><div class="chart-box"><canvas id="report-type-chart"></canvas></div></div></div>',
      '<div class="panel api-stat-panel"><h2>API 脱敏统计</h2>' + riskTable(report.topRiskApis || []) + '</div>',
      '<div class="grid two"><div class="panel"><h2>明文豁免接口</h2>' + riskTable(report.ignoredRiskApis || []) + '</div><div class="panel"><h2>日志规则建议</h2>' + suggestionTable(report.logRuleSuggestions || []) + '</div></div>',
      '<div class="panel dashboard-follow-panel"><h2>性能拆解</h2>' + healthBreakdown(report) + '</div>',
      '</div>'
    ].join('');
    window.SafeOutputCharts.doughnut('report-scene-chart', ['Response', 'Log', 'Manual'], [report.responseCount || 0, report.logCount || 0, report.manualCount || 0]);
    window.SafeOutputCharts.bars('report-type-chart', Object.keys(counts), Object.keys(counts).map(function (key) { return counts[key]; }));
  }

  function tabButton(id, label, hint) {
    return '<button class="dashboard-tab" data-dashboard-tab="' + esc(id) + '"><strong>' + esc(label) + '</strong><span>' + esc(hint) + '</span></button>';
  }

  function bindTabs() {
    Array.prototype.forEach.call(document.querySelectorAll('[data-dashboard-tab]'), function (button) {
      button.onclick = async function () {
        state.activeTab = button.dataset.dashboardTab;
        await load();
      };
    });
  }

  function syncTabs() {
    Array.prototype.forEach.call(document.querySelectorAll('[data-dashboard-tab]'), function (button) {
      button.className = 'dashboard-tab' + (button.dataset.dashboardTab === state.activeTab ? ' active' : '');
    });
  }

  function reportFileSelect(files) {
    if (!files.length) {
      return '<div class="empty-note">当前暂无报告文件。</div>';
    }
    return '<select class="report-file-select" id="report-file-select">' + files.map(function (file) {
      const selected = file.name === state.selectedReport ? ' selected' : '';
      return '<option value="' + esc(file.name) + '"' + selected + '>' +
        esc(file.name) + ' · ' + esc(file.size) + ' bytes · ' + esc(new Date(file.modifiedAt).toLocaleString()) +
        '</option>';
    }).join('') + '</select>';
  }

  function bindReportSelect() {
    const select = document.getElementById('report-file-select');
    if (!select) {
      return;
    }
    select.onchange = function () {
      showReport(select.value);
    };
  }

  function markSelectedReportSelect() {
    const select = document.getElementById('report-file-select');
    if (select) {
      select.value = state.selectedReport;
    }
  }

  function containsReport(name) {
    for (let i = 0; i < state.files.length; i++) {
      if (state.files[i].name === name) {
        return true;
      }
    }
    return false;
  }

  function metric(label, value, hint) {
    return '<div class="panel metric"><span>' + esc(label) + '</span><strong>' + esc(value == null ? 0 : value) + '</strong><small>' + esc(hint || '') + '</small></div>';
  }

  function healthBreakdown(data) {
    return '<div class="breakdown-list">' +
      breakdown('平均耗时', window.SafeOutputFormat.nanosToMs(data.averageElapsedNanos), '聚合总耗时 / 命中次数') +
      breakdown('最大耗时', window.SafeOutputFormat.nanosToMs(data.maxElapsedNanos), '当前数据源记录的单次峰值') +
      breakdown('慢接口', data.slowApiCount || 0, '超过内部慢脱敏阈值的接口数') +
      breakdown('失败次数', data.failureCount || 0, 'fail-open 事件聚合') +
      '</div>';
  }

  function breakdown(label, value, hint) {
    return '<div class="breakdown-item"><span>' + esc(label) + '</span><strong>' + esc(value == null ? 0 : value) + '</strong><small>' + esc(hint) + '</small></div>';
  }

  function riskTable(items) {
    if (!items || !items.length) {
      return '<div class="empty-note">当前暂无接口脱敏统计。访问业务工作台或导出报告后，这里会按聚合指标展示。</div>';
    }
    return '<div class="dashboard-table api-stat-table"><table><thead><tr><th>接口</th><th>调用次数</th><th>脱敏字段</th><th>脱敏类型</th><th>标签</th></tr></thead><tbody>' +
      items.slice(0, 6).map(function (item) {
        const tags = item.riskTags || item.riskReasons || [];
        const typeCounts = item.maskTypeCounts || {};
        const maskedFieldCount = item.maskedFieldCount == null ? totalCount(typeCounts) : item.maskedFieldCount;
        const hitCount = item.hitCount == null ? '-' : item.hitCount;
        return '<tr><td><strong>' + esc((item.method || 'GET') + ' ' + (item.path || '-')) + '</strong>' +
          (item.ignored ? '<div class="table-note">API ignore：' + esc(item.ignoreReason || '已豁免') + '</div>' : '') +
          '</td><td><strong>' + esc(hitCount) + '</strong></td><td><strong>' + esc(maskedFieldCount) + '</strong></td><td>' +
          typeChips(typeCounts) + '</td><td>' + tagChips(tags) + '</td></tr>';
      }).join('') +
      '</tbody></table></div>';
  }

  function suggestionTable(items) {
    if (!items || !items.length) {
      return '<div class="empty-note">当前暂无日志规则建议。继续访问业务工作台或脱敏实验室后，未配置 key 会以聚合线索出现在这里。</div>';
    }
    return '<div class="dashboard-table"><table><thead><tr><th>Key</th><th>建议类型</th><th>命中</th><th>置信度</th><th>采纳方式</th></tr></thead><tbody>' +
      items.slice(0, 6).map(function (item) {
        return '<tr><td><strong>' + esc(item.key || '-') + '</strong><div class="table-note">' + esc(item.evidence || '') + '</div></td><td>' +
          typeChip(item.suggestedType || '-') + '</td><td><strong>' + esc(item.hitCount || 0) + '</strong></td><td>' +
          confidenceBadge(item.confidence) + '</td><td>人工复核后启用</td></tr>';
      }).join('') +
      '</tbody></table></div>';
  }

  function typeChips(counts) {
    const keys = Object.keys(counts);
    if (!keys.length) {
      return '<span class="badge">无</span>';
    }
    return keys.map(function (key) {
      return typeChip(key, counts[key]);
    }).join('');
  }

  function typeChip(type, count) {
    return '<span class="type-chip">' + esc(type) + (count == null ? '' : ' · ' + esc(count)) + '</span>';
  }

  function tagChips(tags) {
    if (!tags || !tags.length) {
      return '<span class="badge">常规接口</span>';
    }
    return tags.map(function (tag) {
      return '<span class="type-chip">' + esc(tag) + '</span>';
    }).join('');
  }

  function totalCount(counts) {
    return Object.keys(counts || {}).reduce(function (sum, key) {
      return sum + Number(counts[key] || 0);
    }, 0);
  }

  function confidenceBadge(confidence) {
    const value = confidence || 'LOW';
    const tone = value === 'HIGH' ? 'danger' : (value === 'MEDIUM' ? 'warn' : 'ok');
    return '<span class="badge ' + tone + '">' + esc(value) + '</span>';
  }

  Array.prototype.forEach.call(document.querySelectorAll('[data-route]'), function (link) {
    link.classList.toggle('active', link.dataset.route === 'dashboard');
  });
  render().catch(function (error) {
    root.innerHTML = '<section class="hero"><div><h1>请求失败</h1><p>' + esc(error.message || error) + '</p></div></section>';
  });
})(window, document);
