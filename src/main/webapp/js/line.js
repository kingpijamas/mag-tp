var lineChart = null;

function updateLineChartData(data) {
    if (data.type != 'workLog') { return; }
    lineChart.ticks++;
    lineChart.data[0].values.push({x: lineChart.ticks, y: data.newWorkersCount});
    lineChart.data[1].values.push({x: lineChart.ticks, y: data.newLoiterersCount});
    lineChart.chart.update(lineChart.data);
}

$(function () {
    lineChart = {
        ticks: 0
    };
    lineChart.data = [
        {label: 'New workers (count)', values: []},
        {label: 'New loiterers (count)', values: []},
    ];
    lineChart.data[0].values.push({x: 0, y: 0});
    lineChart.data[1].values.push({x: 0, y: 0});

    lineChart.chart = $('#line-chart').epoch({
        type: 'line',
        data: lineChart.data,
        axes: ['left', 'right', 'bottom']
    });
});
