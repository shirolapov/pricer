package org.shirolapov.pricer;

import com.google.gson.Gson;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.FormatException;
import com.google.zxing.NotFoundException;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import org.apache.http.HttpHost;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.shirolapov.pricer.parsers.ofd.PlatformaOFD;
import org.shirolapov.pricer.parsers.ofd.Taxcom;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;


public class App {

    public static void main(String[] args) throws ArrayIndexOutOfBoundsException, IOException, FormatException, ChecksumException, NotFoundException, ParseException {
        String path = args[0];
        String market = args[1];
        assert path != null;
        assert market != null;

        // Parsing QR code
        BufferedImage tmpBfrImage;
        tmpBfrImage = ImageIO.read(new File(path));
        BinaryBitmap binaryBitmap = new BinaryBitmap(
                new HybridBinarizer(
                        new BufferedImageLuminanceSource(tmpBfrImage)
                )
        );
        QRCodeReader reader = new QRCodeReader();
        String TextFromCheck = reader.decode(binaryBitmap).getText();

        // Parser receipt text to hashMap
        StringParser stringParser = new StringParser();
        HashMap<String, String> dataOfReceipt = stringParser.parseString(TextFromCheck);

        // Generate id for elastic from receipt text
        byte[] bytesOfMessage = TextFromCheck.getBytes(StandardCharsets.UTF_8);
        String EncodeText = Base64.getEncoder().encodeToString(bytesOfMessage);

        // Check on exist receipt check in elastic
        RestHighLevelClient client = new RestHighLevelClient(
                RestClient.builder( new HttpHost("localhost", 9200, "http"))
        );
        GetRequest getRequest = new GetRequest("checks", EncodeText);
        boolean exists = client.exists(getRequest, RequestOptions.DEFAULT);

        // Add receipt and purchase to elastic if not exist
        if (!exists) {
            SalesReceipt salesReceipt = null;
            if (market.equals("Lenta")) {
                Taxcom taxcom = new Taxcom();
                salesReceipt = taxcom.getSalesReceipt(dataOfReceipt);
            } else if (market.equals("Megas")) {
                PlatformaOFD platformaOFD = new PlatformaOFD();
                salesReceipt = platformaOFD.getSalesReceipt(dataOfReceipt);
            }

            Gson gson = new Gson();

            assert salesReceipt != null;
            List<GoodsItem> ListOfGoodsItems = salesReceipt.getGoodsItems();

            for (GoodsItem item: ListOfGoodsItems) {
                IndexRequest requestItem = new IndexRequest("purchases");
                requestItem.source(gson.toJson(item), XContentType.JSON);
                IndexResponse indexResponseItem = client.index(requestItem, RequestOptions.DEFAULT);
                System.out.println(indexResponseItem.getId());
            }

            IndexRequest request = new IndexRequest("checks");
            request.id(EncodeText);
            request.source(gson.toJson(salesReceipt), XContentType.JSON);
            IndexResponse indexResponse = client.index(request, RequestOptions.DEFAULT);
            System.out.println(indexResponse.getId());
        } else {
            System.out.println("Receipt already in Elastic");
        }

        client.close();
    }
}