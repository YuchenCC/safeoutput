(function (window) {
  const modules = [
    {
      id: 'customers',
      title: '客户档案',
      noun: '客户',
      description: '客户档案用于维护客户身份、联系方式、证件、邮箱、等级和常用地址。主要敏感信息包括客户姓名、手机号、证件号、邮箱和收货地址；姓名通过字段注解 @Desensitize(CHINESE_NAME) 实现，手机号/证件号/邮箱使用内置默认字段规则，收货地址使用 safe-output.rules 中的 demoAddress 配置，备注字段通过 ignore.keys 演示字段级豁免。',
      list: '/demo/business/customers',
      detail: '/demo/business/customers/',
      key: 'customerNo',
      columns: ['customerNo', 'displayName', 'mobile', 'customerLevel', 'status'],
      sensitive: ['displayName', 'mobile', 'idCard', 'email', 'shippingAddress']
    },
    {
      id: 'orders',
      title: '订单履约',
      noun: '订单',
      description: '订单履约覆盖订单出库、运输、签收和异常拦截流程。主要敏感信息包括客户姓名、联系手机号、银行卡和收货地址；客户姓名使用字段注解声明 CHINESE_NAME，手机号和银行卡使用内置默认规则，收货地址通过 safe-output.rules 的 shippingAddress 规则配置为 ADDRESS。',
      list: '/demo/business/orders',
      detail: '/demo/business/orders/',
      key: 'orderNo',
      columns: ['orderNo', 'customerName', 'mobile', 'fulfillmentStatus', 'productSku'],
      sensitive: ['customerName', 'mobile', 'bankCard', 'shippingAddress']
    },
    {
      id: 'payments',
      title: '支付核验',
      noun: '支付',
      description: '支付核验用于展示支付流水、付款人、渠道、核验状态和安全问答。主要敏感信息包括付款人姓名、手机号、银行卡、邮箱和核验答案；付款人姓名使用字段注解，手机号/银行卡/邮箱使用内置默认规则，securityAnswer 通过 safe-output.rules 配置为 DEFAULT 兜底脱敏。',
      list: '/demo/business/payments',
      detail: '/demo/business/payments/',
      key: 'paymentNo',
      columns: ['paymentNo', 'payerName', 'mobile', 'channel', 'verifyStatus'],
      sensitive: ['payerName', 'mobile', 'bankCard', 'email', 'securityAnswer']
    },
    {
      id: 'tickets',
      title: '工单处理',
      noun: '工单',
      description: '工单处理模拟客服队列中的账号解锁、支付核验、地址修改和登录异常处理。主要敏感信息包括提交人姓名、手机号、邮箱和用户补充备注；提交人姓名使用字段注解，手机号/邮箱使用内置默认规则，plainNote 通过 ignore.keys 演示字段级不脱敏但保留治理边界。',
      list: '/demo/business/tickets',
      detail: '/demo/business/tickets/',
      key: 'ticketNo',
      columns: ['ticketNo', 'requesterName', 'mobile', 'title', 'priority'],
      sensitive: ['requesterName', 'mobile', 'email', 'plainNote']
    },
    {
      id: 'accounts',
      title: '账户安全',
      noun: '账户',
      description: '账户安全用于展示登录状态、设备变更、密码过期和风险拦截等安全运营场景。主要敏感信息包括实名姓名、手机号、邮箱、密码和地址；实名姓名使用字段注解，手机号/邮箱/密码使用内置默认规则，地址通过 safe-output.rules 的 ADDRESS 配置处理。',
      list: '/demo/business/accounts',
      detail: '/demo/business/accounts/',
      key: 'accountNo',
      columns: ['accountNo', 'realName', 'mobile', 'securityState', 'deviceId'],
      sensitive: ['realName', 'mobile', 'email', 'password', 'shippingAddress']
    }
  ];

  const labels = {
    accountNo: '账户号',
    bankCard: '银行卡',
    channel: '渠道',
    customerLevel: '客户等级',
    customerName: '客户姓名',
    customerNo: '客户号',
    deviceId: '设备',
    displayName: '客户姓名',
    email: '邮箱',
    fulfillmentStatus: '履约状态',
    idCard: '证件号',
    mobile: '手机号',
    orderNo: '订单号',
    password: '密码',
    payerName: '付款人',
    paymentNo: '支付流水',
    plainNote: '备注',
    priority: '优先级',
    productSku: '商品',
    quantity: '数量',
    requesterName: '提交人',
    securityAnswer: '核验答案',
    securityState: '安全状态',
    shippingAddress: '地址',
    status: '状态',
    ticketNo: '工单号',
    title: '标题',
    verifyStatus: '核验状态',
    warehouseMemo: '仓库备注'
  };

  function esc(value) {
    return String(value == null ? '' : value).replace(/[&<>"']/g, function (ch) {
      return ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[ch];
    });
  }

  async function render(root) {
    const active = activeModule();
    const integration = activeSection() === 'integration';
    root.innerHTML = [
      '<section class="hero workbench-hero"><div><h1>' + esc(integration ? '接入说明' : (active ? active.title : '工作台')) + '</h1><p>' + esc(integration ? integrationDescription() : (active ? active.description : overviewDescription())) + '</p></div></section>',
      '<div id="workbench-body"></div>'
    ].join('');
    if (integration) {
      await renderIntegration();
    } else if (active) {
      await renderModule(active);
    } else {
      await renderOverview();
    }
  }

  function activeModule() {
    const id = activeSection();
    if (id === 'integration') {
      return null;
    }
    for (let i = 0; i < modules.length; i++) {
      if (modules[i].id === id) {
        return modules[i];
      }
    }
    return null;
  }

  function activeSection() {
    const parts = location.hash.replace('#', '').split('/');
    return parts.length > 1 ? parts[1] : '';
  }

  async function renderIntegration() {
    const body = document.getElementById('workbench-body');
    const guide = await window.SafeOutputApi.get('/demo/integration-guide');
    body.innerHTML = window.SafeOutputGuide.renderCards(guide.items || []);
  }

  async function renderOverview() {
    const body = document.getElementById('workbench-body');
    const data = await window.SafeOutputApi.get('/demo/workbench');
    body.innerHTML = [
      '<div class="grid three">',
      metric('业务页面', modules.length),
      metric('业务域', data.summary.businessDomains),
      metric('默认入口', data.summary.primaryRoute),
      '</div>',
      '<div class="module-grid">',
      '<a class="module-card module-card-guide" href="#workbench/integration">' +
      '<span>接入</span><strong>接入说明</strong>' +
      '<p>把默认规则、YAML rules、字段注解、字段 ignore 和 API ignore 收进工作台视角，和业务页面放在同一条演示路径里。</p>' +
      '<small>/demo/integration-guide</small></a>',
      modules.map(function (item) {
        const scenario = findScenario(data.scenarios || [], item.id);
        return '<a class="module-card" href="#workbench/' + item.id + '">' +
          '<span>' + esc(item.noun) + '</span><strong>' + esc(item.title) + '</strong>' +
          '<p>' + esc(scenario ? scenario.governance : '列表、详情、明文查看') + '</p>' +
          '<small>' + esc(item.list) + '</small></a>';
      }).join(''),
      '</div>'
    ].join('');
  }

  async function renderModule(module) {
    const body = document.getElementById('workbench-body');
    body.innerHTML = '<div class="business-layout"><section class="panel business-table"><div class="panel-head"><div><h2>' + esc(module.title) + '</h2><p>' + esc(module.noun) + '列表</p></div><button class="primary" id="refresh-module">刷新</button></div><div id="module-table"></div></section><section class="panel business-detail" id="module-detail"></section></div>';
    document.getElementById('refresh-module').onclick = function () { renderModule(module); };
    const rows = await window.SafeOutputApi.get(module.list);
    renderTable(module, rows);
    if (rows && rows.length) {
      await renderDetail(module, rows[0][module.key]);
    }
  }

  function renderTable(module, rows) {
    const target = document.getElementById('module-table');
    target.innerHTML = '<table><thead><tr>' + module.columns.map(function (key) {
      return '<th>' + esc(label(key)) + '</th>';
    }).join('') + '<th>操作</th></tr></thead><tbody>' + rows.map(function (row, index) {
      return '<tr>' + module.columns.map(function (key) {
        return '<td>' + esc(row[key]) + '</td>';
      }).join('') + '<td><button class="icon-button" title="查看详情" data-id="' + esc(row[module.key]) + '">›</button></td></tr>';
    }).join('') + '</tbody></table>';
    Array.prototype.forEach.call(target.querySelectorAll('[data-id]'), function (button) {
      button.onclick = function () { renderDetail(module, button.dataset.id); };
    });
  }

  async function renderDetail(module, id) {
    const detail = await window.SafeOutputApi.get(module.detail + encodeURIComponent(id));
    const target = document.getElementById('module-detail');
    target.innerHTML = [
      '<div class="panel-head"><div><h2>' + esc(module.title) + '详情</h2><p>' + esc(id) + '</p></div>',
      '<button class="eye-button" id="reveal-sensitive" title="查看敏感信息"><span class="eye-dot"></span>查看</button></div>',
      '<div class="detail-grid">',
      Object.keys(detail).map(function (key) {
        return '<div class="detail-item ' + (module.sensitive.indexOf(key) >= 0 ? 'sensitive' : '') + '"><span>' + esc(label(key)) + '</span><strong>' + esc(detail[key]) + '</strong></div>';
      }).join(''),
      '</div>',
      '<div class="reveal-panel" id="reveal-panel"></div>'
    ].join('');
    document.getElementById('reveal-sensitive').onclick = async function () {
      const raw = await window.SafeOutputApi.get(module.detail + encodeURIComponent(id) + '/raw');
      document.getElementById('reveal-panel').innerHTML = '<h3>API ignore 明文查看</h3>' +
        '<div class="detail-grid compact">' + module.sensitive.map(function (key) {
          return '<div class="detail-item danger"><span>' + esc(label(key)) + '</span><strong>' + esc(raw[key]) + '</strong></div>';
        }).join('') + '</div>';
    };
  }

  function findScenario(items, id) {
    for (let i = 0; i < items.length; i++) {
      if (items[i].id === id) {
        return items[i];
      }
    }
    return null;
  }

  function label(key) {
    return labels[key] || key;
  }

  function overviewDescription() {
    return '工作台模拟一个包含客户档案、订单履约、支付核验、工单处理和账户安全的后台系统，并把接入说明并入同一组业务菜单。系统响应中通常包含姓名、手机号、证件号、银行卡、邮箱、地址、密码和安全问答等敏感信息；Demo 通过内置默认字段规则、YAML 配置规则、字段注解、字段 ignore 与 API ignore 组合展示业务系统接入脱敏组件后的治理需求。';
  }

  function integrationDescription() {
    return '这些接入片段只解释工作台真实用到的规则来源和代码位置，不再提供跳转入口；示例代码使用高亮显示，便于在白底后台界面中快速扫描。';
  }

  function metric(labelText, value) {
    return '<div class="panel metric"><span>' + esc(labelText) + '</span><strong>' + esc(value) + '</strong></div>';
  }

  window.SafeOutputViews = window.SafeOutputViews || {};
  window.SafeOutputViews.workbench = render;
})(window);
