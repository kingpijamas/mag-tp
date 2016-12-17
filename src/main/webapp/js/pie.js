var _pieChart = null;

function _updatePieChartData(stats) {
    const workersCount = stats.workersCount
    const total = workersCount + stats.loiterersCount;
    const workingPct = (workersCount / total) * 100;

    _pieChart.data[0].value = workingPct;
    _pieChart.data[1].value = 100 - workingPct;
    _pieChart.chart.update(_pieChart.data);
}

$(function () {
    _pieChart = {};

    _pieChart.data = [
        {label: 'Trabajadores', value: 0},
        {label: 'Holgazanes', value: 100}
    ];

    _pieChart.chart = $('#pie-chart').epoch({
        type: 'pie',
        data: _pieChart.data
    });

    subscribeStatsListener(_updatePieChartData);
});
