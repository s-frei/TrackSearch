package io.sfrei.tracksearch.clients.setup;

import io.sfrei.tracksearch.config.TrackSearchConfig;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;

@Slf4j
abstract class ClientProvider {

    protected static final OkHttpClient okHttpClient;

    static {
        TrackSearchConfig.setTime();

        CookieManager cookieManager = new CookieManager();
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);

        okHttpClient = new OkHttpClient.Builder()
                .addInterceptor(new CustomInterceptor())
                .cookieJar(new JavaNetCookieJar(cookieManager))
                .build();
    }

    private static void logResponseCode(String url, int code) {
        log.error("Request not successful '{}' code: {}", url, code);
    }

    protected static void logRequestException(String url, IOException e) {
        log.error("Failed to request '{}' cause: {}", url, e.getMessage());
    }

    private static final class CustomInterceptor implements Interceptor {
        @NotNull
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
