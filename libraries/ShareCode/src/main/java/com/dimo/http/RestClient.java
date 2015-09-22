package com.dimo.http;

import com.dimo.utils.StringUtil;
import com.dimo.web.WebViewJavascriptBridge;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.apache.http.Header;
import org.apache.http.message.BasicHeader;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Iterator;

import timber.log.Timber;

/**
 * Created by Alsor Zhou on 15/4/24.
 */
public class RestClient {
    public static final int METHOD_GET = 1;
    public static final int METHOD_POST = 2;
    public static final int METHOD_PUT = 3;
    public static final int MEHOTD_DELETE = 4;

    private static RestClient mInstance = null;

    private static AsyncHttpClient client = new AsyncHttpClient();

    private String kHost;

    public static RestClient getInstance() {
        if (mInstance == null) {
            mInstance = new RestClient();
        }
        return mInstance;
    }

    public void setkHost(String kHost) {
        this.kHost = kHost;
    }

    public void get(String url, RequestParams params, AsyncHttpResponseHandler responseHandler) {
        String rawUrl = null;
        if (url.startsWith("http://") || url.startsWith("https://")) {
            rawUrl = url;
        } else {
            rawUrl = getAbsoluteUrl(url);
        }
        client.get(rawUrl, params, responseHandler);
    }

    public void post(String url, RequestParams params, AsyncHttpResponseHandler responseHandler) {
        String rawUrl = null;
        if (url.startsWith("http://") || url.startsWith("https://")) {
            rawUrl = url;
        } else {
            rawUrl = getAbsoluteUrl(url);
        }
        client.post(rawUrl, params, responseHandler);
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
//            else {
//                //FIXME: fake user auth
//                headers = new Header[1];
//                headers[0] = new BasicHeader("access-key", "test1");
//            }
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
                callback.callback(ret.toString());
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
                callback.callback(ret.toString());
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject response) {
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
                callback.callback(ret.toString());
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
                callback.callback(ret.toString());
            }
        });
    }

    private void request(int method, String url, RequestParams params, Header[] headers, AsyncHttpResponseHandler responseHandler) {
        String rawUrl = null;
        if (url.startsWith("http://") || url.startsWith("https://")) {
            rawUrl = url;
        } else {
            rawUrl = getAbsoluteUrl(url);
        }
        if (params != null)
            params.setUseJsonStreamer(true);
        switch (method) {
            case METHOD_GET:
                client.get(null, rawUrl, headers, params, responseHandler);
                break;
            case METHOD_POST:
                client.post(null, rawUrl, headers, params, null, responseHandler);
                break;
            case METHOD_PUT:
                try {
                    client.put(null, rawUrl, headers, params == null ? null : params.getEntity(null), null, responseHandler);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case MEHOTD_DELETE:
                client.delete(null, rawUrl, headers, params, responseHandler);
                break;
            default:
                throw new IllegalArgumentException("");
        }
    }

    private String getAbsoluteUrl(String relativeUrl) {
        return kHost + relativeUrl;
    }
}
