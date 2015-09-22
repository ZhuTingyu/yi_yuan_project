$(function () {

    bridge.connectBridge(function () {

        $('#txt_title').text("Bridge Connected");

        bridge.getPref({'key':'images'}, function(data) {
            var object = JSON.parse(data);
            var images = object.value;
            if (!isEmpty(images)) {
                var list = images.split(',');
                for(var i in list) {
                    debug(list[i]);
                    $('#images').append("<img src='" + list[i] + "' width='100' height='100'>");
                }
            }
        });
    }, false);
});
