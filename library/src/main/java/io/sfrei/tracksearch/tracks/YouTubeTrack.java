package io.sfrei.tracksearch.tracks;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.sfrei.tracksearch.clients.setup.TrackSource;
import io.sfrei.tracksearch.tracks.deserializer.YouTubeTrackDeserializer;
import io.sfrei.tracksearch.tracks.metadata.YouTubeTrackInfo;
import io.sfrei.tracksearch.tracks.metadata.YouTubeTrackMetadata;
import lombok.Getter;
import lombok.Setter;

import java.time.Duration;
import java.util.function.Function;


@JsonDeserialize(using = YouTubeTrackDeserializer.class)
public class YouTubeTrack extends BaseTrack implements Track {

    @Getter
    @Setter
    private YouTubeTrackInfo trackInfo;

    @Getter
    private final YouTubeTrackMetadata trackMetadata;

    @Setter
    private Function<YouTubeTrack, String> streamUrlProvider;

    public YouTubeTrack(String title, Duration duration, String url, YouTubeTrackMetadata trackMetadata) {
        super(TrackSource.Youtube, title, duration, url);
        this.trackMetadata = trackMetadata;
    }

    public YouTubeTrackInfo setAndGetTrackInfo(YouTubeTrackInfo trackInfo) {
        this.trackInfo = trackInfo;
        return this.trackInfo;
    }

    @Override
    public String getStreamUrl() {
        return streamUrlProvider.apply(this);
    }

}
