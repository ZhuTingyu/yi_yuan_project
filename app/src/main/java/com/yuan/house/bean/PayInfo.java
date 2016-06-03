package com.yuan.house.bean;

/**
 * Created by LiFengYi on 16/3/4.
 */
public class PayInfo {

    private String product_name;      //商品名称
    private String product_desc;      //商品信息
    private String total_fee;       //商品总价格
    private String orderNo;         //商品订单号

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public String getProduct_name() {
        return product_name;
    }

    public void setProduct_name(String product_name) {
        this.product_name = product_name;
    }

    public String getProduct_desc() {
        return product_desc;
    }

    public void setProduct_desc(String product_desc) {
        this.product_desc = product_desc;
    }

    public String getTotal_fee() {
        return total_fee;
    }

    public void setTotal_fee(String total_fee) {
        this.total_fee = total_fee;
    }

}
