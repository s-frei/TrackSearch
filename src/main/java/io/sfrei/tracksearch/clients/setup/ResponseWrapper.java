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

import io.sfrei.tracksearch.exceptions.ResponseException;
import io.sfrei.tracksearch.exceptions.TrackSearchException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
public class ResponseWrapper {

    private int code;
    private String content;

    public boolean hasContent() {
        return content != null;
    }

    public boolean isHttpCode(int code) {
        return this.code == code;
    }

    public static ResponseWrapper empty() {
        return new ResponseWrapper();
    }

    public String getContentOrThrow() throws TrackSearchException {
        if (hasContent())
            return content;
        throw new ResponseException("No content - code: " + code);
    }

    public String getContent() {
        return this.content;
    }

}
