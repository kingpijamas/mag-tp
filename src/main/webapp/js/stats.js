var _stats = null;

function updateStats(data) {
    if (data.type != 'statsLog') { return; }
    _stats.ticks++;

    const workStats = data.stats.work;
    const loiteringStats = data.stats.loiter;
    _stats.workersCount = _accumulateAttributeInChildren(workStats, 'currentCount', 0);
    _stats.loiterersCount = _accumulateAttributeInChildren(loiteringStats, 'currentCount', 0);
    _stats.changedToCount.work = _accumulateAttributeInChildren(workStats, 'changedCount', 0);
    _stats.changedToCount.loitering = _accumulateAttributeInChildren(loiteringStats, 'changedCount', 0);

    _stats.listenerUpdaters.forEach((listenerUpdater) => listenerUpdater.call(this, _stats));
}

function subscribeStatsListener(listenerUpdater) {
    _stats.listenerUpdaters.push(listenerUpdater);
}

function _accumulateAttributeInChildren(obj, attributeKey, defaultValue) {
    result = {};
    let accumulation = defaultValue;
    for (let [key, child] of _attributes(obj)) {
        let attributeValue = child[attributeKey];
        if (!attributeValue) {
            attributeValue = defaultValue;
        }
        result[key] = attributeValue;
        accumulation += attributeValue;
    }
    result.accum = accumulation;
    return result;
}

function _attributes(obj) {
    return (for (key of Object.keys(obj)) [key, obj[key]]);
}

$(function() {
    _stats = {
        ticks: 0,
        listenerUpdaters: [],
        accumKey: 'accum',
        changedToCount: {},
        // XXX
        totalEmployeesCount: parseInt($('#employees-count').val())
    };
})
