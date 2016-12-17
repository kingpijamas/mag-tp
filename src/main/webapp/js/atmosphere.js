var atmosphereStarted = false;

function startAtmosphereConnection(url) {
    if (atmosphereStarted) { return; }

    atmosphereStarted = true;
    const socket = $.atmosphere;
    const request = {
        url: url,
        contentType: 'application/json',
        logLevel: 'info',
        transport: 'sse',
        fallbackTransport: 'long-polling'
    };
    request.onOpen = function (response) {
        subSocket.push(jQuery.stringifyJSON(json));
    };
    request.onMessage = function (response) {
        const responseBody = JSON.parse(response.responseBody);
        console.log(response.responseBody);
        updatePieChartData(responseBody);
        updateAreaChartData(responseBody);
        updateLineChartData(responseBody);
        updateCounterData(responseBody);
    };
    request.onClose = function (response) {
        atmosphereStarted = false;
    };
    request.onError = function (response) {
        atmosphereStarted = false;
    };

    const subSocket = socket.subscribe(request);

    const json = {
        string: ''
    };
}
