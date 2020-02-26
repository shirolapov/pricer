package org.shirolapov.pricer;

import org.shirolapov.pricer.GoodsItem;

import java.util.Date;
import java.util.List;

public class SalesReceipt {
    private String receipt;
    private String idOfTaxpayer;
    private String address;
    private Date date;
    private Float total;
    private String KKT;
    private String FN;
    private String FD;
    private String FPD;
    private Float totalTAX;
    private List<GoodsItem> goodsItems;

    public void setReceipt(String receipt) {
        this.receipt = receipt;
    }

    public void setIdOfTaxpayer(String idOfTaxpayer) {
        this.idOfTaxpayer = idOfTaxpayer;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public void setTotal(Float total) { this.total = total; }

    public void setKKT(String KKT) { this.KKT = KKT; }

    public void setFN(String FN) { this.FN = FN; }

    public void setFD(String FD) { this.FD = FD; }

    public void setFPD(String FPD) { this.FPD = FPD; }

    public void setTotalTAX(Float totalTAX) { this.totalTAX = totalTAX; }

    public void setGoodsItems(List<GoodsItem> goodsItems) {
        this.goodsItems = goodsItems;
    }

    public String getReceipt() {
        return receipt;
    }

    public String getIdOfTaxpayer() {
        return idOfTaxpayer;
    }

    public String getAddress() {
        return address;
    }

    public Date getDate() {
        return date;
    }

    public Float getTotal() {return this.total; }

    public String getKKT() { return this.KKT; }

    public String getFN() { return this.FN; }

    public String getFD() { return this.FD; }

    public String getFPD() { return this.FPD; }

    public List<GoodsItem> getGoodsItems() {return goodsItems; }

    public Float getTotalTAX() { return this.totalTAX; };
}
