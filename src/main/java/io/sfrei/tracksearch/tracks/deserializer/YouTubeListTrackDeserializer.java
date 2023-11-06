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

package io.sfrei.tracksearch.tracks.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import io.sfrei.tracksearch.clients.youtube.YouTubeClient;
import io.sfrei.tracksearch.tracks.YouTubeTrack;
import io.sfrei.tracksearch.tracks.YouTubeTrack.YouTubeTrackBuilder;
import io.sfrei.tracksearch.tracks.metadata.YouTubeTrackMetadata;
import io.sfrei.tracksearch.utils.ReplaceUtility;
import io.sfrei.tracksearch.utils.TimeUtility;
import io.sfrei.tracksearch.utils.json.JsonElement;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.time.Duration;
import java.util.Optional;
import java.util.stream.Stream;

@Slf4j
public class YouTubeListTrackDeserializer extends JsonDeserializer<YouTubeTrack.ListYouTubeTrackBuilder> {

    public YouTubeTrack.ListYouTubeTrackBuilder deserialize(final JsonParser p, final DeserializationContext ctxt) throws IOException {

        final JsonElement rootElement = JsonElement.of(ctxt.readTree(p));

        // Track

        final String ref = rootElement.asString("videoId");
        final String title = rootElement.path("title", "runs").firstElement().asString("text");
        final String timeString = rootElement.path("lengthText").asString("simpleText");
        final Duration duration = TimeUtility.getDurationForTimeString(timeString);

        if (title == null || duration == null || ref == null)
            return null;

        final String url = YouTubeClient.HOSTNAME.concat("/watch?v=").concat(ref);

        final YouTubeTrack.ListYouTubeTrackBuilder listYouTubeTrackBuilder = new YouTubeTrack.ListYouTubeTrackBuilder();
        final YouTubeTrackBuilder youTubeTrackBuilder = listYouTubeTrackBuilder.getBuilder()
                .title(title)
                .duration(duration)
                .url(url);

        // Metadata

        final JsonElement owner = rootElement.path("ownerText", "runs").firstElement();

        final String channelName = owner.asString("text");

        final String channelUrlSuffix = owner.path("navigationEndpoint", "commandMetadata", "webCommandMetadata")
                .asString("url");
        final String channelUrl = YouTubeClient.HOSTNAME.concat(channelUrlSuffix);

        final String streamAmountText = rootElement.path("viewCountText").asString("simpleText");
        final String streamAmountDigits = streamAmountText == null || streamAmountText.isEmpty() ?
                null : ReplaceUtility.replaceNonDigits(streamAmountText);
        final Long streamAmount = streamAmountDigits == null || streamAmountDigits.isEmpty() ?
                0L : Long.parseLong(streamAmountDigits);

        final Stream<JsonElement> thumbNailStream = rootElement.path("thumbnail", "thumbnails").elements();
        final Optional<JsonElement> lastThumbnail = thumbNailStream.findFirst();
        final String thumbNailUrl = lastThumbnail.map(thumbNail -> thumbNail.asString("url")).orElse(null);

        youTubeTrackBuilder.trackMetadata(YouTubeTrackMetadata.of(channelName, channelUrl, streamAmount, thumbNailUrl));

        return listYouTubeTrackBuilder;
    }

}
