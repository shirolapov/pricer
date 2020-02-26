package org.shirolapov.pricer.parsers.ofd;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.shirolapov.pricer.GoodsItem;
import org.shirolapov.pricer.SalesReceipt;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Taxcom {

    private String fixAddress(String address) {
        while (
                (address.charAt(address.length() - 1) == '-') |
                (address.charAt(address.length() - 1) == ' ')
        ) {
            address = address.substring(0, address.length() - 1);
        }

        return address;
    };

    public SalesReceipt getSalesReceipt(HashMap<String, String> hash) throws IOException, ParseException {
        String url = String.format("https://receipt.taxcom.ru/v01/show?fp=%s&s=%s&sf=False&sfn=False", hash.get("fp"), hash.get("s"));
        String content = Jsoup.connect(url).get().html();
        Document doc = Jsoup.parse(content);

        Elements items = doc.select("div.item");

        SalesReceipt salesReceipt = new SalesReceipt();
        List<GoodsItem> goodsItems = new ArrayList<>();

        // Get receipt name
        Element receiptNameElement = doc.selectFirst(".receipt-subtitle");
        receiptNameElement = receiptNameElement.selectFirst("b");
        String receiptName = receiptNameElement.text();
        salesReceipt.setReceipt(receiptName);

        // Get ID of taxpayer
        Element idOfTaxpayerElement = doc.selectFirst(".receipt-value-1018");
        String idOfTaxpayer = idOfTaxpayerElement.text();
        salesReceipt.setIdOfTaxpayer(idOfTaxpayer);

        // Get KKT
        Element KKTElement = doc.selectFirst(".receipt-value-1037");
        String KKT = KKTElement.text();
        salesReceipt.setKKT(KKT);

        // Get FN
        Element FNElement = doc.selectFirst(".receipt-value-1041");
        String FN = FNElement.text();
        salesReceipt.setFN(FN);

        // Get FD
        Element FDElement = doc.selectFirst(".receipt-value-1040");
        String FD = FDElement.text();
        salesReceipt.setFD(FD);

        // Get FPD
        Element FPDElement = doc.selectFirst(".receipt-value-1077");
        String FPD = FPDElement.text();
        salesReceipt.setFPD(FPD);

        // Get address
        Element AddressElement = doc.selectFirst(".receipt-value-1009");
        String address = AddressElement.text();
        address = this.fixAddress(address);
        salesReceipt.setAddress(address);

        // Get TimeDate
        Element TimeDateElement = doc.selectFirst(".receipt-value-1012");
        String timeDateString = TimeDateElement.text();
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm");
        salesReceipt.setDate(sdf.parse(timeDateString));

        float totalReceipt = 0;

        for (Element item: items) {
            try {
                Element nameElement = item.selectFirst(".receipt-value-1030");
                String name = nameElement.text();

                Element quantityElement = item.selectFirst(".receipt-value-1023");
                float quantity = Float.parseFloat(quantityElement.text());

                Element priceElement = item.selectFirst(".receipt-value-1079");
                float price = Float.parseFloat(priceElement.text());

                Element taxElement = item.selectFirst(".receipt-value-1199");
                String tax = taxElement.text();

                float sum_tax = 0;
                float total = price * quantity;

                totalReceipt = totalReceipt + total;

                if (tax.equals("НДС 10%")) {
                    sum_tax = total / 110 * 10;
                } else if (tax.equals("НДС 20%")) {
                    sum_tax = total / 120 * 20;
                }

                GoodsItem goodsItem = new GoodsItem(name, price, quantity, tax, total, sum_tax, sdf.parse(timeDateString), address);

                goodsItems.add(goodsItem);
            } catch (NumberFormatException ex) {
                ex.printStackTrace();
                System.exit(1);
            }
        }

        float totalTAX = 0;

        for (GoodsItem goods: goodsItems) {
            totalTAX = totalTAX + goods.getSum_tax();
        }

        salesReceipt.setTotalTAX(totalTAX);
        salesReceipt.setGoodsItems(goodsItems);
        salesReceipt.setTotal(totalReceipt);

        return salesReceipt;
    }
}
