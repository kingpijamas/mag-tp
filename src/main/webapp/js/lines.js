var _lineCharts = null;

function _updateLineChartsData(stats) {
    _lineCharts.charts.forEach((lineChart) => {
      const action = lineChart.action
      const changedToActionCount = stats.changedToCount[action].accum
      lineChart.data[0].values.push({x: stats.ticks, y: changedToActionCount});
      lineChart.chart.update(lineChart.data);
      return;
    });
}

function _initLineChart(label, action) {
    lineChart = {
        action: action,
        data: [{label: label, values: []}]
    };
    lineChart.data[0].values.push({x: 0, y: 0});

    lineChart.chart = $(`#line-chart-${action}`).epoch({
        type: 'line',
        data: lineChart.data,
        axes: ['left', 'right', 'bottom'],
        range: [0, _stats.totalEmployeesCount]
    });

    _lineCharts.charts.push(lineChart);
}

$(function () {
    _lineCharts = {
        charts: []
    };
    _initLineChart('New workers', 'work');
    _initLineChart('New loiterers', 'loitering');

    subscribeStatsListener(_updateLineChartsData);
});
