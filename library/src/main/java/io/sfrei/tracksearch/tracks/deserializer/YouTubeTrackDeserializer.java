package io.sfrei.tracksearch.tracks.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import io.sfrei.tracksearch.clients.youtube.YouTubeClient;
import io.sfrei.tracksearch.tracks.YouTubeTrack;
import io.sfrei.tracksearch.tracks.metadata.YouTubeTrackMetadata;
import io.sfrei.tracksearch.utils.ReplaceUtility;
import io.sfrei.tracksearch.utils.TimeUtility;
import io.sfrei.tracksearch.utils.json.JsonElement;

import java.io.IOException;
import java.util.Optional;
import java.util.stream.Stream;

public class YouTubeTrackDeserializer extends StdDeserializer<YouTubeTrack> {

    @SuppressWarnings("unused")
    public YouTubeTrackDeserializer() {
        this(null);
    }

    protected YouTubeTrackDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public YouTubeTrack deserialize(final JsonParser p, final DeserializationContext ctxt) throws IOException {

        final JsonElement rootElement = JsonElement.of(ctxt.readTree(p).get("videoRenderer"));

        // Track

        final String ref = rootElement.getAsString("videoId");
        final String title = rootElement.get("title", "runs").getFirstField().getAsString("text");
        final String timeString = rootElement.get("lengthText").getAsString("simpleText");
        final Long length = TimeUtility.getSecondsForTimeString(timeString);

        if (title == null || length == null || ref == null)
            return null;

        final String url = YouTubeClient.HOSTNAME.concat("/watch?v=").concat(ref);

        // Metadata

        final JsonElement owner = rootElement.get("ownerText", "runs").getFirstField();

        final String channelName = owner.getAsString("text");

        final String channelUrlSuffix = owner.get("navigationEndpoint", "commandMetadata", "webCommandMetadata")
                .getAsString("url");
        final String channelUrl = YouTubeClient.HOSTNAME.concat(channelUrlSuffix);

        final String streamAmountText = rootElement.get("viewCountText").getAsString("simpleText");
        final String streamAmountDigits = streamAmountText != null && !streamAmountText.isEmpty() ?
                ReplaceUtility.replaceNonDigits(streamAmountText) : null;
        final Long streamAmount = streamAmountDigits != null && !streamAmountDigits.isEmpty() ?
                Long.parseLong(streamAmountDigits) : 0L;

        final Stream<JsonElement> thumbNailStream = rootElement.get("thumbnail", "thumbnails").elements();
        final Optional<JsonElement> lastThumbnail = thumbNailStream.findFirst();
        final String thumbNailUrl = lastThumbnail.map(thumbNail -> thumbNail.getAsString("url")).orElse(null);

        final YouTubeTrackMetadata trackMetadata = new YouTubeTrackMetadata(channelName, channelUrl,
                streamAmount, thumbNailUrl);

        return new YouTubeTrack(title, length, url, trackMetadata);
    }

}
