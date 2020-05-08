package io.sfrei.tracksearch.utils;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class URLUtility {

    private static final String URL_SPLIT_DELIMITER = "&";

    public static Map<String, String> splitAndDecodeUrl(String url) {
        Map<String, String> params = new HashMap<>();
        try {
            String[] splitUrl = url.split(URL_SPLIT_DELIMITER);
            for (String split : splitUrl) {
                String decoded = URLDecoder.decode(split, StandardCharsets.UTF_8.name());
                String[] queryParam = decoded.split("=", 2);
                params.put(queryParam[0], queryParam[1]);
            }
        } catch (UnsupportedEncodingException ignored) {
        }
        return params;
    }

    public static String appendParam(String url, String param, String value) {
        char delimiter;
        if (!url.contains("?"))
            delimiter = '?';
        else
            delimiter = '&';
        return url.concat(delimiter + param + "=" + value);
    }

}
