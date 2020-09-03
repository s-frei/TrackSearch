package io.sfrei.tracksearch.clients.soundcloud;

import io.sfrei.tracksearch.clients.ClientTestImpl;
import io.sfrei.tracksearch.tracks.SoundCloudTrack;
import org.junit.jupiter.api.Tag;

import java.util.Collections;

import static io.sfrei.tracksearch.clients.ClientTestConstants.SEARCH_KEY;

@Tag("ClientTest")
public class SoundCloudClientTest extends ClientTestImpl<SoundCloudTrack> {

    public SoundCloudClientTest() {
        super(new SoundCloudClient(), Collections.singletonList(SEARCH_KEY));
    }

}
