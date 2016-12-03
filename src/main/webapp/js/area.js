var areaChart = null;

function updateAreaChartData(data) {
    if (data.type != 'statsLog') { return; }
    areaChart.ticks++;
    areaChart.data[0].values.push({x: areaChart.ticks, y: data.stats.work.currentCount});
    areaChart.data[1].values.push({x: areaChart.ticks, y: data.stats.loiter.currentCount});
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
