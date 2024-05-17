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

import io.sfrei.tracksearch.clients.common.TrackSource;
import io.sfrei.tracksearch.tracks.metadata.TrackStream;
import io.sfrei.tracksearch.tracks.metadata.YouTubeTrackInfo;
import io.sfrei.tracksearch.tracks.metadata.YouTubeTrackMetadata;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Duration;

public class YouTubeTrack extends BaseTrack implements Track {

    @Getter
    @Setter
    private YouTubeTrackInfo trackInfo;

    @Getter
    private final YouTubeTrackMetadata trackMetadata;

    private final TrackStreamProvider<YouTubeTrack> trackStreamProvider;

    @Builder
    public YouTubeTrack(String title, Duration duration, String url, YouTubeTrackInfo trackInfo,
                        YouTubeTrackMetadata trackMetadata, TrackStreamProvider<YouTubeTrack> trackStreamProvider) {
        super(TrackSource.Youtube, title, duration, url);
        this.trackInfo = trackInfo;
        this.trackMetadata = trackMetadata;
        this.trackStreamProvider = trackStreamProvider;
    }

    @Override
    public TrackStream getStream() {
        return trackStreamProvider.apply(this);
    }

    @Getter
    @NoArgsConstructor
    public static class ListYouTubeTrackBuilder {

        final YouTubeTrackBuilder builder = YouTubeTrack.builder();

    }

    @Getter
    @NoArgsConstructor
    public static class URLYouTubeTrackBuilder {

        final YouTubeTrackBuilder builder = YouTubeTrack.builder();

    }

}
