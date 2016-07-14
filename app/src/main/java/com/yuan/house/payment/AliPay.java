package com.yuan.house.payment;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.widget.Toast;

import com.alipay.pay.PayResult;
import com.alipay.pay.SignUtils;
import com.alipay.sdk.app.PayTask;
import com.dimo.web.WebViewJavascriptBridge;
import com.yuan.house.bean.PayInfo;
import com.yuan.house.common.Constants;

import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * 支付宝
 * Created by LiFengYi on 16/3/4.
 */
public class AliPay {

    // 商户PID
    public static final String PARTNER = "2088021957520575";
    // 商户收款账号
    public static final String SELLER = "zhifubao@bigbuschina.com";
    // 商户私钥，pkcs8格式
    // openssl pkcs8 -topk8 -inform PEM -in rsa_private_key.pem -outform PEM -nocrypt
    public static final String RSA_PRIVATE = "MIICXQIBAAKBgQDVyeELXPE42ohLgDsjtjuKhlGLH7ktJFQ7HuemLrRQTIx6NJjg" +
            "w2Py8z5MhXLiSaDd5obNsGryGBhEcE3DTDQbl5B3dZrdMUz6IB9NLYeoXAOcQ5bS" +
            "Q9ENS0hs6ffpacXad8Y4/s0AJdXDnw1QM1IDomSOA+7NWCDEdnx5mODyxwIDAQAB" +
            "AoGAAXYf07QO8hSMWffGb7gPMCM663iMC4L0EHNlof0vvlzHmQ5C/UZYqJCBHITN" +
            "BD78moe45C8QjBceE1mxLmSW9pYdKni485xSA9hKd4A0L8hC/Dq4OB6b68oeZWQg" +
            "Ovb+wdippqcU/EMb43eeBfP853DHUS6RWpi5QxAsmPrM4gECQQD9p+XgyLKSTXaq" +
            "CiaLKGHCT7g9fFgWWh3RxPNq0KRKJLTuxAWUR8pW/x1LkWzZZ7BZVioBiCSm5JSj" +
            "koPiN+3TAkEA18OppSaMLhG0beeOQEPa6mguemsj/XD+Tx8u6afKIzeF3d4FSHED" +
            "d+UtdzQiM7YUnA1FN88i7we8t63fM09qvQJBAKIK/3zxWbrDB9qq72KY4pLqsK1+" +
            "4RsuaLxYkJSdQ0cmWfvqeBH5Jtca1REwjxP3/J2lwOIHojqH7jjs4ZnD1wcCQExx" +
            "WwJTBOkxhp5FoqVsJPxVitn+aZLaXI9FugY4Vypg+oGfrnl66MKqpJAOHWv6v5SG" +
            "MxbRwUu0jwz4ghKYuXUCQQC64VfeOizPmR7QKNyeBaY7ysymkc04NtfOkmuWoxoX" +
            "ReYzheiYbhofmeBvYVpdmub5LtVrf/+w3f1HbCDuaRc3";

    private static final int SDK_PAY_FLAG = 1;
    private static final int SDK_CHECK_FLAG = 2;

    private PayInfo payInfo;
    private Context context;
    private Activity activity;
    private Handler handler;

    private WebViewJavascriptBridge.WVJBResponseCallback mPayCallback;

    public AliPay(PayInfo payInfo, Context context, Activity activity) {
        super();
        this.payInfo = payInfo;
        this.context = context;
        this.activity = activity;
    }

    public AliPay() {

    }

    public void setHandler(Handler handler) {
        this.handler = handler;
    }

    public void setPayCallback(WebViewJavascriptBridge.WVJBResponseCallback mPayCallback) {
        this.mPayCallback = mPayCallback;
    }


    /**
     * call alipay sdk pay. 调用SDK支付
     */
    public void pay() {
        if (TextUtils.isEmpty(PARTNER) || TextUtils.isEmpty(RSA_PRIVATE)
                || TextUtils.isEmpty(SELLER)) {
            new AlertDialog.Builder(context)
                    .setTitle("警告")
                    .setMessage("需要配置PARTNER | RSA_PRIVATE| SELLER")
                    .setPositiveButton("确定",
                            new DialogInterface.OnClickListener() {
                                public void onClick(
                                        DialogInterface dialoginterface, int i) {
                                    //
                                    activity.finish();
                                }
                            }).show();
            return;
        }
        // 订单
        String orderInfo = getOrderInfo(payInfo.getProduct_name(),
                payInfo.getProduct_desc(), payInfo.getTotal_fee());

        // 对订单做RSA 签名
        String sign = sign(orderInfo);
        try {
            // 仅需对sign 做URL编码
            sign = URLEncoder.encode(sign, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        // 完整的符合支付宝参数规范的订单信息
        final String info = orderInfo + "&sign=\"" + sign + "\"&"
                + getSignType();

        Runnable payRunnable = new Runnable() {

            @Override
            public void run() {
                // 构造PayTask 对象
                PayTask alipay = new PayTask(activity);
                // 调用支付接口，获取支付结果
                String result = alipay.pay(info);

                Message msg = new Message();
                msg.what = SDK_PAY_FLAG;
                msg.obj = result;
                handler.sendMessage(msg);
            }
        };

        // 必须异步调用
        Thread payThread = new Thread(payRunnable);
        payThread.start();
    }


    /**
     * create the order info. 创建订单信息
     */
    public String getOrderInfo(String subject, String body, String price) {

        // 签约合作者身份ID
        String orderInfo = "partner=" + "\"" + PARTNER + "\"";

        // 签约卖家支付宝账号
        orderInfo += "&seller_id=" + "\"" + SELLER + "\"";

        // 商户网站唯一订单号
        orderInfo += "&out_trade_no=" + "\"" + payInfo.getOrderNo() + "\"";

        // 商品名称
        orderInfo += "&subject=" + "\"" + subject + "\"";

        // 商品详情
        orderInfo += "&body=" + "\"" + body + "\"";

        // 商品金额
        orderInfo += "&total_fee=" + "\"" + price + "\"";

        // 服务器异步通知页面路径
        orderInfo += "&notify_url=" + "\"" + Constants.kWebServiceAPIEndpoint + "/orders/alipay_callback"
                + "\"";

        // 服务接口名称， 固定值
        orderInfo += "&service=\"mobile.securitypay.pay\"";

        // 支付类型， 固定值
        orderInfo += "&payment_type=\"1\"";

        // 参数编码， 固定值
        orderInfo += "&_input_charset=\"utf-8\"";

        // 设置未付款交易的超时时间
        // 默认30分钟，一旦超时，该笔交易就会自动被关闭。
        // 取值范围：1m～15d。
        // m-分钟，h-小时，d-天，1c-当天（无论交易何时创建，都在0点关闭）。
        // 该参数数值不接受小数点，如1.5h，可转换为90m。
        orderInfo += "&it_b_pay=\"30m\"";

        // extern_token为经过快登授权获取到的alipay_open_id,带上此参数用户将使用授权的账户进行支付
        // orderInfo += "&extern_token=" + "\"" + extern_token + "\"";

        // 支付宝处理完请求后，当前页面跳转到商户指定页面的路径，可空
        orderInfo += "&return_url=\"m.alipay.com\"";

        // 调用银行卡支付，需配置此参数，参与签名， 固定值 （需要签约《无线银行卡快捷支付》才能使用）
        // orderInfo += "&paymethod=\"expressGateway\"";

        return orderInfo;
    }

    public String sign(String content) {
        return SignUtils.sign(content, RSA_PRIVATE);
    }

    /**
     * get the sign type we use. 获取签名方式
     */
    public String getSignType() {
        return "sign_type=\"RSA\"";
    }

    public void AlipayResultProcess(Message msg) {
        switch (msg.what) {
            case SDK_PAY_FLAG:
                PayResult payResult = new PayResult((String) msg.obj);

                // 支付宝返回此次支付结果及加签，建议对支付宝签名信息拿签约时支付宝提供的公钥做验签
                String resultInfo = payResult.getResult();

                String resultStatus = payResult.getResultStatus();

                // 判断resultStatus 为“9000”则代表支付成功，具体状态码代表含义可参考接口文档
                if (TextUtils.equals(resultStatus, "9000")) {
                    Toast.makeText(context, "支付成功", Toast.LENGTH_SHORT).show();

                    mPayCallback.callback("{\"result\":true}");

                } else {
                    // 判断resultStatus 为非“9000”则代表可能支付失败
                    // “8000”代表支付结果因为支付渠道原因或者系统原因还在等待支付结果确认，最终交易是否成功以服务端异步通知为准（小概率状态）
                    if (TextUtils.equals(resultStatus, "8000")) {
                        Toast.makeText(context, "支付结果确认中", Toast.LENGTH_SHORT).show();
                        try {
                            JSONObject data = new JSONObject();
                            data.put("code", "8000");
                            mPayCallback.callback(data.toString());
                        } catch (Exception e) {

                        }
                    } else {
                        // 其他值就可以判断为支付失败，包括用户主动取消支付，或者系统返回的错误
                        Toast.makeText(context, "支付失败", Toast.LENGTH_SHORT).show();
                        try {
                            JSONObject data = new JSONObject();
                            data.put("code", resultStatus);
//                            mPayCallback.callback(data.toString());
                        } catch (Exception e) {

                        }
                        mPayCallback.callback("{\"result\":false}");
                    }
                }
                break;
            case SDK_CHECK_FLAG: {
                Toast.makeText(context, "检查结果为：" + msg.obj, Toast.LENGTH_SHORT).show();
                break;
            }
            default:
                break;
        }
    }


}
