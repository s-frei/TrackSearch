/*
 * Copyright (C) 2024 s-frei (sfrei.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.sfrei.tracksearch.clients.common;

import io.sfrei.tracksearch.exceptions.TrackSearchException;
import io.sfrei.tracksearch.utils.UserAgent;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import retrofit2.Call;
import retrofit2.Response;

import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.util.List;

@Slf4j
public class SharedClient {

    public static final int OK = 200;
    public static final String HEADER_LANGUAGE_ENGLISH = "Accept-Language: en";
    public static final int UNAUTHORIZED = 401;
    public static final OkHttpClient OK_HTTP_CLIENT;
    private static final CookieManager COOKIE_MANAGER = new CookieManager(null, CookiePolicy.ACCEPT_NONE);

    static {

        COOKIE_MANAGER.setCookiePolicy(CookiePolicy.ACCEPT_ALL);

        OK_HTTP_CLIENT = new OkHttpClient.Builder()
                .connectionSpecs(List.of(ConnectionSpec.RESTRICTED_TLS))
                .addInterceptor(new LoggingAndHeaderInterceptor())
                .cookieJar(new JavaNetCookieJar(COOKIE_MANAGER))
                .retryOnConnectionFailure(true)
                .followRedirects(true)
                .build();
    }

    private static void logResponseCode(String url, int code) {
        log.debug("Code: {} for request not successful '{}' ", code, url);
    }

    @NotNull
    protected static TrackSearchException requestException(String url, IOException e) {
        return new TrackSearchException(String.format("Failed requesting: %s", url), e);
    }

    private static void logRequest(String url) {
        log.trace("Request: {}", url);
    }

    public static ResponseWrapper request(Call<ResponseWrapper> call) {
        final String url = call.request().url().toString();
        logRequest(url);
        try {
            final Response<ResponseWrapper> response = call.execute();

            if (response.isSuccessful() && response.body() != null && response.body().contentPresent()) {
                return response.body();
            }

            return ResponseWrapper.empty(
                    new TrackSearchException(String.format("No response body (%s) requesting: %s", response.code(), url))
            );

        } catch (IOException e) {
            return ResponseWrapper.empty(requestException(url, e));
        }
    }

    public static ResponseWrapper request(String url) {
        logRequest(url);
        final Request request = new Request.Builder().url(url).build();
        try (final okhttp3.Response response = OK_HTTP_CLIENT.newCall(request).execute()) {
            return ResponseProviderFactory.wrapResponse(response.body());
        } catch (IOException e) {
            return ResponseWrapper.empty(requestException(url, e));
        }
    }

    private static final class LoggingAndHeaderInterceptor implements Interceptor {

        @NotNull
        @Override
        public okhttp3.Response intercept(Interceptor.Chain chain) throws IOException {

            final Request.Builder modifiedRequestBuilder = chain.request()
                    .newBuilder()
                    .header("User-Agent", UserAgent.getRandom());

            final Request modifiedRequest = modifiedRequestBuilder.build();

            final String url = modifiedRequest.url().toString();

            final okhttp3.Response response;
            try {
                response = chain.proceed(modifiedRequest);
            } catch (IOException e) {
                log.error("Failed request: {}", url, e);
                throw e;
            }

            if (!response.isSuccessful())
                logResponseCode(url, response.code());

            return response;
        }
    }

}
