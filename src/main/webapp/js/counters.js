var _counters = null;

function _updateCounterData(stats) {
    _counters.ticks++;

    const workersCount = stats.workersCount;
    const loiterersCount = stats.loiterersCount;
    const total = workersCount + loiterersCount;

    if ((workersCount / total) >= _counters.limit) {
        _markLimit('work');
    } else if ((loiterersCount / total) >= _counters.limit) {
        _markLimit('loiter');
    }
}

function _markLimit(counterKey) {
    if (_counters.limitReached == counterKey) { return; }
    _counters.limitReached = counterKey;
    _counters[counterKey].text(_counters.ticks);
}

function resetCounters() {
    _counters.ticks = 0;
    _counters.limitReached = null;
    _counters.work.text('-');
    _counters.loiter.text('-');
}

$(function() {
    _counters = {
      ticks: 0,
      limit: 0.95,
      work: $('#counter-work'),
      loiter: $('#counter-loiter')
    };
    resetCounters();
    $('#counter-dominance-limit').text(_counters.limit * 100);

    subscribeStatsListener(_updateCounterData);
});
