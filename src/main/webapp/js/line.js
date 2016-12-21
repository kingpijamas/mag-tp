var _lineChart = null;

function _updateLineChartData(stats) {
    _lineChart.data[0].values.push({x: stats.ticks, y: stats.changedToWorkCount.accum});
    _lineChart.data[1].values.push({x: stats.ticks, y: stats.changedToLoiteringCount.accum});
    _lineChart.chart.update(_lineChart.data);
}

$(function () {
    _lineChart = {};
    _lineChart.data = [
        {label: 'New workers (count)', values: []},
        {label: 'New loiterers (count)', values: []},
    ];
    _lineChart.data[0].values.push({x: 0, y: 0});
    _lineChart.data[1].values.push({x: 0, y: 0});

    _lineChart.chart = $('#line-chart').epoch({
        type: 'line',
        data: _lineChart.data,
        axes: ['left', 'right', 'bottom']
    });

    subscribeStatsListener(_updateLineChartData);
});
