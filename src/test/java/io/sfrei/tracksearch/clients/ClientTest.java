/*
 * Copyright (C) 2024 s-frei (sfrei.io)
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

import io.sfrei.tracksearch.clients.common.SharedClient;
import io.sfrei.tracksearch.exceptions.TrackSearchException;
import io.sfrei.tracksearch.tracks.Track;
import io.sfrei.tracksearch.tracks.TrackList;
import io.sfrei.tracksearch.tracks.metadata.TrackFormat;
import io.sfrei.tracksearch.tracks.metadata.TrackMetadata;
import io.sfrei.tracksearch.tracks.metadata.TrackStream;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Condition;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static io.sfrei.tracksearch.clients.TestSuite.SEARCH_KEYS;
import static io.sfrei.tracksearch.clients.TestSuite.SINGLE_SEARCH_KEY;
import static io.sfrei.tracksearch.clients.common.SharedClient.requestAndGetCode;
import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@Slf4j
@Getter
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public abstract class ClientTest<C extends TrackSearchClient<T>, T extends Track> {

    protected final C trackSearchClient;

    private final List<String> searchKeys;

    private final List<Named<TrackList<T>>> tracksForSearch;

    private Stream<Named<Track>> getAllTracksFromTrackLists() {
        return tracksForSearch.stream()
                .flatMap(trackList -> trackList.getPayload().stream())
                .map(track -> Named.of(String.format("%s - %s", track.getTitle(), track.getUrl()), track));
    }

    public ClientTest(C searchClient, boolean single) {
        this.trackSearchClient = searchClient;
        this.searchKeys = single ? List.of(SINGLE_SEARCH_KEY) : SEARCH_KEYS;
        tracksForSearch = new ArrayList<>();
    }

    public abstract List<String> trackURLs();

    @Order(1)
    @ParameterizedTest
    @MethodSource("trackURLs")
    public void clientApplicableForURL(String url) {
        final boolean applicableForURL = trackSearchClient.isApplicableForURL(url);

        assertThat(applicableForURL)
                .as("Client is applicable for URL: %s", url)
                .isTrue();

    }

    @Order(2)
    @ParameterizedTest
    @MethodSource("trackURLs")
    public void trackForURL(String url) throws TrackSearchException {
        final Track trackForURL = trackSearchClient.getTrack(url);
        checkTrackMetadata(trackForURL);
    }

    @Order(3)
    @ParameterizedTest
    @MethodSource("getSearchKeys")
    public void tracksForSearch(String key) throws TrackSearchException {
        TrackList<T> tracksForSearch = trackSearchClient.getTracksForSearch(key);

        assertThat(tracksForSearch.isEmpty())
                .as("TrackList should contain tracks for search: %s", key)
                .isFalse();

        this.tracksForSearch.add(Named.of(tracksForSearch.getQueryValue(), tracksForSearch));
    }

    @Order(4)
    @ParameterizedTest
    @MethodSource("getTracksForSearch")
    public void trackListGotPagingValues(TrackList<Track> trackList) {
        final boolean hasPagingValues = trackSearchClient.hasPagingValues(trackList);

        assertThat(hasPagingValues)
                .as("TrackList should be pageable")
                .isTrue();
    }

    @Order(5)
    @ParameterizedTest
    @MethodSource("getTracksForSearch")
    public void getNextTracks(TrackList<Track> trackList) throws TrackSearchException {

        log.trace("Get next: {}", trackList);
        TrackList<T> nextTracksForSearch = trackSearchClient.getNext(trackList);

        assertThat(nextTracksForSearch.isEmpty())
                .as("Paged TrackList should contain tracks for: %s", trackList.getQueryInformation())
                .isFalse();

        log.trace("Get next again: {}", nextTracksForSearch);
        TrackList<T> moreTracksForSearch = nextTracksForSearch.next();

        assertThat(moreTracksForSearch.isEmpty())
                .as("Further paged TrackList should contain tracks for: %s", trackList.getQueryInformation())
                .isFalse();
    }

    @Order(6)
    @ParameterizedTest
    @MethodSource("getAllTracksFromTrackLists")
    public static void checkTrack(Track track) {
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

        assertions.assertAll();
    }

    @Order(7)
    @ParameterizedTest
    @MethodSource("getAllTracksFromTrackLists")
    public static void checkTrackInfo(Track track) {
        log.trace("{}", track.pretty());

        final SoftAssertions assertions = new SoftAssertions();
        final TrackMetadata trackMetadata = track.getTrackMetadata();

        assertions.assertThat(trackMetadata)
                .as("TrackMetadata should not be null for Track '%s'", track.getUrl())
                .isNotNull();

        assertions.assertThat(trackMetadata)
                .extracting(TrackMetadata::channelName)
                .as("TrackMetadata should have channel name for Track '%s'", track.getUrl())
                .asString()
                .isNotEmpty();

        assertions.assertThat(trackMetadata)
                .extracting(TrackMetadata::channelUrl)
                .as("TrackMetadata should have channel URL for Track '%s'", track.getUrl())
                .asString()
                .isNotEmpty();

        assertions.assertThat(trackMetadata)
                .extracting(TrackMetadata::streamAmount, as(InstanceOfAssertFactories.LONG))
                .as("TrackMetadata should have stream amount for Track '%s'", track.getUrl())
                .isNotNull()
                .isNotNegative();

        assertions.assertThat(trackMetadata)
                .extracting(TrackMetadata::thumbNailUrl)
                .as("TrackMetadata should have thumbnail URL for Track '%s'", track.getUrl())
                .asString()
                .isNotEmpty();

        assertions.assertAll();
    }

    @Order(8)
    @ParameterizedTest
    @MethodSource("getAllTracksFromTrackLists")
    public static void checkTrackMetadata(Track track) {
        log.trace("{}", track.pretty());

        final SoftAssertions assertions = new SoftAssertions();
        final List<? extends TrackFormat> formats = track.getFormats();

        assertions.assertThat(formats)
                .as("Track formats should not be null or empty for Track '%s'", track.getUrl())
                .isNotNull()
                .isNotEmpty();

        for (TrackFormat format : formats) {

            assertions.assertThat(format)
                    .extracting(TrackFormat::getMimeType)
                    .as("TrackFormat should have a mime type Track '%s'", track.getUrl())
                    .asString()
                    .isNotEmpty();

            assertions.assertThat(format)
                    .extracting(TrackFormat::getUrl)
                    .as("TrackFormat should have a URL for Track '%s'", track.getUrl())
                    .asString()
                    .isNotEmpty();

        }

        assertions.assertAll();
    }

    @Order(9)
    @ParameterizedTest
    @MethodSource("getAllTracksFromTrackLists")
    public void getTrackStream(Track track) {
        final TrackStream trackStream = assertDoesNotThrow(track::getStream,
                String.format("Track stream resolving should not throw for: %s", track.getUrl()));

        final String streamUrl = trackStream.url();
        assertThat(streamUrl)
                .as("Track stream should have stream URL for Track '%s'", track.getUrl())
                .isNotEmpty();

        assertThat(trackStream.format())
                .as("Track stream should have format for Track '%s'", track.getUrl())
                .isNotNull();

        log.trace("StreamURL: {}", streamUrl);
        @SuppressWarnings("DataFlowIssue") final int code = requestAndGetCode(streamUrl);

        assertThat(code)
                .is(new Condition<>(SharedClient::successResponseCode, "Stream URL response is 2xx"));
    }

}
