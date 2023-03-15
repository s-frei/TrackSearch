package io.sfrei.tracksearch.tracks.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import io.sfrei.tracksearch.tracks.SoundCloudTrack;
import io.sfrei.tracksearch.tracks.metadata.FormatType;
import io.sfrei.tracksearch.tracks.metadata.SoundCloudTrackFormat;
import io.sfrei.tracksearch.tracks.metadata.SoundCloudTrackInfo;
import io.sfrei.tracksearch.tracks.metadata.SoundCloudTrackMetadata;
import io.sfrei.tracksearch.utils.TimeUtility;
import io.sfrei.tracksearch.utils.json.JsonElement;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class SoundCloudTrackDeserializer extends StdDeserializer<SoundCloudTrack> {

    @SuppressWarnings("unused")
    public SoundCloudTrackDeserializer() {
        this(null);
    }

    protected SoundCloudTrackDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public SoundCloudTrack deserialize(final JsonParser p, final DeserializationContext ctxt) throws IOException {

        // Track

        final JsonElement rootElement = JsonElement.of(ctxt.readTree(p));
        final String title = rootElement.fieldAsString("title");
        final Long length = TimeUtility.getSecondsForMilliseconds(rootElement.fieldAsLong("duration"));
        final String url = rootElement.fieldAsString("permalink_url");

        if (title == null || length == null || url == null)
            return null;

        // Metadata

        final JsonElement owner = rootElement.path("user");

        final String channelName = owner.fieldAsString("username");

        final String channelUrl = owner.fieldAsString("permalink_url");

        final Long streamAmount = rootElement.fieldAsLong("playback_count");

        final String thumbNailUrl = rootElement.path("artwork_url")
                .orElse(rootElement)
                .path("user", "avatar_url") // Fallback to channel thumbnail
                .fieldAsString();

        final SoundCloudTrackMetadata trackMetadata = new SoundCloudTrackMetadata(channelName, channelUrl,
                streamAmount, thumbNailUrl);

        final SoundCloudTrack soundcloudTrack = new SoundCloudTrack(title, length, url, trackMetadata);

        // Formats

        final List<SoundCloudTrackFormat> trackFormats = rootElement.path("media", "transcodings")
                .arrayElements()
                .map(transcoding -> {

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

                }).collect(Collectors.toList());

        final SoundCloudTrackInfo trackInfo = new SoundCloudTrackInfo(trackFormats);
        soundcloudTrack.setTrackInfo(trackInfo);

        return soundcloudTrack;
    }

}
