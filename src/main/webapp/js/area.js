var _areaChart = null;

function _updateAreaChartData(stats) {
    _areaChart.data[0].values.push({x: stats.ticks, y: stats.workersCount.accum});
    _areaChart.data[1].values.push({x: stats.ticks, y: stats.loiterersCount.accum});
    _areaChart.chart.update(_areaChart.data);
}

$(function () {
    _areaChart = {};
    _areaChart.data = [
        {label: 'Work', values: []},
        {label: 'Loitering', values: []}
    ];
    _areaChart.data[0].values.push({x: 0, y: 0});
    _areaChart.data[1].values.push({x: 0, y: 0});

    _areaChart.chart = $('#area-chart').epoch({
        type: 'area',
        data: _areaChart.data,
        axes: ['left', 'right', 'bottom'],
        range: [0, _stats.totalEmployeesCount]
    });

    subscribeStatsListener(_updateAreaChartData);
});
