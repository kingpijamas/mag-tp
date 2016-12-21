var _pieCharts = null;

function _updatePieChartsData(stats) {
    _pieCharts.charts.forEach((pieChart) => {
        const groupName = pieChart.groupName;
        const workersCount = _fetch(stats.workersCount, groupName, _stats.accumKey);
        const loiterersCount = _fetch(stats.loiterersCount, groupName, _stats.accumKey);
        const total = workersCount + loiterersCount;
        const workingPct = (workersCount / total) * 100;

        pieChart.data[0].value = workingPct;
        pieChart.data[1].value = 100 - workingPct;
        pieChart.chart.update(pieChart.data);
        return;
    });
}

function _fetch(obj, key, defaultKey) {
  return obj.hasOwnProperty(key) ? obj[key] : obj[defaultKey];
}

function _onPieChartSelectionChange(pieChartSelector, selectId) {
    return () => {
        let groupName = $(pieChartSelector).val();
        if (!groupName) {
            groupName = _stats.accumKey;
        }
        _pieCharts.charts[selectId].groupName = groupName;
        return;
    };
}

function _initPieChart(selectId) {
    const data = [
        {label: 'Trabajadores', value: 0},
        {label: 'Holgazanes', value: 100}
    ];

    const chart = $(`#pie-chart-${selectId}`).epoch({
        type: 'pie',
        data: data
    });

    const pieChart = {data: data, chart: chart, groupName: _stats.accumKey};
    _pieCharts.charts[selectId] = pieChart;
    return pieChart;
}

$(function () {
    _pieCharts = {
        charts: []
    };

    $('.pie-chart-select').each((_, pieChartSelector) => {
        const selectId = $(pieChartSelector).attr('group-select-id');
        const pieChart = _initPieChart(selectId);
        $(pieChartSelector).change(_onPieChartSelectionChange(pieChartSelector, selectId));
        return;
    });

    // _pieCharts.chart = $('#pie-chart').epoch({
    //     type: 'pie',
    //     data: _pieCharts.data
    // });

    subscribeStatsListener(_updatePieChartsData);
});
