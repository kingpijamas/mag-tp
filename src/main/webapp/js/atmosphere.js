var _atmosphereStarted = false;

function startAtmosphereConnection(url) {
    if (_atmosphereStarted) { return; }

    _atmosphereStarted = true;
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
        console.log(response.responseBody); // TODO: remove!
        updateStats(responseBody);
    };
    request.onClose = function (response) {
        _atmosphereStarted = false;
    };
    request.onError = function (response) {
        _atmosphereStarted = false;
    };

    const subSocket = socket.subscribe(request);

    const json = {
        string: ''
    };
}
