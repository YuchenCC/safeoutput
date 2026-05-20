(function (window, document) {
  const root = document.getElementById('app');

  async function render() {
    const route = window.location.hash.replace('#', '') || 'overview';
    if (route === 'reports') {
      await renderReports();
    } else if (route === 'lab') {
      renderLab();
    } else if (route === 'log-suggestions') {
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

  async function renderReports() {
    const data = await window.SafeOutputDashboardApi.post('/reports/list', {});
    const files = data.files || [];
    root.innerHTML = '<section class="hero"><h1>历史报告</h1><p>报告文件名通过 POST body 传递。</p></section>'
      + '<section class="panel"><h2>报告文件</h2>' + reportList(files)
      + '<div><input type="file" id="report-upload" accept=".json"><button id="report-upload-run">上传查看</button></div>'
      + '<div id="report-detail"></div></section>';
    Array.prototype.forEach.call(document.querySelectorAll('[data-report-name]'), function (button) {
      button.onclick = function () {
        showReport(button.dataset.reportName);
      };
    });
    document.getElementById('report-upload-run').onclick = uploadReport;
  }

  function reportList(files) {
    if (!files.length) {
      return '<p>暂无历史报告。</p>';
    }
    return files.map(function (file) {
      return '<button data-report-name="' + escapeHtml(file.name) + '">' + escapeHtml(file.name) + '</button>';
    }).join('');
  }

  async function showReport(filename) {
    const report = await window.SafeOutputDashboardApi.post('/reports/view', { filename: filename });
    document.getElementById('report-detail').innerHTML = '<pre>' + escapeHtml(JSON.stringify(report, null, 2))
      + '</pre>';
  }

  async function uploadReport() {
    const input = document.getElementById('report-upload');
    if (!input.files || !input.files.length) {
      return;
    }
    const form = new FormData();
    form.append('file', input.files[0]);
    const report = await window.SafeOutputDashboardApi.postForm('/reports/upload', form);
    document.getElementById('report-detail').innerHTML = '<pre>' + escapeHtml(JSON.stringify(report, null, 2))
      + '</pre>';
  }

  function renderLab() {
    root.innerHTML = '<section class="hero"><h1>脱敏实验室</h1><p>固定两轮执行，响应不返回原始输入。</p></section>'
      + '<section class="panel"><h2>按类型标签</h2><input id="type-value" value="13800138000">'
      + '<select id="type-name"><option>MOBILE</option><option>EMAIL</option><option>ID_CARD</option></select>'
      + '<button id="type-run">执行</button><pre id="type-result"></pre></section>'
      + '<section class="panel"><h2>对象脱敏</h2><input id="object-real-name" value="张三">'
      + '<input id="object-mobile" value="13800138000"><input id="object-name" value="演示商品">'
      + '<button id="object-run">执行</button><pre id="object-result"></pre></section>'
      + '<section class="panel"><h2>强文本扫描</h2><textarea id="strong-text">联系 13800138000 foo@example.com</textarea>'
      + '<button id="strong-run">执行</button><pre id="strong-result"></pre></section>';
    document.getElementById('type-run').onclick = runByType;
    document.getElementById('object-run').onclick = runObject;
    document.getElementById('strong-run').onclick = runStrong;
  }

  async function runByType() {
    const data = await window.SafeOutputDashboardApi.post('/lab/by-type', {
      value: document.getElementById('type-value').value,
      type: document.getElementById('type-name').value
    });
    document.getElementById('type-result').textContent = JSON.stringify(data, null, 2);
  }

  async function runObject() {
    const data = await window.SafeOutputDashboardApi.post('/lab/object', {
      realName: document.getElementById('object-real-name').value,
      mobile: document.getElementById('object-mobile').value,
      name: document.getElementById('object-name').value
    });
    document.getElementById('object-result').textContent = JSON.stringify(data, null, 2);
  }

  async function runStrong() {
    const data = await window.SafeOutputDashboardApi.post('/lab/strong', {
      text: document.getElementById('strong-text').value
    });
    document.getElementById('strong-result').textContent = JSON.stringify(data, null, 2);
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
