package com.yuan.house.http;

import android.app.Application;
import android.os.Looper;

import com.baidu.location.BDLocation;
import com.dimo.utils.StringUtil;
import com.dimo.web.WebViewJavascriptBridge;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.SyncHttpClient;
import com.yuan.house.application.DMApplication;
import com.yuan.house.event.NotificationEvent;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.message.BasicHeader;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import de.greenrobot.event.EventBus;
import timber.log.Timber;

/**
 * Created by Alsor Zhou on 15/4/24.
 */
public class RestClient {
    public static final int METHOD_GET = 1;
    public static final int METHOD_POST = 2;
    public static final int METHOD_PUT = 3;
    public static final int MEHOTD_DELETE = 4;

    private static final int kHttpStatusCodeErrorKickOut = 450;

    private static RestClient mInstance = null;

    private static AsyncHttpClient mAsyncClient = new AsyncHttpClient();
    private static SyncHttpClient mSyncClient = new SyncHttpClient();

    static {
        mAsyncClient.setTimeout(20000);
        mSyncClient.setTimeout(20000);
    }

    private String mHostname;

    public RestClient() {
        getClient().setTimeout(20 * 1000);
    }

    /**
     * @return an async client when calling from the main thread, otherwise a sync client.
     */
    private static AsyncHttpClient getClient() {
        // Return the synchronous HTTP client when the thread is not prepared
        if (Looper.myLooper() == null)
            return mSyncClient;
        return mAsyncClient;
    }

    public static RestClient getInstance() {
        if (mInstance == null) {
            mInstance = new RestClient();
        }
        return mInstance;
    }

    public RestClient setHostname(String mHost) {
        this.mHostname = mHost;
        return mInstance;
    }

    public void get(String url, HashMap<String, String> headers, AsyncHttpResponseHandler responseHandler) {
        get(url, headers, null, responseHandler);
    }

    public void get(String url, HashMap<String, String> headers, RequestParams params, AsyncHttpResponseHandler responseHandler) {
        String rawUrl;
        if (url.startsWith("http://") || url.startsWith("https://")) {
            rawUrl = url;
        } else {
            rawUrl = getAbsoluteUrl(url);
        }

        if (headers != null && (headers instanceof HashMap)) {
            Iterator it = headers.entrySet().iterator();
            BDLocation lastActivatedLocation = DMApplication.getInstance().getLastActivatedLocation();
            getClient().addHeader("city",lastActivatedLocation.getCity());
            getClient().addHeader("district",lastActivatedLocation.getDistrict());
            while (it.hasNext()) {
                Map.Entry pair = (Map.Entry) it.next();
                Timber.v(pair.getKey() + " = " + pair.getValue());
                getClient().addHeader(pair.getKey().toString(), pair.getValue().toString());
                it.remove(); // avoids a ConcurrentModificationException
            }
        }

        getClient().get(rawUrl, params, responseHandler);
    }

    public void post(String url, RequestParams params, AsyncHttpResponseHandler responseHandler) {
        String rawUrl;
        if (url.startsWith("http://") || url.startsWith("https://")) {
            rawUrl = url;
        } else {
            rawUrl = getAbsoluteUrl(url);
        }
        getClient().post(rawUrl, params, responseHandler);
    }

    public void post(String url, HashMap<String, String> headers, HttpEntity entity, AsyncHttpResponseHandler responseHandler) {
        AsyncHttpClient httpClient = new AsyncHttpClient();
        httpClient.setResponseTimeout(20000);

        String rawUrl;
        if (url.startsWith("http://") || url.startsWith("https://")) {
            rawUrl = url;
        } else {
            rawUrl = getAbsoluteUrl(url);
        }

        if (headers != null && (headers instanceof HashMap)) {
            Iterator it = headers.entrySet().iterator();
            BDLocation lastActivatedLocation = DMApplication.getInstance().getLastActivatedLocation();
            getClient().addHeader("city",lastActivatedLocation.getCity());
            getClient().addHeader("district",lastActivatedLocation.getDistrict());
            while (it.hasNext()) {
                Map.Entry pair = (Map.Entry) it.next();
                Timber.v(pair.getKey() + " = " + pair.getValue());
                getClient().addHeader(pair.getKey().toString(), pair.getValue().toString());
                it.remove(); // avoids a ConcurrentModificationException
            }
        }

        getClient().post(null, rawUrl, entity, null, responseHandler);
    }

    public void post(String url, HashMap<String, String> headers, RequestParams requestParams, AsyncHttpResponseHandler responseHandler) {
        AsyncHttpClient httpClient = new AsyncHttpClient();
        httpClient.setResponseTimeout(20000);

        String rawUrl;
        if (url.startsWith("http://") || url.startsWith("https://")) {
            rawUrl = url;
        } else {
            rawUrl = getAbsoluteUrl(url);
        }

        if (headers != null && (headers instanceof HashMap)) {
            Iterator it = headers.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pair = (Map.Entry) it.next();
                Timber.v(pair.getKey() + " = " + pair.getValue());
                httpClient.addHeader(pair.getKey().toString(), pair.getValue().toString());
                it.remove(); // avoids a ConcurrentModificationException
            }
        }

        httpClient.post(rawUrl, requestParams, responseHandler);
    }

    public void bridgeRequest(JSONObject params, int requestMethod, final WebViewJavascriptBridge.WVJBResponseCallback callback) {
        String url = null;
        RequestParams requestParams = null;
        Header[] headers = null;
        try {
            if (params.has("data")) {
                requestParams = StringUtil.JSON2RequestParams(params.getJSONObject("data"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            url = params.has("url") ? params.getString("url") : null;
            if (params.has("headers")) {
                JSONObject headersJson = params.getJSONObject("headers");
                headers = new Header[headersJson.length()];
                Iterator<String> keys = headersJson.keys();
                int i = 0;
                while (keys.hasNext()) {
                    String key = keys.next();
                    headers[i++] = new BasicHeader(key, headersJson.getString(key));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (url == null)
            throw new IllegalArgumentException("json object must have a url key");

        request(requestMethod, url, requestParams, headers, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                JSONObject ret = new JSONObject();
                try {
                    ret.put("status", statusCode);
                    if (response != null) {
                        ret.put("data", response);
                    }
                    if (headers != null && headers.length > 0) {
                        JSONObject headersJson = new JSONObject();
                        for (Header header : headers) {
                            headersJson.put(header.getName(), header.getValue());
                        }
                        ret.put("headers", headersJson);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (callback != null) {
                    callback.callback(ret);
                }
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONArray response) {
                JSONObject ret = new JSONObject();
                try {
                    ret.put("status", statusCode);
                    if (response != null) {
                        ret.put("data", response);
                    }
                    if (headers != null && headers.length > 0) {
                        JSONObject headersJson = new JSONObject();
                        for (Header header : headers) {
                            headersJson.put(header.getName(), header.getValue());
                        }
                        ret.put("headers", headersJson);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (callback != null) {
                    callback.callback(ret);
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject response) {
                if (statusCode == kHttpStatusCodeErrorKickOut) {
                    EventBus.getDefault().post(new NotificationEvent(NotificationEvent.NotificationEventEnum.KICK_OUT, null));
                    return;
                }

                JSONObject ret = new JSONObject();
                try {
                    ret.put("status", statusCode);
                    if (response != null) {
                        ret.put("data", response);
                    } else {
                        JSONObject data = new JSONObject();
                        data.put("msg", "无法访问网络");
                        ret.put("data", data);
                    }
                    if (headers != null && headers.length > 0) {
                        JSONObject headersJson = new JSONObject();
                        for (Header header : headers) {
                            headersJson.put(header.getName(), header.getValue());
                        }
                        ret.put("headers", headersJson);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (callback != null) {
                    callback.callback(ret);
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String response, Throwable throwable) {
                Timber.e(response);
                JSONObject ret = new JSONObject();
                try {
                    ret.put("status", statusCode);
                    ret.put("data", new JSONObject("{\"msg\":\"unknown exception\",\"error_code\":0}"));
                    ret.put("error_code", 0);
                    if (headers != null && headers.length > 0) {
                        JSONObject headersJson = new JSONObject();
                        for (Header header : headers) {
                            headersJson.put(header.getName(), header.getValue());
                        }
                        ret.put("headers", headersJson);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (callback != null) {
                    callback.callback(ret);
                }
            }
        });
    }

    private void request(int method, String url, RequestParams params, Header[] headers, AsyncHttpResponseHandler responseHandler) {
        String rawUrl;
        if (url.startsWith("http://") || url.startsWith("https://")) {
            rawUrl = url;
        } else {
            rawUrl = getAbsoluteUrl(url);
        }
        if (params != null)
            params.setUseJsonStreamer(true);
        switch (method) {
            case METHOD_GET:
                getClient().get(null, rawUrl, headers, params, responseHandler);
                break;
            case METHOD_POST:
                getClient().post(null, rawUrl, headers, params, null, responseHandler);
                break;
            case METHOD_PUT:
                try {
                    getClient().put(null, rawUrl, headers, params == null ? null : params.getEntity(null), null, responseHandler);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case MEHOTD_DELETE:
                getClient().delete(null, rawUrl, headers, params, responseHandler);
                break;
            default:
                throw new IllegalArgumentException("");
        }
    }

    private String getAbsoluteUrl(String relativeUrl) {
        return mHostname + relativeUrl;
    }
}
