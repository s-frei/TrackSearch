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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
public enum MimeType {
    // SoundCloud
    AUDIO_MPEG("audio/mpeg"),
    // SoundCloud (codec OPUS)
    AUDIO_OGG("audio/ogg"),
    // YouTube
    AUDIO_MP4("audio/mp4"),
    // YouTube (codec OPUS)
    AUDIO_WEBM("audio/webm"),
    // YouTube
    VIDEO_MP4("video/mp4"),
    // YouTube
    VIDEO_WEBM("video/webm"),
    // Fallback
    UNKNOWN("unknown");

    private final String identifier;

    public static MimeType byIdentifier(String identifier) {
        final Optional<MimeType> mimeType = Arrays.stream(MimeType.values())
                .filter(type -> identifier.startsWith(type.identifier))
                .findFirst();

        if (mimeType.isPresent()) return mimeType.get();
        log.warn("Mime type '{}' is unknown to track search -> needs fix", identifier);
        return UNKNOWN;
    }

    public  boolean isVideo() {
        return identifier.contains("video");
    }

}
