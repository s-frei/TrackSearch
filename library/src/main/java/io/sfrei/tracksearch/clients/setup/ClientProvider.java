package io.sfrei.tracksearch.clients.setup;

import io.sfrei.tracksearch.utils.UserAgentUtility;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;

@Slf4j
abstract class ClientProvider {

    protected final OkHttpClient okHttpClient;

    public ClientProvider() {
        CookieManager cookieManager = new CookieManager();
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);

        okHttpClient = new OkHttpClient.Builder()
                .addInterceptor(new LoggingAndUserAgentInterceptor())
                .cookieJar(new JavaNetCookieJar(cookieManager))
                .build();
    }

    private static void logResponseCode(String url, int code) {
        log.error("Code: {} for request not successful '{}' ", code, url);
    }

    protected static void logRequestException(String url, IOException e) {
        log.error("Failed to request '{}' cause: {}", url, e);
    }

    private static final class LoggingAndUserAgentInterceptor implements Interceptor {
        @NotNull
        @Override
        public Response intercept(Interceptor.Chain chain) throws IOException {

            final Request modifiedRequest = chain.request().newBuilder()
                    .header("user-agent", UserAgentUtility.getRandomUserAgent())
                    .build();

            final String url = modifiedRequest.url().toString();

            final Response response;
            try {
                response = chain.proceed(modifiedRequest);
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
