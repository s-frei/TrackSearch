package io.sfrei.tracksearch.utils;

import lombok.experimental.UtilityClass;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@UtilityClass
public class URLUtility {

    private final String URL_SPLIT_DELIMITER = "&";

    public Map<String, String> splitAndDecodeUrl(final String url) {
        final Map<String, String> params = new HashMap<>();
        try {
            final String[] splitUrl = url.split(URL_SPLIT_DELIMITER);
            for (final String split : splitUrl) {
                final String decoded = URLDecoder.decode(split, StandardCharsets.UTF_8.name());
                final String[] queryParam = decoded.split("=", 2);
                params.put(queryParam[0], queryParam[1]);
            }
        } catch (UnsupportedEncodingException ignored) {
        }
        return params;
    }

    public String appendParam(final String url, final String param, final String value) {
        final char delimiter;
        if (url.contains("?"))
            delimiter = '&';
        else
            delimiter = '?';

        return url.concat(delimiter + param + "=" + value);
    }

}
