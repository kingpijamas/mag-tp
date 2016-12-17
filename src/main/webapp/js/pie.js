var pieChart = null;

function updatePieChartData(data) {
    if (data.type != 'statsLog') { return; }

    const workersCount = accumulateAttributeInChildren(data.stats.work, 'currentCount', 0);
    const loiterersCount = accumulateAttributeInChildren(data.stats.loiter, 'currentCount', 0);
    const total = workersCount + loiterersCount;
    const workingPct = (workersCount / total) * 100;

    pieChart.data[0].value = workingPct;
    pieChart.data[1].value = 100 - workingPct;
    pieChart.chart.update(pieChart.data);
}

$(function () {
    pieChart = {};

    pieChart.data = [
        {label: 'Trabajadores', value: 0},
        {label: 'Holgazanes', value: 100}
    ];

    pieChart.chart = $('#pie-chart').epoch({
        type: 'pie',
        data: pieChart.data
    });
});
