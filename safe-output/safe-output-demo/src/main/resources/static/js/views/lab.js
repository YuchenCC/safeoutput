(function (window) {
  function esc(value) {
    return String(value == null ? '' : value).replace(/[&<>"']/g, function (ch) {
      return ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[ch];
    });
  }
  async function render(root) {
    root.innerHTML = '<section class="hero"><div><h1>主动脱敏实验室</h1><p>用 SafeOutputMaskService 固定执行两轮脱敏，直观看到首次结果、二次脱敏稳定性和单轮耗时；输入只参与当前请求，不写入报告。</p></div></section>' +
      '<div class="grid three">' +
      labPanel('by-type', '按类型标签', '<input id="by-type-value" value="13800138000"><select id="by-type-type"><option>MOBILE</option><option>EMAIL</option><option>ID_CARD</option><option>DEFAULT</option><option>mobileM</option></select>', '执行') +
      labPanel('object', '业务对象', '<input id="object-real-name" value="张三" placeholder="realName"><input id="object-mobile" value="13800138000" placeholder="mobile"><input id="object-name" value="演示商品" placeholder="name">', '执行') +
      labPanel('strong', '强文本扫描', '<textarea id="strong-text">联系 13800138000 foo@example.com</textarea>', '执行') +
      '</div>';
    document.getElementById('by-type-run').onclick = runByType;
    document.getElementById('object-run').onclick = runObject;
    document.getElementById('strong-run').onclick = runStrong;
  }
  function labPanel(id, title, controls, label) {
    return '<div class="panel"><h2>' + title + '</h2><div class="grid">' + controls + '<button class="primary" id="' + id + '-run">' + label + '</button><div class="result round-list" id="' + id + '-result"></div></div></div>';
  }
  async function runByType() {
    const data = await window.SafeOutputApi.post('/demo/mask/by-type', {
      value: document.getElementById('by-type-value').value,
      type: document.getElementById('by-type-type').value
    });
    show('by-type-result', data);
  }
  async function runObject() {
    const payload = objectPayload();
    const data = await window.SafeOutputApi.post('/demo/mask/object', payload);
    show('object-result', data, payload);
  }
  async function runStrong() {
    const data = await window.SafeOutputApi.post('/demo/mask/strong', {
      text: document.getElementById('strong-text').value
    });
    show('strong-result', data);
  }
  function objectPayload() {
    return {
      realName: document.getElementById('object-real-name').value,
      mobile: document.getElementById('object-mobile').value,
      name: document.getElementById('object-name').value
    };
  }
  function show(id, data, original) {
    const target = document.getElementById(id);
    if (!Array.isArray(data)) {
      target.innerHTML = '<pre>' + esc(JSON.stringify(window.SafeOutputFormat.toDisplayTiming(data), null, 2)) + '</pre>';
      return;
    }
    const originalHtml = original ? '<div class="round-card"><div class="round-meta"><strong>原始输入</strong><span class="badge warn">待脱敏</span></div><pre>' + esc(JSON.stringify(original, null, 2)) + '</pre></div>' : '';
    target.innerHTML = originalHtml + data.map(function (item) {
      return '<div class="round-card"><div class="round-meta"><strong>Round ' + esc(item.round) + '</strong><span>' + esc(window.SafeOutputFormat.nanosToMs(item.elapsedNanos)) + '</span><span class="badge ' + (item.sameAsPrevious ? 'ok' : 'warn') + '">' + (item.sameAsPrevious ? '稳定' : '首次变化') + '</span></div><pre>' + esc(JSON.stringify(item.result, null, 2)) + '</pre></div>';
    }).join('');
  }
  window.SafeOutputViews = window.SafeOutputViews || {};
  window.SafeOutputViews.lab = render;
})(window);
