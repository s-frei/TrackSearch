package io.sfrei.tracksearch.utils;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@UtilityClass
public class URLUtility {

    private final String PARAMS_DELIMITER = "&";

    public Map<String, String> decodeAndSplitParams(final String url) {
        final Map<String, String> params = new HashMap<>();
        final String[] splitUrl = url.split(PARAMS_DELIMITER);
        for (final String split : splitUrl) {

            final String decoded;
            try {
                decoded = URLDecoder.decode(split, StandardCharsets.UTF_8.name());
            } catch (UnsupportedEncodingException e) {
                log.error("Failed to decode: {}", split, e);
                continue;
            }

            final String[] queryParam = decoded.split("=", 2);
            params.put(queryParam[0], queryParam[1]);
        }
        return params;
    }

    private Map<String, String> splitQueryParams(final String url) {
        final Map<String, String> params = new HashMap<>();
        final String[] splitUrl = url.split(PARAMS_DELIMITER);
        for (final String split : splitUrl) {
            final String[] queryParam = split.split("=", 2);
            params.put(queryParam[0], queryParam[1]);
        }
        return params;
    }

    public String setParam(final String url, final String param, final String value) {

        final URI uri = URI.create(url);

        final Map<String, String> queryParams = splitQueryParams(uri.getQuery());
        queryParams.put(param, value);

        final String params = queryParams.entrySet().stream()
                .map(entry -> String.format("%s=%s", entry.getKey(), entry.getValue()))
                .collect(Collectors.joining("&"));

        return String.format("%s://%s%s?%s", uri.getScheme(), uri.getHost(), uri.getPath(), params);
    }

}
