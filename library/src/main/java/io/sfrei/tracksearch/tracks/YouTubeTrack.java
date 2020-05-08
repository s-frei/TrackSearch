package io.sfrei.tracksearch.tracks;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.sfrei.tracksearch.tracks.deserializer.YouTubeTrackDeserializer;
import io.sfrei.tracksearch.clients.setup.TrackSource;
import io.sfrei.tracksearch.tracks.metadata.YouTubeTrackInfo;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
@JsonDeserialize(using = YouTubeTrackDeserializer.class)
public class YouTubeTrack extends BaseTrack implements Track {

    private YouTubeTrackInfo trackInfo;

    public YouTubeTrack(String title, Long length, String url) {
        super(TrackSource.Youtube, title, length, url);
    }

    public YouTubeTrackInfo setAndGetTrackInfo(YouTubeTrackInfo trackInfo) {
        this.trackInfo = trackInfo;
        return this.trackInfo;
    }

}
