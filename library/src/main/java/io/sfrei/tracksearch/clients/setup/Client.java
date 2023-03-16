/*
 * Copyright (C) 2023 s-frei (sfrei.io)
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

package io.sfrei.tracksearch.clients.setup;

import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;
import org.jetbrains.annotations.Nullable;
import retrofit2.Call;
import retrofit2.Response;

import java.io.IOException;
import java.net.CookiePolicy;
import java.util.Map;

@Slf4j
public class Client extends ClientProvider {

    public static final int OK = 200;
    public static final int PARTIAL_CONTENT = 206;
    public static final int UNAUTHORIZED = 401;
    public static final int FORBIDDEN = 403;

    public Client(@Nullable CookiePolicy cookiePolicy, @Nullable Map<String, String> headers) {
        super(cookiePolicy, headers);
    }

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

    public ResponseWrapper requestURL(String url) {
        log.trace("Requesting: {}", url);
        final Request request = new Request.Builder().url(url).build();
        try (final okhttp3.Response response = okHttpClient.newCall(request).execute()) {
            return ResponseProviderFactory.getWrapper(response.body());
        } catch (IOException e) {
            logRequestException(url, e);
        }
        return ResponseWrapper.empty();
    }

    public static boolean successResponseCode(int code) {
        return code == OK || code == PARTIAL_CONTENT;
    }

    protected int requestAndGetCode(String url) {
        final Request request = new Request.Builder().url(url)
                .header("connection", "close")
                .header("range", "bytes=0-1")
                .build();
        try (final okhttp3.Response response = okHttpClient.newCall(request).execute()) {
            return response.code();
        } catch (IOException e) {
            logRequestException(url, e);
            return 418;
        }
    }

    private static ResponseWrapper getBody(Response<ResponseWrapper> response) {
        if (response.isSuccessful() && response.body() != null && response.body().hasContent()) {
            return response.body();
        }
        return ResponseWrapper.of(response.raw().code(), null);
    }

}
