$(() => {
    let restartButton = $('#restart-btn');
    restartButton.click(() => {
      resetCounters();
      restartButton.text('Reiniciar');
      $.post('/restart', (data) => startAtmosphereConnection('/ui'));
    });
    $('#pause-btn').click(() => $.post('/pause'));
    $('#resume-btn').click(() => $.post('/resume'));
    $('#step-btn').click(() => $.post('/step'));
    // $('#stop-btn').click(() => $.post('/stop'));
});
