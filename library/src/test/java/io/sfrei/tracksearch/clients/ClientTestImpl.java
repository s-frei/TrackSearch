package io.sfrei.tracksearch.clients;

import io.sfrei.tracksearch.clients.setup.Client;
import io.sfrei.tracksearch.exceptions.TrackSearchException;
import io.sfrei.tracksearch.tracks.Track;
import io.sfrei.tracksearch.tracks.TrackList;
import io.sfrei.tracksearch.tracks.metadata.TrackMetadata;
import lombok.Getter;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.net.CookiePolicy;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@Getter
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public abstract class ClientTestImpl<T extends Track> extends Client implements ClientTest<T> {

    protected final TrackSearchClient<T> searchClient;

    private final List<String> searchKeys;

    private final List<Named<TrackList<T>>> tracksForSearch;

    private Stream<Named<T>> getAllTracksFromTrackLists() {
        return tracksForSearch.stream()
                .flatMap(trackList -> trackList.getPayload().getTracks().stream())
                .map(track -> Named.of(String.format("%s - %s", track.getTitle(), track.getUrl()), track));
    }

    public ClientTestImpl(TrackSearchClient<T> trackSearchClient, List<String> searchKeys) {
        super(CookiePolicy.ACCEPT_ALL, null);
        searchClient = trackSearchClient;
        this.searchKeys = searchKeys;
        tracksForSearch = new ArrayList<>();
    }

    @Order(1)
    @Override
    @ParameterizedTest
    @MethodSource("getSearchKeys")
    public void tracksForSearch(String key) throws TrackSearchException {
        TrackList<T> tracksForSearch = searchClient.getTracksForSearch(key);
        assertFalse(tracksForSearch.isEmpty());
        this.tracksForSearch.add(Named.of(tracksForSearch.getQueryInformation().get(TrackList.QUERY_PARAM), tracksForSearch));

    }

    @Order(2)
    @Override
    @ParameterizedTest
    @MethodSource("getTracksForSearch")
    public void trackListGotPagingValues(TrackList<T> trackList) {
        assertTrue(searchClient.hasPagingValues(trackList));
    }

    @Order(3)
    @Override
    @ParameterizedTest
    @MethodSource("getTracksForSearch")
    public void getNextTracks(TrackList<T> trackList) throws TrackSearchException {
        TrackList<T> nextTracksForSearch = searchClient.getNext(trackList);
        assertFalse(nextTracksForSearch.isEmpty());
    }

    @Order(4)
    @Override
    @ParameterizedTest
    @MethodSource("getTracksForSearch")
    public void checkMetadata(TrackList<T> trackList) {
        for (T track : trackList.getTracks()) {

            assertNotNull(track.getTitle());
            assertNotNull(track.getLength());
            assertNotNull(track.getUrl());

            final TrackMetadata trackMetadata = track.getTrackMetadata();
            assertNotNull(trackMetadata);

            assertNotNull(trackMetadata.getChannelName());
            assertNotNull(trackMetadata.getChannelUrl());
            assertNotNull(trackMetadata.getStreamAmount());
            assertNotNull(trackMetadata.getThumbNailUrl());
        }
    }

    @Order(5)
    @Override
    @ParameterizedTest
    @MethodSource("getAllTracksFromTrackLists")
    public void getStreamUrl(Track track) {
        String streamUrl = track.getStreamUrl();
        assertNotNull(streamUrl);
        final int code = requestAndGetCode(streamUrl);
        assertNotEquals(FORBIDDEN, code);
    }

}
