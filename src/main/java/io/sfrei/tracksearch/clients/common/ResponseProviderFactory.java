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
import lombok.extern.slf4j.Slf4j;
import okhttp3.ResponseBody;
import org.jetbrains.annotations.NotNull;
import retrofit2.Converter;
import retrofit2.Retrofit;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

@Slf4j
public class ResponseProviderFactory extends Converter.Factory {

    public static ResponseProviderFactory create() {
        return new ResponseProviderFactory();
    }

    public static ResponseWrapper wrapResponse(ResponseBody responseBody) {
        if (responseBody != null) {
            try {
                String body = new String(responseBody.string().getBytes(StandardCharsets.UTF_8));
                return ResponseWrapper.content(SharedClient.OK, body);
            } catch (IOException e) {
                return ResponseWrapper.empty(new TrackSearchException("Cannot process response", e));
            }
        }
        return ResponseWrapper.empty(new TrackSearchException("No response body"));
    }

    @Override
    public Converter<ResponseBody, ?> responseBodyConverter(@NotNull Type type,
                                                            @NotNull Annotation[] annotations,
                                                            @NotNull Retrofit retrofit) {
        return StringProvider.INSTANCE;
    }

    @Slf4j
    final static class StringProvider implements Converter<ResponseBody, ResponseWrapper> {
        static final StringProvider INSTANCE = new StringProvider();

        @Override
        public ResponseWrapper convert(@NotNull ResponseBody responseBody) {
            return wrapResponse(responseBody);
        }
    }

}
