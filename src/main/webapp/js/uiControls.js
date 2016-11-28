$(() => {
    var restartButton = $('#restart-btn');
    restartButton.click(() => {
      resetCounters();
      restartButton.text('Reiniciar');
      $.post('/restart');
    });
    $('#stop-btn').click(() => $.post('/stop'));
});
