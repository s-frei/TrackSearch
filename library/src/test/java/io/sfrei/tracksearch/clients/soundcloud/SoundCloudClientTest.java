package io.sfrei.tracksearch.clients.soundcloud;

import io.sfrei.tracksearch.clients.ClientTestImpl;
import io.sfrei.tracksearch.tracks.SoundCloudTrack;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Tag;

import java.util.Collections;

import static io.sfrei.tracksearch.clients.ClientTestConstants.DEFAULT_SEARCH_KEY;

@Slf4j
@Tag("ClientTest")
public class SoundCloudClientTest extends ClientTestImpl<SoundCloudTrack> {

    public SoundCloudClientTest() {
        super(new SoundCloudClient(), Collections.singletonList(DEFAULT_SEARCH_KEY), log);
    }

}
