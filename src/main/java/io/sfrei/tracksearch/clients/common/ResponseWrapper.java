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
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ResponseWrapper {

    private final Integer code;

    @Getter
    private final String content;

    private TrackSearchException exception;

    public static ResponseWrapper content(Integer code, @NonNull String content) {
        return new ResponseWrapper(code, content, null);
    }

    public static ResponseWrapper empty(TrackSearchException exception) {
        return new ResponseWrapper(null, null, exception);
    }

    public boolean contentPresent() {
        return content != null;
    }

    public boolean isHttpCode(int code) {
        return this.code == code;
    }

    public String contentOrThrow() throws TrackSearchException {
        if (contentPresent()) return content;
        throw exception;
    }

}
