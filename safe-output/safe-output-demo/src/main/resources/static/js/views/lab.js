(function (window) {
  function esc(value) {
    return String(value == null ? '' : value).replace(/[&<>"']/g, function (ch) {
      return ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[ch];
    });
  }
  async function render(root) {
    root.innerHTML = '<section class="hero"><div><h1>主动脱敏实验室</h1><p>用 SafeOutputMaskService 验证按类型、业务对象、强文本扫描和批量执行耗时；输入只参与当前请求，不写入报告。</p></div></section>' +
      '<div class="grid three">' +
      labPanel('by-type', '按类型标签', '<input id="by-type-value" value="13800138000"><select id="by-type-type"><option>MOBILE</option><option>EMAIL</option><option>ID_CARD</option><option>DEFAULT</option><option>mobileM</option></select><input id="by-type-iterations" type="number" min="1" max="1000" value="3">', '执行') +
      labPanel('object', '业务对象', '<input id="object-iterations" type="number" min="1" max="1000" value="2">', '执行') +
      labPanel('strong', '强文本扫描', '<textarea id="strong-text">联系 13800138000 foo@example.com</textarea><input id="strong-iterations" type="number" min="1" max="1000" value="2">', '执行') +
      '</div><div class="panel"><h2>MANUAL 统计</h2><button id="refresh-lab">刷新统计</button><pre id="manual-stats"></pre></div>';
    document.getElementById('by-type-run').onclick = runByType;
    document.getElementById('object-run').onclick = runObject;
    document.getElementById('strong-run').onclick = runStrong;
    document.getElementById('refresh-lab').onclick = loadStats;
    await loadStats();
  }
  function labPanel(id, title, controls, label) {
    return '<div class="panel"><h2>' + title + '</h2><div class="grid">' + controls + '<button class="primary" id="' + id + '-run">' + label + '</button><pre class="result" id="' + id + '-result"></pre></div></div>';
  }
  async function runByType() {
    const data = await window.SafeOutputApi.post('/demo/mask/by-type', {
      value: document.getElementById('by-type-value').value,
      type: document.getElementById('by-type-type').value,
      iterations: Number(document.getElementById('by-type-iterations').value)
    });
    show('by-type-result', data);
    await loadStats();
  }
  async function runObject() {
    const data = await window.SafeOutputApi.post('/demo/mask/object', {
      iterations: Number(document.getElementById('object-iterations').value)
    });
    show('object-result', data);
    await loadStats();
  }
  async function runStrong() {
    const data = await window.SafeOutputApi.post('/demo/mask/strong', {
      text: document.getElementById('strong-text').value,
      iterations: Number(document.getElementById('strong-iterations').value)
    });
    show('strong-result', data);
    await loadStats();
  }
  async function loadStats() {
    const stats = await window.SafeOutputApi.get('/demo/report/dashboard');
    document.getElementById('manual-stats').textContent = JSON.stringify({
      manualCount: stats.manualCount,
      averageElapsedNanos: stats.averageElapsedNanos,
      maskTypeCounts: stats.maskTypeCounts
    }, null, 2);
  }
  function show(id, data) {
    document.getElementById(id).textContent = JSON.stringify(data, null, 2);
  }
  window.SafeOutputViews = window.SafeOutputViews || {};
  window.SafeOutputViews.lab = render;
})(window);
