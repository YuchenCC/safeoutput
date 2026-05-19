(function (window) {
  const details = {
    'json-like': {
      label: '结构化字段识别',
      body: '来自业务详情接口的 JSON-like 日志，保留日志格式，只对 mobile/email 等字段值执行脱敏。'
    },
    'key-value': {
      label: '业务参数识别',
      body: '来自业务菜单和脱敏实验室的 key=value 审计日志，已配置 key 直接命中规则，未配置 key 进入补充建议。'
    },
    'regex-fallback': {
      label: '兜底文本识别',
      body: '漏脱敏补充提醒来自当前内存中的 fallback 线索。接口会实时过滤已配置 key，'
        + '并按命中次数标记 LOW、MEDIUM、HIGH 置信度；YAML 配置建议会为所有未配置 key '
        + '生成候选规则，但默认 enabled: false，需要人工复核后再采纳。'
    }
  };

  let currentData = null;
  let activeId = 'json-like';

  function esc(value) {
    return String(value == null ? '' : value).replace(/[&<>"']/g, function (ch) {
      return ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[ch];
    });
  }

  async function render(root) {
    root.innerHTML = [
      '<section class="hero log-hero">',
      '<div><h1>日志场景</h1><p>日志页只展示来自脱敏实验室和业务工作台接口的真实 Log4j2 聚合结果；'
        + '%safeOutputMsg 在输出侧完成 JSON-like、key=value 和 fallback 脱敏。</p></div>',
      '<div class="log-counter"><span>LOG 脱敏计数</span><strong id="log-count">0</strong><small>成功脱敏的日志值次数</small></div>',
      '</section>',
      '<div id="logs-body"></div>'
    ].join('');
    await load();
  }

  async function load() {
    currentData = await window.SafeOutputApi.get('/demo/logs/scenarios');
    const scenarios = currentData.scenarios || [];
    if (scenarios.length && !findScenario(activeId)) {
      activeId = scenarios[0].id;
    }
    document.getElementById('log-count').textContent = (currentData.summary || {}).logCount || 0;
    document.getElementById('logs-body').innerHTML = [
      '<div class="grid three log-scenario-grid">',
      scenarios.map(card).join(''),
      '</div>',
      '<div id="log-detail"></div>'
    ].join('');
    Array.prototype.forEach.call(document.querySelectorAll('[data-log-scenario]'), function (button) {
      button.onclick = function () {
        activeId = button.dataset.logScenario;
        renderCards();
        renderDetail();
      };
    });
    renderDetail();
  }

  function renderCards() {
    Array.prototype.forEach.call(document.querySelectorAll('[data-log-scenario]'), function (button) {
      if (button.dataset.logScenario === activeId) {
        button.classList.add('active');
      } else {
        button.classList.remove('active');
      }
    });
  }

  function card(item) {
    const meta = details[item.id] || {};
    return [
      '<button class="panel log-scenario-card ' + (item.id === activeId ? 'active' : '') + '" data-log-scenario="' + esc(item.id) + '">',
      '<span class="badge">' + esc(meta.label || '日志脱敏') + '</span>',
      item.id === 'regex-fallback' ? '<span class="badge warn">可收集脱敏信息</span>' : '',
      '<strong>' + esc(item.title) + '</strong>',
      '<small>' + esc(item.templateSummary) + '</small>',
      '</button>'
    ].join('');
  }

  function renderDetail() {
    const target = document.getElementById('log-detail');
    if (activeId !== 'regex-fallback') {
      target.innerHTML = '';
      return;
    }
    const scenario = findScenario(activeId) || {};
    const summary = (currentData && currentData.summary) || {};
    const suggestions = summary.logRuleSuggestions || [];
    const meta = details[activeId] || {};
    target.innerHTML = [
      '<div class="panel log-detail">',
      '<div class="panel-head"><div><h2>' + esc(scenario.title || '日志脱敏方式')
        + '</h2><p>' + esc(meta.body || scenario.templateSummary || '')
        + '</p></div><span class="badge ok">只读聚合</span></div>',
      '<div class="log-rule-note">',
      '<strong>提醒与配置建议关系</strong>',
      '<p>漏脱敏补充提醒展示的是未配置 key 的聚合线索；YAML 配置建议是同一批线索的候选配置视图。'
        + 'LOW 表示单次命中，MEDIUM 表示至少 2 次命中，HIGH 表示至少 5 次命中。'
        + '所有候选都默认关闭，不会自动改变脱敏规则。</p>',
      '</div>',
      '<div class="grid two">',
      '<div><h3>漏脱敏补充提醒</h3>' + suggestionHtml(suggestions) + '</div>',
      '<div><h3>YAML 配置建议</h3><pre id="log-yaml">' + esc(summary.configSnippet || '当前暂无可直接补充的 YAML 片段') + '</pre></div>',
      '</div>',
      '</div>'
    ].join('');
  }

  function suggestionHtml(suggestions) {
    if (!suggestions.length) {
      return '<div class="empty-note">当前暂无补充建议。继续使用业务工作台或脱敏实验室后，这里会汇总未配置 key 的脱敏线索。</div>';
    }
    return '<div class="log-suggestion-list">' + suggestions.map(function (item) {
      return [
        '<div class="log-suggestion-item">',
        '<strong>' + esc(item.key) + '</strong>',
        '<span>' + esc(item.suggestedType) + ' · ' + esc(item.confidence) + ' · ' + esc(item.hitCount) + ' hits</span>',
        '<code>' + esc(item.evidence) + '</code>',
        '</div>'
      ].join('');
    }).join('') + '</div>';
  }

  function findScenario(id) {
    const scenarios = (currentData && currentData.scenarios) || [];
    for (let i = 0; i < scenarios.length; i++) {
      if (scenarios[i].id === id) {
        return scenarios[i];
      }
    }
    return null;
  }

  window.SafeOutputViews = window.SafeOutputViews || {};
  window.SafeOutputViews.logs = render;
})(window);
