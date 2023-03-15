package io.sfrei.tracksearch.clients.soundcloud;

import io.sfrei.tracksearch.clients.ClientTest;
import io.sfrei.tracksearch.tracks.SoundCloudTrack;
import org.junit.jupiter.api.Tag;

@Tag("ClientTest")
public class SoundCloudClientTest extends ClientTest<SoundCloudTrack> {

    public SoundCloudClientTest() {
        super(new SoundCloudClient(), true);
    }

}
