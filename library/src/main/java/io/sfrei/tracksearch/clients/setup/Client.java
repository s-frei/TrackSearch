package io.sfrei.tracksearch.clients.setup;

import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;
import retrofit2.Call;
import retrofit2.Response;

import java.io.IOException;

@Slf4j
public class Client extends ClientProvider {

    public static final int OK = 200;
    public static final int UNAUTHORIZED = 401;

    public static ResponseWrapper request(Call<ResponseWrapper> call) {
        final String url = call.request().url().toString();
        log.trace("Requesting: {}", url);
        try {
            final Response<ResponseWrapper> response = call.execute();
            return getBody(response);
        } catch (IOException e) {
            logRequestException(url, e);
            return ResponseWrapper.empty();
        }
    }

    public static ResponseWrapper requestURL(String url) {
        log.trace("Requesting: {}", url);
        final Request request = new Request.Builder().url(url).build();
        try (final okhttp3.Response response = okHttpClient.newCall(request).execute()) {
            return ResponseProviderFactory.getWrapper(response.body());
        } catch (IOException e) {
            logRequestException(url, e);
        }
        return ResponseWrapper.empty();
    }

    protected static int requestAndGetCode(String url) throws IOException {
        log.trace("Requesting: {}", url);
        final Request request = new Request.Builder().url(url).build();
        try (final okhttp3.Response response = okHttpClient.newCall(request).execute()) {
            return response.code();
        } catch (IOException e) {
            logRequestException(url, e);
            throw e;
        }
    }

    private static ResponseWrapper getBody(Response<ResponseWrapper> response) {
        if (response.isSuccessful() && response.body() != null && response.body().hasContent()) {
            return response.body();
        }
        return ResponseWrapper.of(response.raw().code(), null);
    }

}
