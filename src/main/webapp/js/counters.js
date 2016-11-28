var counters = null;

function updateCounterData(data) {
    if (data.type != 'workLog') { return; }
    if (counters.limitReached) { return; }
    counters.ticks++;

    var total = data.workingCount + data.loiteringCount;
    var workingPct = data.workingCount / total;
    var loiteringPct = data.loiteringCount / total;

    var counterToUpdate = null;
    if (workingPct >= counters.limit) {
        counterToUpdate = counters.work;
    } else if (loiteringPct >= counters.limit) {
        counterToUpdate = counters.loiter;
    } else {
        return;
    }

    counters.limitReached = true;
    counterToUpdate.text(counters.ticks);
}

function resetCounters() {
    counters.limitReached = false;
    counters.ticks = 0;
    counters.work.text('-');
    counters.loiter.text('-');
}

$(function() {
    counters = {};
    counters.limit = 0.95;
    counters.work = $('#counter-work');
    counters.loiter = $('#counter-loiter');
    resetCounters();
    $('#counter-dominance-limit').text(counters.limit * 100)
});
