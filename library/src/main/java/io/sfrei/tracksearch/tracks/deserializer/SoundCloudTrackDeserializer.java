package io.sfrei.tracksearch.tracks.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import io.sfrei.tracksearch.tracks.SoundCloudTrack;
import io.sfrei.tracksearch.tracks.metadata.FormatType;
import io.sfrei.tracksearch.tracks.metadata.SoundCloudTrackFormat;
import io.sfrei.tracksearch.tracks.metadata.SoundCloudTrackInfo;
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
    public SoundCloudTrack deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {

        JsonElement rootElement = JsonElement.of(ctxt.readTree(p));
        String title = rootElement.getAsString("title");
        Long length = TimeUtility.getSecondsForMilliseconds(rootElement.getLongFor("duration"));
        String url = rootElement.getAsString("permalink_url");

        if (title == null || length == null || url == null)
            return null;

        SoundCloudTrack soundcloudTrack = new SoundCloudTrack(title, length, url);

        List<SoundCloudTrackFormat> trackFormats = rootElement.get("media", "transcodings")
                .arrayElements()
                .map(transcoding -> {

                    String formatUrl = transcoding.getAsString("url");
                    String audioQuality = transcoding.getAsString("quality");

                    JsonElement formatElement = transcoding.get("format");
                    String mimeType = formatElement.getAsString("mime_type");
                    String protocol = formatElement.getAsString("protocol");

                    return SoundCloudTrackFormat.builder()
                            .mimeType(mimeType)
                            .formatType(FormatType.Audio)
                            .audioQuality(audioQuality)
                            .streamReady(false)
                            .protocol(protocol)
                            .url(formatUrl)
                            .build();

                }).collect(Collectors.toList());

        SoundCloudTrackInfo trackInfo = new SoundCloudTrackInfo(trackFormats);
        soundcloudTrack.setTrackInfo(trackInfo);

        return soundcloudTrack;
    }

}
