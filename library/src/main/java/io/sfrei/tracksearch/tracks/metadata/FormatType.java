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

package io.sfrei.tracksearch.tracks.metadata;

import lombok.Getter;

import java.util.Locale;

@Getter
public enum FormatType {
    Unknown("unknown"),
    Audio("audio"),  //Default
    Video("video");  //Fallback

    private final String typeDef;

    FormatType(String typeDef) {
        this.typeDef = typeDef;
    }

    public static FormatType getFormatType(String mimeTypeDef) {
        String typeDef = mimeTypeDef.toLowerCase(Locale.ROOT);
        if (typeDef.contains(Audio.getTypeDef())) {
            return Audio;
        } else if (typeDef.contains(Video.getTypeDef())) {
            return Video;
        } else
            return Unknown;
    }

}
