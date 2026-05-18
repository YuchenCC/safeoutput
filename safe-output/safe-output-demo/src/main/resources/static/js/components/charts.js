(function (window) {
  const charts = {};
  function chart(id, config) {
    const canvas = document.getElementById(id);
    if (!canvas || !window.Chart) {
      return;
    }
    if (charts[id]) {
      charts[id].destroy();
    }
    charts[id] = new Chart(canvas, config);
  }
  window.SafeOutputCharts = {
    bars: function (id, labels, values) {
      chart(id, {
        type: 'bar',
        data: { labels: labels, datasets: [{ data: values, backgroundColor: '#e0a326' }] },
        options: { responsive: true, maintainAspectRatio: false, plugins: { legend: { display: false } } }
      });
    },
    doughnut: function (id, labels, values) {
      chart(id, {
        type: 'doughnut',
        data: { labels: labels, datasets: [{ data: values, backgroundColor: ['#e0a326', '#42b8dd', '#43c77b'] }] },
        options: { responsive: true, maintainAspectRatio: false }
      });
    }
  };
})(window);
