var pieChart = null;

var pieChartData = [
    {label: 'Work', value: 0},
    {label: 'Loitering', value: 100}
];

function updatePieChartData(data) {
    if (data.type == 'workLog') {
        var totalTime = data.totalWork + data.totalLoitering
        var workedTimePct = (data.totalWork / totalTime) * 100
        pieChartData[0].value = workedTimePct
        pieChartData[1].value = 100 - workedTimePct
        pieChart.update(pieChartData)
    }
}

$(function(){
    pieChart = $('#pie-chart').epoch({
        type: 'pie',
        data: pieChartData
    });
})
