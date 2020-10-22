package io.sfrei.tracksearch.clients.setup;

import io.sfrei.tracksearch.config.TrackSearchConfig;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;

@Slf4j
class ClientProvider {

    protected static final OkHttpClient okHttpClient;

    static {
        TrackSearchConfig.load();
        okHttpClient = new OkHttpClient.Builder()
                .addInterceptor(new CustomInterceptor())
                .build();
    }

    private static void logResponseCode(String url, int code) {
        log.error("Request not successful '{}' code: {}", url, code);
    }

    protected static void logRequestException(String url, IOException e) {
        log.error("Failed to request '{}' cause: {}", url, e.getMessage());
    }

    private static final class CustomInterceptor implements Interceptor {
        @Override
        public Response intercept(Interceptor.Chain chain) throws IOException {

            final Request request = chain.request();
            final String url = request.url().toString();

            final Response response;
            try {
                response = chain.proceed(request);
            } catch (IOException e) {
                logRequestException(url, e);
                throw e;
            }

            int statusCode = response.code();
            if (statusCode != Client.OK) {
                logResponseCode(url, statusCode);
            }
            return response;
        }
    }

}
