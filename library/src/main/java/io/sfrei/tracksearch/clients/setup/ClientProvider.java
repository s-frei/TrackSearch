package io.sfrei.tracksearch.clients.setup;

import io.sfrei.tracksearch.utils.UserAgentUtility;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.List;
import java.util.Map;

@Slf4j
abstract class ClientProvider {

    protected final CookieManager COOKIE_MANAGER;
    protected final OkHttpClient okHttpClient;

    public ClientProvider(@Nullable final CookiePolicy cookiePolicy, @Nullable final Map<String, String> headers) {

        COOKIE_MANAGER = new CookieManager();
        if (cookiePolicy != null)
            COOKIE_MANAGER.setCookiePolicy(CookiePolicy.ACCEPT_ALL);

        okHttpClient = new OkHttpClient.Builder()
                .connectionSpecs(List.of(ConnectionSpec.RESTRICTED_TLS))
                .addInterceptor(new LoggingAndHeaderInterceptor(headers))
                .proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(8000)))
                .cookieJar(new JavaNetCookieJar(COOKIE_MANAGER))
                .build();
    }

    private static void logResponseCode(String url, int code) {
        log.error("Code: {} for request not successful '{}' ", code, url);
    }

    protected static void logRequestException(String url, IOException e) {
        log.error("Failed to request '{}' cause: {}", url, e);
    }

    @RequiredArgsConstructor
    private static final class LoggingAndHeaderInterceptor implements Interceptor {

        private final Map<String, String> headers;

        @NotNull
        @Override
        public Response intercept(Interceptor.Chain chain) throws IOException {

            final Request.Builder modifiedRequestBuilder = chain.request()
                    .newBuilder()
                    .header("user-agent", UserAgentUtility.getRandomUserAgent());

            if (headers != null) {
                headers.forEach(modifiedRequestBuilder::header);
            }

            final Request modifiedRequest = modifiedRequestBuilder.build();

            final String url = modifiedRequest.url().toString();

            final Response response;
            try {
                response = chain.proceed(modifiedRequest);
            } catch (IOException e) {
                logRequestException(url, e);
                throw e;
            }

            if (!response.isSuccessful())
                logResponseCode(url, response.code());

            return response;
        }
    }

}
