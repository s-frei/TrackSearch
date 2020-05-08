package io.sfrei.tracksearch.tracks.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import io.sfrei.tracksearch.tracks.SoundCloudTrack;
import io.sfrei.tracksearch.tracks.metadata.SoundCloudTrackFormat;
import io.sfrei.tracksearch.tracks.metadata.SoundCloudTrackInfo;
import io.sfrei.tracksearch.utils.TimeUtility;
import io.sfrei.tracksearch.utils.json.JsonElement;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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

        JsonNode rootNode = ctxt.readTree(p);
        JsonElement rootElement = new JsonElement(rootNode);
        String title = rootElement.getStringFor("title");
        Long length = TimeUtility.getSecondsForMilliseconds(rootElement.getLongFor("duration"));
        String url = rootElement.getStringFor("permalink_url");

        if (title == null || length == null || url == null)
            return null;

        SoundCloudTrack soundcloudTrack = new SoundCloudTrack(title, length, url);

        List<SoundCloudTrackFormat> trackFormats = new ArrayList<>();
        JsonNode transcodings = rootElement.getNode("media", "transcodings");
        if (transcodings.isArray()) {
            for (JsonNode node : transcodings) {
                JsonElement transcodingsElement = new JsonElement(node);
                String formatUrl = transcodingsElement.getStringFor("url");
                String audioQuality = transcodingsElement.getStringFor("quality");

                JsonElement formatElement = transcodingsElement.get("format");
                String mimeType = formatElement.getStringFor("mime_type");
                String protocol = formatElement.getStringFor("protocol");

                SoundCloudTrackFormat trackFormat = SoundCloudTrackFormat.builder()
                        .mimeType(mimeType)
                        .audioQuality(audioQuality)
                        .streamReady(false)
                        .protocol(protocol)
                        .url(formatUrl)
                        .build();
                trackFormats.add(trackFormat);
            }
        }

        SoundCloudTrackInfo trackInfo = new SoundCloudTrackInfo(trackFormats);
        soundcloudTrack.setTrackInfo(trackInfo);

        return soundcloudTrack;
    }

}
