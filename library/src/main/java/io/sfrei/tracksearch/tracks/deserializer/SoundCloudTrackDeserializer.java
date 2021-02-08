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
        final String title = rootElement.getAsString("title");
        final Long length = TimeUtility.getSecondsForMilliseconds(rootElement.getLongFor("duration"));
        final String url = rootElement.getAsString("permalink_url");

        if (title == null || length == null || url == null)
            return null;

        // Metadata

        final JsonElement owner = rootElement.get("user");

        final String channelName = owner.getAsString("username");

        final String channelUrl = owner.getAsString("permalink_url");

        final Long streamAmount = rootElement.getLongFor("playback_count");

        final SoundCloudTrackMetadata trackMetadata = new SoundCloudTrackMetadata(channelName, channelUrl, streamAmount);

        final SoundCloudTrack soundcloudTrack = new SoundCloudTrack(title, length, url, trackMetadata);

        // Formats

        final List<SoundCloudTrackFormat> trackFormats = rootElement.get("media", "transcodings")
                .arrayElements()
                .map(transcoding -> {

                    final String formatUrl = transcoding.getAsString("url");
                    final String audioQuality = transcoding.getAsString("quality");

                    final JsonElement formatElement = transcoding.get("format");
                    final String mimeType = formatElement.getAsString("mime_type");
                    final String protocol = formatElement.getAsString("protocol");

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
