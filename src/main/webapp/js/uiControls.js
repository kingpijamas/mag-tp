$(function () {
    $('#restart-btn').click(function () {
        console.log('Restarting...');
        $.post('/restart');
    });
});
