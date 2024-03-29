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

package io.sfrei.tracksearch.config;

public final class TrackSearchConfig {

    public static final String POSITION_KEY_SUFFIX = "Position";
    public static final String OFFSET_KEY_SUFFIX = "Offset";
    public static final String HEADER_LANGUAGE_ENGLISH = "Accept-Language: en";
    public static final String HEADER_YOUTUBE_CLIENT_NAME = "x-youtube-client-name: 1";
    public static final String HEADER_YOUTUBE_CLIENT_VERSION = "x-youtube-client-version: 2.20211004.00.00";
    public static final int DEFAULT_RESOLVING_RETIRES = 4;

    public static Integer playListOffset = 20;
    public static Integer resolvingRetries = DEFAULT_RESOLVING_RETIRES;

}
