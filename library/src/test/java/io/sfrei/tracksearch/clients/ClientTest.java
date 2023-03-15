package io.sfrei.tracksearch.clients;

import io.sfrei.tracksearch.clients.setup.Client;
import io.sfrei.tracksearch.exceptions.TrackSearchException;
import io.sfrei.tracksearch.tracks.Track;
import io.sfrei.tracksearch.tracks.TrackList;
import io.sfrei.tracksearch.tracks.metadata.TrackMetadata;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.net.CookiePolicy;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@Getter
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public abstract class ClientTest<T extends Track> extends Client {

    protected static final String SINGLE_SEARCH_KEY = "Ben Böhmer";

    private static final List<String> SEARCH_KEYS = List.of(
            SINGLE_SEARCH_KEY,
            "Tale Of Us",
            "Hans Zimmer",
            "Paul Kalkbrenner",
            "Einmusik",
            "Mind Against",
            "Adriatique",
            "Fideles",
            "Marek Hemann",
            "Christian Löffler"
    );

    protected final TrackSearchClient<T> searchClient;

    private final List<String> searchKeys;

    private final List<Named<TrackList<T>>> tracksForSearch;

    private Stream<Named<T>> getAllTracksFromTrackLists() {
        return tracksForSearch.stream()
                .flatMap(trackList -> trackList.getPayload().getTracks().stream())
                .map(track -> Named.of(String.format("%s - %s", track.getTitle(), track.getUrl()), track));
    }

    public ClientTest(TrackSearchClient<T> trackSearchClient, boolean single) {
        super(CookiePolicy.ACCEPT_ALL, null);
        searchClient = trackSearchClient;
        this.searchKeys = single ? List.of(SINGLE_SEARCH_KEY) : SEARCH_KEYS;
        tracksForSearch = new ArrayList<>();
    }

    @Order(1)
    @ParameterizedTest
    @MethodSource("getSearchKeys")
    public void tracksForSearch(String key) throws TrackSearchException {
        TrackList<T> tracksForSearch = searchClient.getTracksForSearch(key);
        assertFalse(tracksForSearch.isEmpty());

        assertThat(tracksForSearch.isEmpty())
                .as("TrackList should contain tracks")
                .isFalse();

        this.tracksForSearch.add(Named.of(tracksForSearch.getQueryInformation().get(TrackList.QUERY_PARAM), tracksForSearch));
    }

    @Order(2)
    @ParameterizedTest
    @MethodSource("getTracksForSearch")
    public void trackListGotPagingValues(TrackList<T> trackList) {
        final boolean hasPagingValues = searchClient.hasPagingValues(trackList);

        assertThat(hasPagingValues)
                .as("TrackList should be pageable")
                .isTrue();
    }

    @Order(3)
    @ParameterizedTest
    @MethodSource("getTracksForSearch")
    public void getNextTracks(TrackList<T> trackList) throws TrackSearchException {
        TrackList<T> nextTracksForSearch = searchClient.getNext(trackList);

        assertThat(nextTracksForSearch.isEmpty())
                .as("Paged TrackList should contain tracks")
                .isFalse();
    }

    @Order(4)
    @ParameterizedTest
    @MethodSource("getTracksForSearch")
    public void checkTrackAndMetadata(TrackList<T> trackList) {
        for (T track : trackList.getTracks()) {

            assertThat(track)
                    .as("Track should not be null")
                    .isNotNull();

            assertThat(track)
                    .extracting(Track::getUrl)
                    .as("Track should have URL")
                    .asString()
                    .isNotEmpty();

            assertThat(track)
                    .extracting(Track::getTitle)
                    .as("Track should have title for '%s'", track.getUrl())
                    .asString()
                    .isNotEmpty();

            assertThat(track)
                    .extracting(Track::getLength)
                    .as("Track should have length for '%s'", track.getUrl())
                    .isNotNull();

            final TrackMetadata trackMetadata = track.getTrackMetadata();
            assertNotNull(trackMetadata);

            assertThat(trackMetadata)
                    .as("TrackMetadata should not be null for Track '%s'", track.getUrl())
                    .isNotNull();

            assertThat(trackMetadata)
                    .extracting(TrackMetadata::getChannelName)
                    .as("TrackMetadata should have channel name for Track '%s'", track.getUrl())
                    .asString()
                    .isNotEmpty();

            assertThat(trackMetadata)
                    .extracting(TrackMetadata::getChannelUrl)
                    .as("TrackMetadata should have channel URL for Track '%s'", track.getUrl())
                    .asString()
                    .isNotEmpty();

            assertThat(trackMetadata)
                    .extracting(TrackMetadata::getStreamAmount)
                    .as("TrackMetadata should have stream amount for Track '%s'", track.getUrl())
                    .asString()
                    .isNotEmpty();

            assertThat(trackMetadata)
                    .extracting(TrackMetadata::getThumbNailUrl)
                    .as("TrackMetadata should have thumbnail URL for Track '%s'", track.getUrl())
                    .asString()
                    .isNotEmpty();
        }
    }

    @Order(5)
    @ParameterizedTest
    @MethodSource("getAllTracksFromTrackLists")
    public void getStreamUrl(Track track) {
        String streamUrl = track.getStreamUrl();

        assertThat(streamUrl)
                .as("Track '%s' should have stream for Track '%s'", track.getUrl())
                .isNotEmpty();

        final int code = requestAndGetCode(streamUrl);
        assertNotEquals(FORBIDDEN, code);
    }

}
