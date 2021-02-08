package io.sfrei.tracksearch.tracks.metadata;

import lombok.ToString;
import lombok.Value;

@Value
@ToString
public class SoundCloudTrackMetadata implements TrackMetadata {

    String channelName;

    String channelUrl;

    Long streamAmount;

    String thumbNailUrl;

}
