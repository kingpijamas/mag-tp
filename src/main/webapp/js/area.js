var areaChartData = [
  { label: 'Layer 1', values: [ {x: 0, y: 0}, {x: 1, y: 1}, {x: 2, y: 2} ] },
  { label: 'Layer 2', values: [ {x: 0, y: 0}, {x: 1, y: 1}, {x: 2, y: 4} ] }
];

var areaChart = $('#areaChart').epoch({
    type: 'area',
    data: areaChartData,
    axes: ['left', 'right', 'bottom']
});
