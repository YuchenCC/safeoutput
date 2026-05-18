(function (window) {
  function esc(value) {
    return String(value == null ? '' : value).replace(/[&<>"']/g, function (ch) {
      return ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[ch];
    });
  }
  async function render(root) {
    const guide = await window.SafeOutputApi.get('/demo/integration-guide');
    root.innerHTML = '<section class="hero"><div><h1>接入说明矩阵</h1><p>按业务字段、规则来源和可触发接口对照 YAML、注解、默认规则、ignore、Log4j2 与主动脱敏服务。</p></div></section><div class="panel"><table><thead><tr><th>接入方式</th><th>业务字段或日志 key</th><th>规则来源</th><th>示例入口</th><th></th></tr></thead><tbody>' +
      guide.items.map(function (item) {
        return '<tr><td>' + esc(item.title) + '</td><td>' + esc(item.businessField) + '</td><td><span class="badge warn">' + esc(item.ruleSource) + '</span></td><td><code>' + esc(item.endpoint) + '</code></td><td><a class="badge ok" href="' + esc(item.actionHash) + '">打开</a></td></tr>';
      }).join('') + '</tbody></table></div><div class="panel"><h2>YAML 片段</h2><pre>safe-output:\n  rules:\n    - name: demoRealName\n      keys: [realName]\n      type: CHINESE_NAME\n    - name: demoAddress\n      keys: [shippingAddress]\n      type: ADDRESS\n  ignore:\n    keys: [plainNote]\n  log:\n    regex-fallback:\n      enabled: true</pre></div>';
  }
  window.SafeOutputViews = window.SafeOutputViews || {};
  window.SafeOutputViews.guide = render;
})(window);
