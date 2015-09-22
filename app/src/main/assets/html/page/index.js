var toggleTabbar = false;
var toggleSwipe = false;

$(function () {
  //alert($(location).attr('href'));

  bridge.connectBridge(function () {
    $('#txt_title').text("Bridge Connected");

    bridge.registerHandler("testJavascriptHandler", function (data) {
      responseCallback(data);
    });

    bridge.registerHandler("callbackWhenGetNotification", function (data) {
      debug(data);
    });

    bridge.registerHandler("activetyFinished", function (data) {
      debug(data);
    });

    $('#btn_new_activity_with_back').click(function () {
      bridge.redirect("http://soso.com", {"hasBackButton": 'true', 'title':'SOSO'}, function (data) {
        debug(data);
      });
    });

    $('#btn_new_activity_without_back').click(function () {
      bridge.redirect("http://soso.com", {"hasBackButton": 'false', 'title':'SOSO'}, function (data) {
        debug(data);
      });
    });

    $('#btn_chose_photo').click(function () {
      bridge.selectPhoto({}, function (data) {
        debug(data);
      });
    });

    $('#btn_upload_photo').click(function () {
      bridge.upload({}, function (data) {
        debug(data);
      });
    });

    $('#btn_call').click(function () {
      bridge.callNumber({'phone': '12312341234'}, function (data) {
        debug(data);
      });
    });

    bridge.enableSwipe({'enable': 'false'}, function (data) {
      debug(data);

      $('#btn_toggle_swipe').text("Swipe Disabled");
    });

    $('#btn_toggle_swipe').click(function () {
      toggleSwipe = !toggleSwipe;

      var val = toggleSwipe ? 'true' : 'false';

      bridge.enableSwipe({'enable': val}, function (data) {
        debug(data);

        if (toggleSwipe) {
          $('#btn_toggle_swipe').text("Swipe Enabled");
        } else {
          $('#btn_toggle_swipe').text("Swipe Disabled");
        }
      });
    });

    // text button on the top right bar item
    //bridge.setRightItem({'text':"option"}, function(data) {
    //  debug(data);
    //});

    // if want to use ... instead of text, leave the 'text' field to empty
    var menu = {"text" : "", "menu" : "['Jonathan Ive', 'Steve Jobs', 'Open In Browser']"};

    bridge.setRightItem(menu, function (data) {
      debug(data);
      //location.reload();
    });

    bridge.getAOSPVersion('', function (data) {
      $('#field_aosp_version').text(data);
    });

    $('#btn_open_browser').click(function () {
      bridge.openLinkWithBrowser({'link': 'http://www.163.com'}, function (data) {
        debug(data);
      });
    });

    $('#btn_finish_activity').click(function () {
      bridge.finishActivity({'result': 'http://www.163.com'}, function (data) {
        debug(data);
      });
    });
    $('#btn_set_pref').click(function () {
      bridge.setPref({'key': 'x', 'value' : '3'}, function (data) {
        debug(data);
      });
    });
    $('#btn_get_pref').click(function () {
      bridge.getPref({'key': 'x'}, function (data) {
        debug(data);

        var obj = JSON.parse(data);
        var string = "Get Pref (x = " + obj.value + ")";
        $('#btn_get_pref').text(string);
      });
    });

    $('#btn_login_leanchat').click(function () {
      bridge.reverseLeanChatLogin({'user':"111", 'passwd':"111"}, function(data) {
        debug(data);

// 1 to 1
        bridge.chatByUserId({'objectId':"5586f624e4b096d588d24453"}, function(result) {
          debug(result);
        });

// group chat
//        bridge.chatByConversion({'objectId':"5586f624e4b096d588d24453"}, function(result) {
//          debug(result);
//        });
      })
    });
  }, false);

});
