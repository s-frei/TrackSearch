package io.sfrei.tracksearch.tracks;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.sfrei.tracksearch.clients.setup.TrackSource;
import io.sfrei.tracksearch.tracks.deserializer.SoundCloudTrackDeserializer;
import io.sfrei.tracksearch.tracks.metadata.SoundCloudTrackInfo;
import io.sfrei.tracksearch.tracks.metadata.SoundCloudTrackMetadata;
import lombok.Getter;
import lombok.Setter;

import java.util.function.Function;


@JsonDeserialize(using = SoundCloudTrackDeserializer.class)
public class SoundCloudTrack extends BaseTrack implements Track {

    @Getter
    @Setter
    private SoundCloudTrackInfo trackInfo;

    @Getter
    private final SoundCloudTrackMetadata trackMetadata;

    @Setter
    private Function<SoundCloudTrack, String> streamUrlProvider;

    public SoundCloudTrack(String title, Long length, String mrl, SoundCloudTrackMetadata trackMetadata) {
        super(TrackSource.Soundcloud, title, length, mrl);
        this.trackMetadata = trackMetadata;
    }

    @Override
    public String getStreamUrl() {
        return streamUrlProvider.apply(this);
    }
}
