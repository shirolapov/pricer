package org.shirolapov.pricer;

import java.util.Date;

public class GoodsItem {

    private String name;
    private float price;
    private float quantity;
    private float total;
    private String tax;
    private float sum_tax;
    private Date date;
    private String address;

    public String getName() { return this.name;}
    public float getPrice() { return this.price;}
    public float getQuantity() {return this.quantity;}
    public float getTotal() {return this.total;}
    public String getTax() {return this.tax;}
    public float getSum_tax() {return this.sum_tax;}
    public Date getDate() {return this.date;}
    public String getAddress() {return this.address;}

    public GoodsItem(String name, float price, float quantity, String tax, float total, Float sum_tax, Date date, String address) {
        this.name = name;
        this.price = price;
        this.quantity = quantity;
        this.tax = tax;
        this.total = total;
        this.sum_tax = sum_tax;
        this.date = date;
        this.address = address;
    }
}
