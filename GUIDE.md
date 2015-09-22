### 如何编写可与Native交互的Javascript文件
本文档主要介绍如何代码实现 Native Java 与 Javascript 相互调用。

#### Bridge 介绍
Bridge 是基于 href 方式的封装，提供了双向函数接口，简化了开发和维护难度。我们采用的是 [WebViewJavascriptBridge for Android](https://github.com/jesse01/WebViewJavascriptBridge)。

#### JS 调用 Native 代码
在 JS 中调用 Native 代码是主要的应用场景，例如，我们可以在 JS 中请求打开硬件资源，通过 Native 提供的 HTTP 访问接口获取数据，使用系统默认的交互方式等。至关重要的是，可以隐藏我们实现的关键逻辑。

想要从 JS 中调用 Native 代码，需要采用如下步骤：

1. 我们需要在 Native 代码中注册 handler
```
registerHandler("testNativeCallback", new WVJBWebViewClient.WVJBHandler() {
    @Override
    public void request(Object data, WVJBResponseCallback callback) {
        Timber.v(TAG, "testNativeCallback called:" + data);
        callback.callback("Response from testNativeCallback!");
    }
});
```
2. 在 bridge 初始化完成之后，可以在 JS 中采用如下格式调用
```
bridge.call("testNativeCallback", {"foo":"before ready"}, null);
```
3. 如果需要在执行完 Native 部分之后，执行 JS 中的代码，可以在 callback 中实现
```
bridge.call("testNativeCallback", {"foo":"before ready"}, function(data) {
       responseCallback(data);
});
```

#### Native 调用 JS 代码
有些时候我们想让 Native 的部分也能调用 JS 代码，例如，接收到系统通知时刷新当前页面，获取到数据之后传给页面，把系统图库中的图片发给页面等。

想要从 Native 中调用 JS 代码，需要采用如下步骤：

1. 在 bridge 初始化完成之后, 在 JS 代码中注册 handler
```
  bridge.registerHandler("testJavascriptHandler", function(data) {
      responseCallback(data);
  });
```
2. 可以在 Native 中采用如下格式调用
```
callHandler("testJavascriptHandler", new JSONObject("{\"foo\":\"before ready\" }"), new WVJBResponseCallback() {

    @Override
    public void callback(Object data) {
        Timber.v(TAG, "ObjC call testJavascriptHandler got response! :" + data);
    }
});
```

#### 附录 Appendix
如果本文档中有未尽描述事项，请参考如下两个项目的文档
* [WebViewJavascriptBridge for Android](https://github.com/jesse01/WebViewJavascriptBridge)
* [WebViewJavascriptBridge for iOS](https://github.com/marcuswestin/WebViewJavascriptBridge)