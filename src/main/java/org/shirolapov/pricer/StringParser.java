package org.shirolapov.pricer;
import java.util.HashMap;

public class StringParser {
    public HashMap<String, String> parseString(String receiptString) {
        String[] result = receiptString.split("&");

        HashMap<String, String> hashMap = new HashMap<String, String>();

        for (String a: result) {
            //System.out.println(a);

            if (a.startsWith("t=")) {
                hashMap.put("t", a.replace("t=", ""));
            } else if (a.startsWith("s=")) {
                hashMap.put("s", a.replace("s=", ""));
            } else if (a.startsWith("fn=")) {
                hashMap.put("fn", a.replace("fn=", ""));
            } else if (a.startsWith("i=")) {
                hashMap.put("i", a.replace("i=", ""));
            } else if (a.startsWith("fp=")) {
                hashMap.put("fp", a.replace("fp=", ""));
            }else if (a.startsWith("n=")) {
                hashMap.put("n", a.replace("n=", ""));
            }
        }

        return hashMap;
    }
}
