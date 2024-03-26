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

package io.sfrei.tracksearch.tracks;

import io.sfrei.tracksearch.clients.interfaces.functional.StreamURLFunction;
import io.sfrei.tracksearch.clients.setup.TrackSource;
import io.sfrei.tracksearch.tracks.metadata.SoundCloudTrackInfo;
import io.sfrei.tracksearch.tracks.metadata.SoundCloudTrackMetadata;
import lombok.Builder;
import lombok.Getter;

import java.time.Duration;


public class SoundCloudTrack extends BaseTrack implements Track {

    @Getter
    private final SoundCloudTrackInfo trackInfo;

    @Getter
    private final SoundCloudTrackMetadata trackMetadata;

    private final StreamURLFunction<SoundCloudTrack> streamUrlFunction;

    @Builder
    public SoundCloudTrack(String title, Duration duration, String url, SoundCloudTrackInfo trackInfo,
                           SoundCloudTrackMetadata trackMetadata, StreamURLFunction<SoundCloudTrack> streamUrlFunction) {
        super(TrackSource.Soundcloud, title, duration, url);
        this.trackInfo = trackInfo;
        this.trackMetadata = trackMetadata;
        this.streamUrlFunction = streamUrlFunction;
    }

    @Override
    public String getStreamUrl() {
        return streamUrlFunction.apply(this);
    }

}
