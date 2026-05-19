(function (window) {
  function esc(value) {
    return String(value == null ? '' : value).replace(/[&<>"']/g, function (ch) {
      return ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[ch];
    });
  }

  async function render(root) {
    const guide = await window.SafeOutputApi.get('/demo/integration-guide');
    root.innerHTML = [
      '<section class="hero"><div><h1>接入说明</h1><p>只展示工作台实际用到的几类字段配置：默认规则、YAML rules、字段注解、字段 ignore 和 API ignore。</p></div></section>',
      renderCards(guide.items || [])
    ].join('');
  }

  function renderCards(items) {
    return '<div class="guide-grid">' + items.map(card).join('') + '</div>';
  }

  function card(item) {
    return [
      '<article class="panel guide-card">',
      '<div class="panel-head"><div><h2>' + esc(item.title) + '</h2><p>' + esc(item.description) + '</p></div><span class="badge warn">' + esc(item.ruleSource) + '</span></div>',
      '<div class="guide-meta">',
      '<span>业务字段</span><strong>' + esc(item.businessField) + '</strong>',
      '<span>示例接口</span><code>' + esc(item.endpoint) + '</code>',
      '<span>片段来源</span><code>' + esc(item.sourceFile) + '</code>',
      '</div>',
      '<pre class="code-block code-' + esc(language(item)) + '"><code>' + highlight(item.snippet, language(item)) + '</code></pre>',
      '</article>'
    ].join('');
  }

  function language(item) {
    return /\.ya?ml$/i.test(item.sourceFile || '') ? 'yaml' : 'java';
  }

  function highlight(snippet, lang) {
    const escaped = esc(snippet);
    if (lang === 'yaml') {
      return escaped
        .replace(/^(\s*)([A-Za-z0-9_.-]+)(:)/gm, '$1<span class="tok-key">$2</span>$3')
        .replace(/\b(GET|POST|PUT|DELETE|ADDRESS|DEFAULT|MOBILE|EMAIL|ID_CARD|BANK_CARD)\b/g, '<span class="tok-type">$1</span>');
    }
    return escaped
      .replace(/\b(public|private|final|class|static|new|return)\b/g, '<span class="tok-kw">$1</span>')
      .replace(/\b(MaskRule|MaskTypes|Arrays|String)\b/g, '<span class="tok-type">$1</span>')
      .replace(/(@[A-Za-z0-9_]+)/g, '<span class="tok-anno">$1</span>')
      .replace(/(&quot;[^&]*?&quot;)/g, '<span class="tok-str">$1</span>');
  }

  window.SafeOutputViews = window.SafeOutputViews || {};
  window.SafeOutputGuide = { renderCards: renderCards };
  window.SafeOutputViews.guide = render;
})(window);
