package org.shirolapov.pricer.parsers.ofd;

import org.apache.http.Consts;
import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import org.apache.http.message.BasicNameValuePair;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.shirolapov.pricer.GoodsItem;
import org.shirolapov.pricer.SalesReceipt;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class PlatformaOFD {
    public SalesReceipt getSalesReceipt(HashMap<String, String> hash) throws IOException, ParseException {
        URL salesReceiptUrl = this.getURLToReceiptOnSite(
                hash.get("fn"), hash.get("fp"), hash.get("i")
        );
        String htmlContent = this.getContextFromURL(salesReceiptUrl);
        return this.getSalesReceiptFromContent(htmlContent);
    }

    private String getContentFromResponse(CloseableHttpResponse httpResponse) throws IOException {

        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(httpResponse.getEntity().getContent()));

        String inputLine;
        StringBuilder response = new StringBuilder();

        while ((inputLine = bufferedReader.readLine()) != null) {
            response.append(inputLine);
        }
        bufferedReader.close();

        return response.toString();
    }

    private URL getURLOfCaptcha(String pageContent) throws MalformedURLException {
        Document doc = Jsoup.parse(pageContent);

        Element CaptchaImg = doc.selectFirst("#captchaImg");

        return new URL("https://lk.platformaofd.ru" + CaptchaImg.attr("src"));
    }

    private String getCSRFToken(String pageContent) {
        Document doc = Jsoup.parse(pageContent);
        Element csrf = doc.selectFirst("input[name='_csrf']");
        return csrf.attr("value");
    }

    private String getContextFromURL(URL url) throws IOException {
        return Jsoup.connect(String.valueOf(url)).get().html();
    }

    private SalesReceipt getSalesReceiptFromContent(String content) throws ParseException {
        Document doc = Jsoup.parse(content);

        SalesReceipt salesReceipt = new SalesReceipt();

        Element receiptNameElement = doc.selectFirst(".check-top");
        ArrayList<Element> elements = receiptNameElement.select("div");

        String receiptName = elements.get(1).text();
        salesReceipt.setReceipt(receiptName);

        String address = elements.get(2).text();
        salesReceipt.setAddress(address);

        String idOfTaxpayer = elements.get(3).text();
        idOfTaxpayer = idOfTaxpayer.replace("ИНН", "");
        idOfTaxpayer = idOfTaxpayer.replace(" ", "");
        salesReceipt.setIdOfTaxpayer(idOfTaxpayer);

        String timeDateString = null;
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm");

        for (Element item: doc.select(".check-row")) {
            if (item.select(".check-col-left").text().equals("регистрационный номер ККТ")) {
                salesReceipt.setKKT(item.select(".check-col-right").text());
            }

            if (item.select(".check-col-left").text().equals("N ФН")) {
                salesReceipt.setFN(item.select(".check-col-right").text());
            }

            if (item.select(".check-col-left").text().equals("N ФД")) {
                salesReceipt.setFD(item.select(".check-col-right").text());
            }

            if (item.select(".check-col-left").text().equals("ФП")) {
                salesReceipt.setFPD(item.select(".check-col-right").text());
            }

            if (item.select(".check-col-left").text().equals("Приход")) {
                timeDateString = item.select(".check-col-right").text();
                salesReceipt.setDate(sdf.parse(timeDateString));
            }
        }

        String pattern = "\\d*.\\d* х \\d*.\\d*";
        String patternTax = "10%|20%";

        List<GoodsItem> goodsItems = new ArrayList<>();

        float totalReceipt = 0;

        for (Element item: doc.select(".check-section")) {
            float quantity = 0;
            float price = 0;
            float total = 0;
            String tax = "";
            float sum_tax = 0;
            if (!item.select(".check-product-name").isEmpty()) {
                for (Element innerItem: item.select(".check-col-right")) {
                    if (innerItem.text().matches(pattern)) {
                        quantity = Float.parseFloat(innerItem.text().split(" х ")[0]);
                        price = Float.parseFloat(innerItem.text().split(" х ")[1]);
                        total = price * quantity;
                    } else if (innerItem.text().matches(patternTax)) {
                        tax = innerItem.text();
                    }
                }

                if (tax.equals("10%")) {
                    sum_tax = total / 110 * 10;
                } else if (tax.equals("20%")) {
                    sum_tax = total / 120 * 20;
                }

                totalReceipt = totalReceipt + total;

                String name = item.selectFirst(".check-product-name").text();
                GoodsItem goodsItem = new GoodsItem(name, price, quantity, tax, total, sum_tax, sdf.parse(timeDateString), address);

                goodsItems.add(goodsItem);
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

    private URL getURLToReceiptOnSite(String fn, String fpd, String fd) throws IOException {
        String url = "https://lk.platformaofd.ru/web/noauth/cheque/search";

        CookieStore cookieStore = new BasicCookieStore();
        HttpClientContext context = HttpClientContext.create();
        context.setCookieStore(cookieStore);

        CloseableHttpClient httpclient = HttpClients.createDefault();

        HttpGet httpGet = new HttpGet(url);

        CloseableHttpResponse httpResponse = httpclient.execute(httpGet, context);

        String pageContent = getContentFromResponse(httpResponse);

        String csrfText = getCSRFToken(pageContent);

        HttpGet httpGetCaptcha = new HttpGet(getURLOfCaptcha(pageContent).toString());
        CloseableHttpResponse captchaImgResponse = httpclient.execute(httpGetCaptcha, context);

        InputStream is = captchaImgResponse.getEntity().getContent();
        String filePath = "captcha.jpg";

        FileOutputStream fos = new FileOutputStream(new File(filePath));

        int inByte;
        while((inByte = is.read()) != -1)
            fos.write(inByte);
        is.close();
        fos.close();

        // Wait enter captcha
        Scanner myObj = new Scanner(System.in);
        System.out.println("Enter captcha:");
        String captcha = myObj.nextLine();

        HttpPost httpPost = new HttpPost(url);

        List<NameValuePair> params = new ArrayList<>();

        params.add(new BasicNameValuePair("fn", fn));
        params.add(new BasicNameValuePair("fp", fpd));
        params.add(new BasicNameValuePair("i", fd));
        params.add(new BasicNameValuePair("captcha", captcha));
        params.add(new BasicNameValuePair("_csrf", csrfText));
        params.add(new BasicNameValuePair("fragments", "content"));
        params.add(new BasicNameValuePair("ajaxSource", "cheque_search_form_id"));

        UrlEncodedFormEntity entity = new UrlEncodedFormEntity(params, Consts.UTF_8);
        httpPost.setEntity(entity);
        httpPost.setHeader("Referer", "https://lk.platformaofd.ru/web/noauth/cheque/search");

        CloseableHttpResponse httpResponse2 = httpclient.execute(httpPost, context);

        return new URL("https://lk.platformaofd.ru" + httpResponse2.getFirstHeader("Spring-Redirect-URL").getValue());
    }
}
