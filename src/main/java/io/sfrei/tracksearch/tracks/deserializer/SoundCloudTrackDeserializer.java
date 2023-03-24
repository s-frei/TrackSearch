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
import io.sfrei.tracksearch.tracks.SoundCloudTrack;
import io.sfrei.tracksearch.tracks.SoundCloudTrack.SoundCloudTrackBuilder;
import io.sfrei.tracksearch.tracks.metadata.FormatType;
import io.sfrei.tracksearch.tracks.metadata.SoundCloudTrackFormat;
import io.sfrei.tracksearch.tracks.metadata.SoundCloudTrackInfo;
import io.sfrei.tracksearch.tracks.metadata.SoundCloudTrackMetadata;
import io.sfrei.tracksearch.utils.TimeUtility;
import io.sfrei.tracksearch.utils.json.JsonElement;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

public class SoundCloudTrackDeserializer extends JsonDeserializer<SoundCloudTrackBuilder> {

    public SoundCloudTrackBuilder deserialize(final JsonParser p, final DeserializationContext ctxt) throws IOException {

        // Track

        final JsonElement rootElement = JsonElement.of(ctxt.readTree(p));
        final String title = rootElement.fieldAsString("title");
        final Duration duration = TimeUtility.getDurationForMilliseconds(rootElement.fieldAsLong("duration"));
        final String url = rootElement.fieldAsString("permalink_url");

        if (title == null || duration == null || url == null)
            return null;

        final SoundCloudTrackBuilder soundCloudTrackBuilder = SoundCloudTrack.builder()
                .title(title)
                .duration(duration)
                .url(url);

        // Metadata

        final JsonElement owner = rootElement.path("user");

        final String channelName = owner.fieldAsString("username");

        final String channelUrl = owner.fieldAsString("permalink_url");

        final Long playbackCount = rootElement.fieldAsLong("playback_count");
        final Long streamAmount = playbackCount == null ? 0L : playbackCount; // Apparently can be 'null' in the JSON

        final String thumbNailUrl = rootElement.path("artwork_url")
                .orElse(rootElement)
                .path("user", "avatar_url") // Fallback to channel thumbnail
                .fieldAsString();

        soundCloudTrackBuilder.trackMetadata(SoundCloudTrackMetadata.of(channelName, channelUrl, streamAmount, thumbNailUrl));

        // Formats

        final List<SoundCloudTrackFormat> trackFormats = rootElement.path("media", "transcodings")
                .arrayElements()
                .map(SoundCloudTrackDeserializer::transcodingToTrackFormat)
                .collect(Collectors.toList());

        soundCloudTrackBuilder.trackInfo(new SoundCloudTrackInfo(trackFormats));

        return soundCloudTrackBuilder;
    }

    private static SoundCloudTrackFormat transcodingToTrackFormat(JsonElement transcoding) {

        final String formatUrl = transcoding.fieldAsString("url");
        final String audioQuality = transcoding.fieldAsString("quality");

        final JsonElement formatElement = transcoding.path("format");
        final String mimeType = formatElement.fieldAsString("mime_type");
        final String protocol = formatElement.fieldAsString("protocol");

        return SoundCloudTrackFormat.builder()
                .mimeType(mimeType)
                .formatType(FormatType.Audio)
                .audioQuality(audioQuality)
                .streamReady(true)
                .protocol(protocol)
                .url(formatUrl)
                .build();
    }

}
