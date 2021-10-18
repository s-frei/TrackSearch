package io.sfrei.tracksearch.clients.soundcloud;

import io.sfrei.tracksearch.clients.ClientTestImpl;
import io.sfrei.tracksearch.tracks.SoundCloudTrack;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Tag;

import static io.sfrei.tracksearch.clients.ClientTestConstants.SEARCH_KEYS;

@Slf4j
@Tag("DetailedClientTest")
public class DetailedSoundCloudClientTest extends ClientTestImpl<SoundCloudTrack> {

    public DetailedSoundCloudClientTest() {
        super(new SoundCloudClient(), SEARCH_KEYS, log);
    }

}
