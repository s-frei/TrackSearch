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
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Optional;
import java.util.stream.Stream;

@Slf4j
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

        final JsonElement rootElement = JsonElement.of(ctxt.readTree(p));

        // Track

        final String ref = rootElement.fieldAsString("videoId");
        final String title = rootElement.path("title", "runs").getFirstField().fieldAsString("text");
        final String timeString = rootElement.path("lengthText").fieldAsString("simpleText");
        final Long length = TimeUtility.getSecondsForTimeString(timeString);

        if (title == null || length == null || ref == null)
            return null;

        final String url = YouTubeClient.HOSTNAME.concat("/watch?v=").concat(ref);

        // Metadata

        final JsonElement owner = rootElement.path("ownerText", "runs").getFirstField();

        final String channelName = owner.fieldAsString("text");

        final String channelUrlSuffix = owner.path("navigationEndpoint", "commandMetadata", "webCommandMetadata")
                .fieldAsString("url");
        final String channelUrl = YouTubeClient.HOSTNAME.concat(channelUrlSuffix);

        final String streamAmountText = rootElement.path("viewCountText").fieldAsString("simpleText");
        final String streamAmountDigits = streamAmountText == null || streamAmountText.isEmpty() ?
                null : ReplaceUtility.replaceNonDigits(streamAmountText);
        final Long streamAmount = streamAmountDigits == null || streamAmountDigits.isEmpty() ?
                0L : Long.parseLong(streamAmountDigits);

        final Stream<JsonElement> thumbNailStream = rootElement.path("thumbnail", "thumbnails").elements();
        final Optional<JsonElement> lastThumbnail = thumbNailStream.findFirst();
        final String thumbNailUrl = lastThumbnail.map(thumbNail -> thumbNail.fieldAsString("url")).orElse(null);

        final YouTubeTrackMetadata trackMetadata = new YouTubeTrackMetadata(channelName, channelUrl,
                streamAmount, thumbNailUrl);

        return new YouTubeTrack(title, length, url, trackMetadata);
    }

}
