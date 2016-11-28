$(() => {
    $('#restart-btn').click(() => {
      resetCounters();
      $.post('/restart');
    });
    $('#stop-btn').click(() => $.post('/stop'));
});
