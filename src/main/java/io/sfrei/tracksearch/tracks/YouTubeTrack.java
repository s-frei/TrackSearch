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

import io.sfrei.tracksearch.clients.TrackSource;
import io.sfrei.tracksearch.tracks.metadata.YouTubeTrackMetadata;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Duration;

@Getter
public class YouTubeTrack extends BaseTrack implements Track {

    private final YouTubeTrackMetadata trackMetadata;

    @Builder
    public YouTubeTrack(String title, Duration duration, String url,
                        YouTubeTrackMetadata trackMetadata) {
        super(TrackSource.Youtube, title, duration, url);
        this.trackMetadata = trackMetadata;
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
