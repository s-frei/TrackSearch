package io.sfrei.tracksearch.clients.soundcloud;

import io.sfrei.tracksearch.clients.ClientTestImpl;
import io.sfrei.tracksearch.tracks.SoundCloudTrack;

public class SoundCloudClientTest extends ClientTestImpl<SoundCloudTrack> {

    public SoundCloudClientTest() {
        super(new SoundCloudClient());
    }

}
