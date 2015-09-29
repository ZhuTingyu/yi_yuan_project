/**
 * Web Bridge Routine
 *
 * An iOS/OSX bridge for sending messages between Obj-C and JavaScript in UIWebViews/WebViews.
 *
 * https://github.com/marcuswestin/WebViewJavascriptBridge
 *
 * Created by Alsor Zhou on 14-9-11.
 */
var gBridge;

function connectWebViewJavascriptBridge(callback) {
  if (window.WebViewJavascriptBridge) {
    callback(WebViewJavascriptBridge)
  } else {
    document.addEventListener('WebViewJavascriptBridgeReady', function () {
      callback(WebViewJavascriptBridge)
    }, false)
  }
}

connectWebViewJavascriptBridge(function (b) {
  // save a global copy
  gBridge = b;

  b.init(function (message, responseCallback) {
    console.log("connectWebViewJavascriptBridge finished");
    //        alert('JS got a message', message);
  })
});

var bridge = {
  connectBridge: function (callback, dummy) {
    connectWebViewJavascriptBridge(callback);
  },
  /**
   * Call native function thru web bridge in raw format
   *
   * @param handler
   * @param params
   * @param callback
   */
  //call: function (handler, params, callback) {
  //  gBridge.callHandler(handler, params, callback);
  //},
  /**
   * Register monitor for objc reverse invokation
   * @param fn function name
   * @param callback
   */
  registerHandler: function (fn, callback) {
    gBridge.registerHandler(fn, callback);
  },
  /**
   * Page redirection and show in NEW window
   *
   * @param url redirection page
   * @param params query params
   */
  redirect: function (url, params, callback) {
    var obj = {'url': url, 'params': params};
    var string = JSON.stringify(obj);

    gBridge.callHandler('redirectPage', string, callback);
  },
  /**
   * Page redirection and show in CURRENT window (replace old one)
   *
   * @param url redirection page
   * @param params query params
   * @param callback callback when finished
   */
  replace: function (url, params, callback) {
    var obj = {'url': url, 'params': params};
    var string = JSON.stringify(obj);

    gBridge.callHandler('replacePage', string, callback);
  },
  /**
   * Show an indetermine message popup - Error
   *
   * @param params popup parameters
   */
  showErrorMsg: function (params) {
    var obj = params;
    var string = JSON.stringify(obj);

    gBridge.callHandler('showErrorMsg', string, null);
  },
  /**
   * Show an indetermine message popup - Success
   *
   * @param params popup parameters
   */
  showSuccessMsg: function (params) {
    var obj = params;
    var string = JSON.stringify(obj);

    gBridge.callHandler('showSuccessMsg', string, null);
  },
  /**
   * Show progress circular bar
   */
  showProgressDialog: function (msg) {
    var obj = params;
    var string = JSON.stringify(obj);

    gBridge.callHandler('showProgressDialog', string, null);
  },
  /**
   * Dismiss progress circular bar
   */
  dismissProgressDialog: function () {
    gBridge.callHandler('dismissProgressDialog', null, null);
  },
  setUnreadMsgNumber: function (count, callback) {
    var obj = {"count": count};
    var string = JSON.stringify(obj);

    gBridge.callHandler('setUnreadMsgNumber', string, callback);
  },

  updatePackage: function (params, callback) {
    var obj = params;
    var string = JSON.stringify(obj);

    gBridge.callHandler('updatePackage', string, callback);
  },
  getCurrentPackageVersion: function (params, callback) {
    var obj = params;
    var string = JSON.stringify(obj);

    gBridge.callHandler('getCurrentPackageVersion', string, callback);
  },
  shareOnSocial: function (params, callback) {
    var obj = params;
    var string = JSON.stringify(obj);

    gBridge.callHandler('shareOnSocial', string, callback);
  },
  selectPhoto: function (params, callback) {
    var obj = params;
    var string = JSON.stringify(obj);

    gBridge.callHandler('selectPhoto', string, callback);
  },
  showImagePicker: function (params, callback) {
    var obj = params;
    var string = JSON.stringify(obj);

    gBridge.callHandler('showImagePicker', string, callback);
  },
  uploadFile: function (params, callback) {
    var obj = params;
    var string = JSON.stringify(obj);

    gBridge.callHandler('uploadFile', string, callback);
  },
  setTitle: function (params, callback) {
    var obj = params;
    var string = JSON.stringify(obj);

    gBridge.callHandler('setTitle', string, callback);
  },

  showTabbar: function (params, callback) {
    var obj = params;
    var string = JSON.stringify(obj);

    gBridge.callHandler('showTabbar', string, callback);
  },

  getAOSPVersion: function (callback) {
    gBridge.callHandler('getAOSPVersion', null, callback);
  },

  openLinkWithBrowser: function (params, callback) {
    var obj = params;
    var string = JSON.stringify(obj);

    gBridge.callHandler('openLinkWithBrowser', string, callback);
  },

  callNumber: function (params, callback) {
    var obj = params;
    var string = JSON.stringify(obj);

    gBridge.callHandler('callNumber', string, callback);
  },

  enableSwipe: function (params, callback) {
    var obj = params;
    var string = JSON.stringify(obj);

    gBridge.callHandler('enableSwipe', string, callback);
  },

  setRightItem: function (params, callback) {
    var obj = params;
    var string = JSON.stringify(obj);

    gBridge.callHandler('setRightItem', string, callback);
  },

  finishActivity: function (params, callback) {
    var obj = params;
    var string = JSON.stringify(obj);

    gBridge.callHandler('finishActivity', string, callback);
  },

  setActivityResult: function (params, callback) {
    var obj = params;
    var string = JSON.stringify(obj);

    gBridge.callHandler('setActivityResult', string, callback);
  },

  setPref: function (params, callback) {
    var obj = params;
    var string = JSON.stringify(obj);

    gBridge.callHandler('setPref', string, callback);
  },

  getPref: function (params, callback) {
    var obj = params;
    var string = JSON.stringify(obj);

    gBridge.callHandler('getPref', string, callback);
  },

  chatByUserId: function (params, callback) {
    var obj = params;
    var string = JSON.stringify(obj);

    gBridge.callHandler('chatByUserId', string, callback);
  },

  chatByConversion: function (params, callback) {
    var obj = params;
    var string = JSON.stringify(obj);

    gBridge.callHandler('chatByConversion', string, callback);
  },

  executeLeanChatLogout: function (params, callback) {
    var obj = params;
    var string = JSON.stringify(obj);

    gBridge.callHandler('executeLeanChatLogout', string, callback);
  },

  reverseLeanChatLogin: function (params, callback) {
    var obj = params;
    var string = JSON.stringify(obj);

    gBridge.callHandler('reverseLeanChatLogin', string, callback);
  },

  saveImageDataToGallery: function (params, callback) {
    var obj = {"data": params};
    var string = JSON.stringify(obj);

    gBridge.callHandler('saveImageDataToGallery', string, callback);
  }
};
