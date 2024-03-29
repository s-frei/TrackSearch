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

package io.sfrei.tracksearch.tracks.metadata;

import io.sfrei.tracksearch.exceptions.YouTubeException;
import lombok.Getter;

import java.util.List;

@Getter
public class YouTubeTrackInfo extends TrackInfo<YouTubeTrackFormat> {

    private final String scriptUrl;

    public YouTubeTrackInfo(List<YouTubeTrackFormat> formats, String scriptUrl) {
        super(formats);
        this.scriptUrl = scriptUrl;
    }

    public String getScriptUrlOrThrow() throws YouTubeException {
        if (scriptUrl != null) return scriptUrl;
        else throw new YouTubeException("ScriptURL was not resolved");
    }

}
