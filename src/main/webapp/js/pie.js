var pieChart = null;

function updatePieChartData(data) {
    if (data.type != 'workLog') { return; }
    var totalTime = data.workingCount + data.loiteringCount;
    var workedTimePct = (data.workingCount / totalTime) * 100;
    pieChart.data[0].value = workedTimePct;
    pieChart.data[1].value = 100 - workedTimePct;
    pieChart.chart.update(pieChart.data);
}

$(function () {
    pieChart = {};

    pieChart.data = [
        // {label: 'Work', value: 0},
        // {label: 'Loitering', value: 100}
        {label: 'Trabajadores', value: 0},
        {label: 'Holgazanes', value: 100}
    ];

    pieChart.chart = $('#pie-chart').epoch({
        type: 'pie',
        data: pieChart.data
    });
});
