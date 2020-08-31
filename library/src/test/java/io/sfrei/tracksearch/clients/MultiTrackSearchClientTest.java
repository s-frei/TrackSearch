package io.sfrei.tracksearch.clients;

import io.sfrei.tracksearch.tracks.Track;

/**
 * Even tho the clients are already tested separately
 * test the async "multi" client
 */
public class MultiTrackSearchClientTest extends ClientTestImpl<Track> {

    public MultiTrackSearchClientTest() {
        super(new MultiSearchClient());
    }

}
