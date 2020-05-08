package io.sfrei.tracksearch.clients.setup;

import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;

import java.io.IOException;

@Slf4j
public class Client extends ClientProvider {

    public static final int OK = 200;
    public static final int UNAUTHORIZED = 401;

    public static ResponseWrapper request(Call<ResponseWrapper> call) {
        log.trace("Requesting: " + call.request().url());
        try {
            Response<ResponseWrapper> response = call.execute();
            return getBody(response);
        } catch (IOException e) {
            log.error(e.getMessage());
            return ResponseWrapper.empty();
        }
    }

    public static ResponseWrapper requestURL(String url) {
        Request request = new Request.Builder().url(url).build();

        try {
            ResponseBody body = okHttpClient.newCall(request).execute().body();
            return ResponseProviderFactory.getWrapper(body);
        } catch (IOException e) {
            log.error(e.getMessage());
        }
        return ResponseWrapper.empty();
    }

    private static ResponseWrapper getBody(Response<ResponseWrapper> response) {
        if (response.isSuccessful() && response.body() != null && response.body().hasContent()) {
            return response.body();
        }
        return ResponseWrapper.of(response.raw().code(), null);
    }

}
