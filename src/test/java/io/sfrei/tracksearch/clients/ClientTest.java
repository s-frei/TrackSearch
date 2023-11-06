/*
 * Copyright (C) 2023 s-frei (sfrei.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.sfrei.tracksearch.clients;

import io.sfrei.tracksearch.clients.setup.Client;
import io.sfrei.tracksearch.exceptions.TrackSearchException;
import io.sfrei.tracksearch.tracks.Track;
import io.sfrei.tracksearch.tracks.TrackList;
import io.sfrei.tracksearch.tracks.metadata.TrackMetadata;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.net.CookiePolicy;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@Getter
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public abstract class ClientTest<C extends TrackSearchClient<T>, T extends Track> extends Client {

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
            "Marek Hemmann",
            "Christian Löffler"
    );

    protected final C searchClient;

    private final List<String> searchKeys;

    private final List<Named<TrackList<T>>> tracksForSearch;

    private Stream<Named<Track>> getAllTracksFromTrackLists() {
        return tracksForSearch.stream()
                .flatMap(trackList -> trackList.getPayload().stream())
                .map(track -> Named.of(String.format("%s - %s", track.getTitle(), track.getUrl()), track));
    }

    public ClientTest(C searchClient, boolean single) {
        super(CookiePolicy.ACCEPT_ALL, null);
        this.searchClient = searchClient;
        this.searchKeys = single ? List.of(SINGLE_SEARCH_KEY) : SEARCH_KEYS;
        tracksForSearch = new ArrayList<>();
    }

    @Order(1)
    @ParameterizedTest
    @MethodSource("getSearchKeys")
    public void tracksForSearch(String key) throws TrackSearchException {
        TrackList<T> tracksForSearch = searchClient.getTracksForSearch(key);

        assertThat(tracksForSearch.isEmpty())
                .as("TrackList should contain tracks")
                .isFalse();

        this.tracksForSearch.add(Named.of(tracksForSearch.getQueryValue(), tracksForSearch));
    }

    @Order(2)
    @ParameterizedTest
    @MethodSource("getTracksForSearch")
    public void trackListGotPagingValues(TrackList<Track> trackList) {
        final boolean hasPagingValues = searchClient.hasPagingValues(trackList);

        assertThat(hasPagingValues)
                .as("TrackList should be pageable")
                .isTrue();
    }

    @Order(3)
    @ParameterizedTest
    @MethodSource("getTracksForSearch")
    public void getNextTracks(TrackList<Track> trackList) throws TrackSearchException {

        log.trace("Get next: {}", trackList);
        TrackList<T> nextTracksForSearch = searchClient.getNext(trackList);

        assertThat(nextTracksForSearch.isEmpty())
                .as("Paged TrackList should contain tracks")
                .isFalse();

        log.trace("Get next again: {}", nextTracksForSearch);
        TrackList<T> moreTracksForSearch = nextTracksForSearch.next();

        assertThat(moreTracksForSearch.isEmpty())
                .as("Further paged TrackList should contain tracks")
                .isFalse();
    }

    @Order(4)
    @ParameterizedTest
    @MethodSource("getTracksForSearch")
    public void checkTrackAndMetadata(TrackList<Track> trackList) {
        for (Track track : trackList) {

            log.trace("{}", track.pretty());

            final SoftAssertions assertions = new SoftAssertions();

            assertions.assertThat(track)
                    .as("Track should not be null")
                    .isNotNull();

            assertions.assertThat(track)
                    .extracting(Track::getUrl)
                    .as("Track should have URL")
                    .asString()
                    .isNotEmpty();

            assertions.assertThat(track)
                    .extracting(Track::getTitle)
                    .as("Track should have title for '%s'", track.getUrl())
                    .asString()
                    .isNotEmpty();

            assertions.assertThat(track)
                    .extracting(Track::getDuration)
                    .as("Track should have duration for '%s'", track.getUrl())
                    .isNotNull();

            final TrackMetadata trackMetadata = track.getTrackMetadata();
            assertNotNull(trackMetadata);

            assertions.assertThat(trackMetadata)
                    .as("TrackMetadata should not be null for Track '%s'", track.getUrl())
                    .isNotNull();

            assertions.assertThat(trackMetadata)
                    .extracting(TrackMetadata::getChannelName)
                    .as("TrackMetadata should have channel name for Track '%s'", track.getUrl())
                    .asString()
                    .isNotEmpty();

            assertions.assertThat(trackMetadata)
                    .extracting(TrackMetadata::getChannelUrl)
                    .as("TrackMetadata should have channel URL for Track '%s'", track.getUrl())
                    .asString()
                    .isNotEmpty();

            assertions.assertThat(trackMetadata)
                    .extracting(TrackMetadata::getStreamAmount, as(InstanceOfAssertFactories.LONG))
                    .as("TrackMetadata should have stream amount for Track '%s'", track.getUrl())
                    .isNotNull()
                    .isNotNegative();

            assertions.assertThat(trackMetadata)
                    .extracting(TrackMetadata::getThumbNailUrl)
                    .as("TrackMetadata should have thumbnail URL for Track '%s'", track.getUrl())
                    .asString()
                    .isNotEmpty();

            assertions.assertAll();
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
