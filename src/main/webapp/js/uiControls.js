$(() => {
    $('#restart-btn').click(() => $.post('/restart'));
    $('#stop-btn').click(() => $.post('/stop'));
});
