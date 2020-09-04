package io.sfrei.tracksearch.tracks.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import io.sfrei.tracksearch.clients.youtube.YouTubeClient;
import io.sfrei.tracksearch.tracks.YouTubeTrack;
import io.sfrei.tracksearch.utils.TimeUtility;
import io.sfrei.tracksearch.utils.json.JsonElement;

import java.io.IOException;

public class YouTubeTrackDeserializer extends StdDeserializer<YouTubeTrack> {

    @SuppressWarnings("unused")
    public YouTubeTrackDeserializer() {
        this(null);
    }

    protected YouTubeTrackDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public YouTubeTrack deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {

        JsonElement rootElement = JsonElement.of(ctxt.readTree(p).get("videoRenderer"));

        String ref = rootElement.getAsString("videoId");
        String title = rootElement.get("title", "runs").getFirstField().getAsString("text");
        String timeString = rootElement.get("lengthText").getAsString("simpleText");
        Long length = TimeUtility.getSecondsForTimeString(timeString);

        if (title == null || length == null || ref == null)
            return null;

        String url = YouTubeClient.HOSTNAME + "/watch?v=" + ref;

        return new YouTubeTrack(title, length, url);
    }

}
