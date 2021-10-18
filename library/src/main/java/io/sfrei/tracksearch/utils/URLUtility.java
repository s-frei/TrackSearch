package io.sfrei.tracksearch.utils;

import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class URLUtility {

    private static final String PARAMS_DELIMITER = "&";
    private static final Charset DECODE_CHARSET = StandardCharsets.UTF_8;

    public static String decode(String encoded) {
        return URLDecoder.decode(encoded, DECODE_CHARSET);
    }

    public static Map<String, String> splitParamsAndDecode(final String url) {
        final Map<String, String> params = new HashMap<>();
        final String[] splitUrl = url.split(PARAMS_DELIMITER);
        for (final String split : splitUrl) {

            final String decoded = decode(split);
            final String[] queryParam = decoded.split("=", 2);
            params.put(queryParam[0], queryParam[1]);
        }
        return params;
    }

    private static Map<String, String> splitQueryParams(final String url) {
        final Map<String, String> params = new HashMap<>();
        final String[] splitUrl = url.split(PARAMS_DELIMITER);
        for (final String split : splitUrl) {
            final String[] queryParam = split.split("=", 2);
            params.put(queryParam[0], queryParam[1]);
        }
        return params;
    }

    public static String addRequestParam(final String url, final String param, final String value) {

        final URI uri = URI.create(decode(url));

        final Map<String, String> queryParams = splitQueryParams(uri.getQuery());
        queryParams.put(param, URLEncoder.encode(value, StandardCharsets.UTF_8));

        final String params = queryParams.entrySet().stream()
                .map(entry -> String.format("%s=%s", entry.getKey(), entry.getValue()))
                .collect(Collectors.joining("&"));

        return String.format("%s://%s%s?%s", uri.getScheme(), uri.getHost(), uri.getPath(), params);
    }

}
