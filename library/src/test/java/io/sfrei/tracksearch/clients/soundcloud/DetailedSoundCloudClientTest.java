package io.sfrei.tracksearch.clients.soundcloud;

import io.sfrei.tracksearch.clients.ClientTest;
import io.sfrei.tracksearch.tracks.SoundCloudTrack;
import org.junit.jupiter.api.Tag;

@Tag("DetailedClientTest")
public class DetailedSoundCloudClientTest extends ClientTest<SoundCloudTrack> {

    public DetailedSoundCloudClientTest() {
        super(new SoundCloudClient(), false);
    }

}
