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

package io.sfrei.tracksearch.tracks.deserializer.youtube;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import io.sfrei.tracksearch.clients.youtube.YouTubeClient;
import io.sfrei.tracksearch.tracks.YouTubeTrack;
import io.sfrei.tracksearch.tracks.YouTubeTrack.YouTubeTrackBuilder;
import io.sfrei.tracksearch.tracks.metadata.YouTubeTrackMetadata;
import io.sfrei.tracksearch.utils.TimeUtility;
import io.sfrei.tracksearch.utils.json.JsonElement;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.time.Duration;
import java.util.Optional;
import java.util.stream.Stream;

@Slf4j
public class YouTubeURLTrackDeserializer extends JsonDeserializer<YouTubeTrack.URLYouTubeTrackBuilder> {

    public YouTubeTrack.URLYouTubeTrackBuilder deserialize(final JsonParser p, final DeserializationContext ctxt) throws IOException {

        final JsonElement rootElement = JsonElement.of(ctxt.readTree(p));

        // Track

        final JsonElement videoDetails = rootElement.path("videoDetails");

        final String ref = videoDetails.asString("videoId");
        final String title = videoDetails.asString("title");
        final Long lengthSeconds = Long.parseLong(videoDetails.asString("lengthSeconds"));
        final Duration duration = TimeUtility.getDurationForSeconds(lengthSeconds);

        if (title == null || duration == null || ref == null)
            return null;

        final String url = YouTubeClient.URL.concat("/watch?v=").concat(ref);

        final YouTubeTrack.URLYouTubeTrackBuilder listYouTubeTrackBuilder = new YouTubeTrack.URLYouTubeTrackBuilder();
        final YouTubeTrackBuilder youTubeTrackBuilder = listYouTubeTrackBuilder.getBuilder()
                .title(title)
                .duration(duration)
                .url(url);

        // Metadata

        final JsonElement owner = rootElement.path("microformat", "playerMicroformatRenderer");

        final String channelName = owner.asString("ownerChannelName");

        final String channelUrl = owner.asString("ownerProfileUrl").replaceFirst("^http", "https");

        final long streamAmount = Long.parseLong(owner.asString("viewCount"));

        final Stream<JsonElement> thumbNailStream = owner.path("thumbnail", "thumbnails").elements();
        final Optional<JsonElement> firstThumbnail = thumbNailStream.findFirst();
        final String thumbNailUrl = firstThumbnail.map(thumbNail -> thumbNail.asString("url")).orElse(null);

        youTubeTrackBuilder.trackMetadata(YouTubeTrackMetadata.of(channelName, channelUrl, streamAmount, thumbNailUrl));

        return listYouTubeTrackBuilder;
    }

}
