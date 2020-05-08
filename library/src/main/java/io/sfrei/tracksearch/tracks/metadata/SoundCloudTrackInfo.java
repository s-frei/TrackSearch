package io.sfrei.tracksearch.tracks.metadata;

import lombok.Getter;

import java.util.List;

@Getter
public class SoundCloudTrackInfo extends TrackInfo<SoundCloudTrackFormat> {

    public SoundCloudTrackInfo(List<SoundCloudTrackFormat> formats) {
        super(formats);
    }

}
