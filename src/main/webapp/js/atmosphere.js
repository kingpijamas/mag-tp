var socket = $.atmosphere;
var request = {
    url: 'http://localhost:8080/ui',
    contentType: 'application/json',
    logLevel: 'info',
    transport: 'sse',
    fallbackTransport: 'long-polling'
};
request.onOpen = function (response) {
    // console.log('request.onOpen: ' + response.transport);
    // console.log(response);
    subSocket.push(jQuery.stringifyJSON(json));
};
request.onMessage = function (response) {
    // console.log('request.onMessage: ' + response.responseBody)
    var responseBody = JSON.parse(response.responseBody);
    updatePieChartData(responseBody);
    updateAreaChartData(responseBody);
    updateLineChartData(responseBody);
    updateCounterData(responseBody);
};
request.onClose = function (response) {
    // console.log('request.onClose');
};
request.onError = function (response) {
    // console.log('request.onError');
};

var subSocket = socket.subscribe(request);

var json = {
    string: ''
};
