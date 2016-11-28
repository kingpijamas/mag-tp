var pieChart = null;

function updatePieChartData(data) {
    if (data.type != 'workLog') { return; }
    var totalTime = data.workStats.sum + data.loiteringStats.sum;
    var workedTimePct = (data.workStats.sum / totalTime) * 100;
    pieChart.data[0].value = workedTimePct;
    pieChart.data[1].value = 100 - workedTimePct;
    pieChart.chart.update(pieChart.data);
}

$(function () {
    pieChart = {};

    pieChart.data = [
        {label: 'Work', value: 0},
        {label: 'Loitering', value: 100}
    ];

    pieChart.chart = $('#pie-chart').epoch({
        type: 'pie',
        data: pieChart.data
    });
});
