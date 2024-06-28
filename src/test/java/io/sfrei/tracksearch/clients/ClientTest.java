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

import io.sfrei.tracksearch.exceptions.TrackSearchException;
import io.sfrei.tracksearch.tracks.Track;
import io.sfrei.tracksearch.tracks.TrackList;
import io.sfrei.tracksearch.tracks.metadata.TrackMetadata;
import jdk.dynalink.linker.GuardedInvocationTransformer;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
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
import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@Getter
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public abstract class ClientTest<C extends TrackSearchClient<T>, T extends Track> {

    public static final int GH_ACTION_DELAY = 3000;
    private final boolean isGitHubAction;

    protected final C trackSearchClient;

    private final List<String> searchKeys;

    private final List<Named<TrackList<T>>> tracksForSearch;

    private Stream<Named<T>> getAllTracksFromTrackLists() {
        return tracksForSearch.stream()
                .flatMap(trackList -> trackList.getPayload().stream())
                .map(track -> Named.of(track.pretty(), track));
    }

    public ClientTest(C searchClient, boolean single) {
        this.trackSearchClient = searchClient;
        this.searchKeys = single ? List.of(SINGLE_SEARCH_KEY) : SEARCH_KEYS;
        tracksForSearch = new ArrayList<>();

        final String gitHubActionsEnv = System.getenv("GITHUB_ACTIONS");
        isGitHubAction = gitHubActionsEnv != null;

        log.info("Tests running in GitHub Actions: {}", isGitHubAction);
    }

    public abstract List<String> trackURLs();

    /**
     * Avoid to many requests 429
     */
    @SuppressWarnings("unused")
    @SneakyThrows
    private void delayWhenGitHubAction() {
        if (isGitHubAction) {
            log.trace("GitHub delay for: {}ms", GH_ACTION_DELAY);
            Thread.sleep(GH_ACTION_DELAY);
        }
    }

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
    public void checkTrackForURL(String url) throws TrackSearchException {
        log.trace("[trackForURL]: {}", url);
        final T trackForURL = trackSearchClient.getTrack(url);
        checkTrack(trackForURL);
        checkTrackMetadata(trackForURL);
    }

    @Order(3)
    @ParameterizedTest
    @MethodSource("getSearchKeys")
    public void checkTracksForSearch(String key) throws TrackSearchException {
        delayWhenGitHubAction();
        log.trace("[checkTracksForSearch]: {}", key);
        TrackList<T> tracksForSearch = trackSearchClient.getTracksForSearch(key);

        assertThat(tracksForSearch.isEmpty())
                .as("TrackList should contain tracks for search: %s", key)
                .isFalse();

        this.tracksForSearch.add(Named.of(tracksForSearch.getQueryValue(), tracksForSearch));
    }

    @Order(4)
    @ParameterizedTest
    @MethodSource("getTracksForSearch")
    public void checkPagingValuesPresent(TrackList<T> trackList) {
        log.trace("[checkPagingValuesPresent]: {}", trackList.getQueryInformation());
        final boolean hasPagingValues = trackSearchClient.hasPagingValues(trackList);

        assertThat(hasPagingValues)
                .as("TrackList should be pageable")
                .isTrue();
    }

    @Order(5)
    @ParameterizedTest
    @MethodSource("getTracksForSearch")
    public void checkNextTracks(TrackList<T> trackList) throws TrackSearchException {
        delayWhenGitHubAction();
        log.trace("[checkNextTracks]: {}", trackList.getQueryInformation());
        TrackList<T> nextTracksForSearch = trackSearchClient.getNext(trackList);

        assertThat(nextTracksForSearch.isEmpty())
                .as("Paged TrackList should contain tracks for: %s", trackList.getQueryInformation())
                .isFalse();

        delayWhenGitHubAction();
        log.trace("Get next again: {}", nextTracksForSearch);
        TrackList<T> moreTracksForSearch = nextTracksForSearch.next();

        assertThat(moreTracksForSearch.isEmpty())
                .as("Further paged TrackList should contain tracks for: %s", trackList.getQueryInformation())
                .isFalse();
    }

    @Order(6)
    @ParameterizedTest
    @MethodSource("getAllTracksFromTrackLists")
    public void checkTrack(T track) {
        log.trace("[checkTrack]: {}", track.pretty());

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
    public void checkTrackMetadata(T track) {
        log.trace("[checkTrackMetadata]: {}", track.pretty());

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

}
