package io.sfrei.tracksearch.clients;

import io.sfrei.tracksearch.exceptions.TrackSearchException;

import java.io.IOException;

public interface ClientTest {

    void tracksFoSearch() throws TrackSearchException;

    void getNextTracks() throws TrackSearchException;

    void trackListGotPagingValues();

    void getStreamUrl() throws IOException;

}
