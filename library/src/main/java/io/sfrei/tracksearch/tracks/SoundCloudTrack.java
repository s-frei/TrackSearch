package io.sfrei.tracksearch.tracks;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.sfrei.tracksearch.tracks.deserializer.SoundCloudTrackDeserializer;
import io.sfrei.tracksearch.tracks.metadata.SoundCloudTrackInfo;
import io.sfrei.tracksearch.clients.setup.TrackSource;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonDeserialize(using = SoundCloudTrackDeserializer.class)
public class SoundCloudTrack extends BaseTrack implements Track {

    private SoundCloudTrackInfo trackInfo;

    public SoundCloudTrack(String title, Long length, String mrl) {
        super(TrackSource.Soundcloud, title, length, mrl);
    }

}
