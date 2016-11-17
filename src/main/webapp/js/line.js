var lineChart = null;

function updateLineChartData(data) {
    if (data.type == 'workLog') {
        lineChart.ticks++;
        lineChart.data[0].values.push({x: lineChart.ticks, y: data.workStats.mean});
        lineChart.data[1].values.push({x: lineChart.ticks, y: data.workStats.variance});
        lineChart.data[2].values.push({x: lineChart.ticks, y: data.loiteringStats.mean});
        lineChart.data[3].values.push({x: lineChart.ticks, y: data.loiteringStats.variance});
        lineChart.chart.update(lineChart.data);
    }
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
