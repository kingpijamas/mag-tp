var areaChart = null;
var ticks = 0;

var areaChartData = [
    {label: 'Work', values: []},
    {label: 'Loitering', values: []}
];
areaChartData[0].values.push({x: 0, y: 0});
areaChartData[1].values.push({x: 0, y: 0});

function updateAreaChartData(data) {
    if (data.type == 'workLog') {
        ticks++;
        areaChartData[0].values.push({x: ticks, y: data.workStats.sum});
        areaChartData[1].values.push({x: ticks, y: data.loiteringStats.sum});
        areaChart.update(areaChartData);
    }
}

$(function () {
    areaChart = $('#area-chart').epoch({
        type: 'area',
        data: areaChartData,
        axes: ['left', 'right', 'bottom']
    });
});
