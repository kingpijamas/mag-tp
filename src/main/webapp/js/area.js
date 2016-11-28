var areaChart = null;

function updateAreaChartData(data) {
    if (data.type != 'workLog') { return; }
    areaChart.ticks++;
    areaChart.data[0].values.push({x: areaChart.ticks, y: data.workingCount});
    areaChart.data[1].values.push({x: areaChart.ticks, y: data.loiteringCount});
    areaChart.chart.update(areaChart.data);
}

$(function () {
    areaChart = {
        ticks: 0
    };
    areaChart.data = [
        {label: 'Work', values: []},
        {label: 'Loitering', values: []}
    ];
    areaChart.data[0].values.push({x: 0, y: 0});
    areaChart.data[1].values.push({x: 0, y: 0});

    areaChart.chart = $('#area-chart').epoch({
        type: 'area',
        data: areaChart.data,
        axes: ['left', 'right', 'bottom']
    });
});
