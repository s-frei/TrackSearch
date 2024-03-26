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

package io.sfrei.tracksearch.clients.setup;

import io.sfrei.tracksearch.clients.TrackSearchClient;
import io.sfrei.tracksearch.clients.soundcloud.SoundCloudClient;
import io.sfrei.tracksearch.clients.youtube.YouTubeClient;
import io.sfrei.tracksearch.tracks.Track;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public enum TrackSource {

    Youtube, Soundcloud;

    public static Set<TrackSource> setOf(TrackSource... sources) {
        return Arrays.stream(sources).collect(Collectors.toSet());
    }

    public <T extends Track> TrackSearchClient<T> createClient() {
        return (TrackSearchClient<T>) switch (this) {
            case Youtube -> new YouTubeClient();
            case Soundcloud -> new SoundCloudClient();
        };
    }

}
