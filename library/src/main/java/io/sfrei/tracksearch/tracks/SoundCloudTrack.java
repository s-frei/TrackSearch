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

package io.sfrei.tracksearch.tracks;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.sfrei.tracksearch.clients.setup.TrackSource;
import io.sfrei.tracksearch.tracks.deserializer.SoundCloudTrackDeserializer;
import io.sfrei.tracksearch.tracks.metadata.SoundCloudTrackInfo;
import io.sfrei.tracksearch.tracks.metadata.SoundCloudTrackMetadata;
import lombok.Getter;
import lombok.Setter;

import java.time.Duration;
import java.util.function.Function;


@JsonDeserialize(using = SoundCloudTrackDeserializer.class)
public class SoundCloudTrack extends BaseTrack implements Track {

    @Getter
    @Setter
    private SoundCloudTrackInfo trackInfo;

    @Getter
    private final SoundCloudTrackMetadata trackMetadata;

    @Setter
    private Function<SoundCloudTrack, String> streamUrlProvider;

    public SoundCloudTrack(String title, Duration duration, String mrl, SoundCloudTrackMetadata trackMetadata) {
        super(TrackSource.Soundcloud, title, duration, mrl);
        this.trackMetadata = trackMetadata;
    }

    @Override
    public String getStreamUrl() {
        return streamUrlProvider.apply(this);
    }
}
