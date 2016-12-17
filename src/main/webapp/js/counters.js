var counters = null;

function updateCounterData(data) {
    if (data.type != 'statsLog') { return; }
    counters.ticks++;

    const workersCount = accumulateAttributeInChildren(data.stats.work, 'currentCount', 0);
    const loiterersCount = accumulateAttributeInChildren(data.stats.loiter, 'currentCount', 0);
    const total = workersCount + loiterersCount;
    const workingPct = workersCount / total;
    const loiteringPct = loiterersCount / total;

    let counterToUpdate = null;
    if (counters.limitReached != 'work' && workingPct >= counters.limit) {
        counters.limitReached = 'work';
        counterToUpdate = counters.work;
    } else if (counters.limitReached != 'loiter' && loiteringPct >= counters.limit) {
        counters.limitReached = 'loiter';
        counterToUpdate = counters.loiter;
    } else {
        return;
    }

    counterToUpdate.text(counters.ticks);
}

function resetCounters() {
    counters.limitReached = null;
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
