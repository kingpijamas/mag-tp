var lineChart = null;

function updateLineChartData(data) {
    if (data.type != 'workLog') { return; }
    lineChart.ticks++;
    if (data.workStats.hasOwnProperty('mean')) {
        lineChart.data[0].values.push({x: lineChart.ticks, y: data.workStats.mean});
        lineChart.data[1].values.push({x: lineChart.ticks, y: data.workStats.variance});
    }
    if (data.loiteringStats.hasOwnProperty('mean')) {
        lineChart.data[2].values.push({x: lineChart.ticks, y: data.loiteringStats.mean});
        lineChart.data[3].values.push({x: lineChart.ticks, y: data.loiteringStats.variance});
    }
    if (!data.workStats.hasOwnProperty('mean') && !data.loiteringStats.hasOwnProperty('mean')) { return; }
    lineChart.chart.update(lineChart.data);
}

$(function () {
    lineChart = {
        ticks: 0
    };
    lineChart.data = [
        {label: 'Work (mean)', values: []},
        {label: 'Work (variance)', values: []},
        {label: 'Loitering (mean)', values: []},
        {label: 'Loitering (variance)', values: []}
    ];
    lineChart.data[0].values.push({x: 0, y: 0});
    lineChart.data[1].values.push({x: 0, y: 0});
    lineChart.data[2].values.push({x: 0, y: 0});
    lineChart.data[3].values.push({x: 0, y: 0});

    lineChart.chart = $('#line-chart').epoch({
        type: 'line',
        data: lineChart.data,
        axes: ['left', 'right', 'bottom']
    });
});
