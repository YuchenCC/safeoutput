(function (window) {
  function esc(value) {
    return String(value == null ? '' : value).replace(/[&<>"']/g, function (ch) {
      return ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[ch];
    });
  }
  async function render(root) {
    root.innerHTML = '<section class="hero"><div><h1>日志场景</h1><p>触发真实 Log4j2 logger，由 %safeOutputMsg 完成 JSON-like、key=value 和 fallback 脱敏；页面只展示模板摘要和聚合建议。</p></div></section><div id="logs-body"></div>';
    await load();
  }
  async function load() {
    const data = await window.SafeOutputApi.get('/demo/logs/scenarios');
    document.getElementById('logs-body').innerHTML = '<div class="grid auto">' + data.scenarios.map(function (item) {
      return '<div class="panel"><h2>' + esc(item.title) + '</h2><p>' + esc(item.templateSummary) + '</p><button class="primary" data-trigger="' + esc(item.triggerEndpoint) + '">触发日志</button></div>';
    }).join('') + '</div><div class="grid two"><div class="panel"><h2>规则建议</h2><pre id="log-suggestions">' + esc(JSON.stringify(data.summary.logRuleSuggestions || [], null, 2)) + '</pre></div><div class="panel"><h2>YAML 建议片段</h2><pre id="log-yaml">' + esc(data.summary.configSnippet || '') + '</pre></div></div><div class="panel metric"><span>LOG 脱敏计数</span><strong id="log-count">' + esc(data.summary.logCount || 0) + '</strong></div>';
    Array.prototype.forEach.call(document.querySelectorAll('[data-trigger]'), function (button) {
      button.onclick = async function () {
        const result = await window.SafeOutputApi.get(button.dataset.trigger);
        document.getElementById('log-count').textContent = result.summary.logCount || 0;
        document.getElementById('log-suggestions').textContent = JSON.stringify(result.summary.logRuleSuggestions || [], null, 2);
        document.getElementById('log-yaml').textContent = result.summary.configSnippet || '';
      };
    });
  }
  window.SafeOutputViews = window.SafeOutputViews || {};
  window.SafeOutputViews.logs = render;
})(window);
