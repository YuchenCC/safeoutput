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
        data: { labels: labels, datasets: [{ data: values, backgroundColor: '#2563eb' }] },
        options: { responsive: true, maintainAspectRatio: false, plugins: { legend: { display: false } } }
      });
    },
    doughnut: function (id, labels, values) {
      chart(id, {
        type: 'doughnut',
        data: { labels: labels, datasets: [{ data: values, backgroundColor: ['#2563eb', '#0f9f8f', '#138a52'] }] },
        options: { responsive: true, maintainAspectRatio: false }
      });
    }
  };
})(window);
