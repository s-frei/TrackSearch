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

package io.sfrei.tracksearch.clients.soundcloud;


import io.sfrei.tracksearch.clients.SearchClient;
import io.sfrei.tracksearch.clients.common.QueryType;
import io.sfrei.tracksearch.clients.common.ResponseProviderFactory;
import io.sfrei.tracksearch.clients.common.ResponseWrapper;
import io.sfrei.tracksearch.clients.common.SharedClient;
import io.sfrei.tracksearch.config.TrackSearchConfig;
import io.sfrei.tracksearch.exceptions.SoundCloudException;
import io.sfrei.tracksearch.exceptions.TrackSearchException;
import io.sfrei.tracksearch.tracks.GenericTrackList;
import io.sfrei.tracksearch.tracks.SoundCloudTrack;
import io.sfrei.tracksearch.tracks.Track;
import io.sfrei.tracksearch.tracks.TrackList;
import io.sfrei.tracksearch.tracks.metadata.SoundCloudTrackFormat;
import io.sfrei.tracksearch.tracks.metadata.TrackStream;
import io.sfrei.tracksearch.utils.TrackFormatComparator;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import retrofit2.Call;
import retrofit2.Retrofit;

import java.util.*;

import static io.sfrei.tracksearch.clients.common.SharedClient.*;

@Slf4j
public class SoundCloudClient implements SearchClient<SoundCloudTrack> {

    public static final String URL = "https://soundcloud.com";
    private static final String INFORMATION_PREFIX = "sc";
    public static final String POSITION_KEY = INFORMATION_PREFIX + TrackSearchConfig.POSITION_KEY_SUFFIX;
    public static final String OFFSET_KEY = INFORMATION_PREFIX + TrackSearchConfig.OFFSET_KEY_SUFFIX;
    private static final String PAGING_OFFSET = "limit";
    private static final String PAGING_POSITION = "position";

    private static final Set<String> VALID_URL_PREFIXES = Set.of(URL); // TODO: Extend

    private final SoundCloudAPI api;
    private final SoundCloudUtility soundCloudUtility;

    private String clientID;

    public SoundCloudClient() {

        final Retrofit base = new Retrofit.Builder()
                .baseUrl(URL)
                .client(OK_HTTP_CLIENT)
                .addConverterFactory(ResponseProviderFactory.create())
                .build();

        api = base.create(SoundCloudAPI.class);
        soundCloudUtility = new SoundCloudUtility();
        refreshClientID();
    }

    public static Map<String, String> makeQueryInformation(final String query) {
        return new HashMap<>(Map.of(TrackList.QUERY_KEY, query));
    }

    @Override
    public Set<String> validURLPrefixes() {
        return VALID_URL_PREFIXES;
    }

    @Override
    public SoundCloudTrack getTrack(@NonNull final String url) throws TrackSearchException {
        if (!isApplicableForURL(url))
            throw new SoundCloudException(String.format("%s not applicable for URL: %s", this.getClass().getSimpleName(), url));

        final String trackHTML = clientIDRequest(api.getForUrlWithClientID(url, clientID)).contentOrThrow();
        final String trackURL = soundCloudUtility.extractTrackURL(trackHTML);
        final String trackJSON = clientIDRequest(api.getForUrlWithClientID(trackURL, clientID)).contentOrThrow();
        return soundCloudUtility.extractSoundCloudTrack(trackJSON, this::trackStreamProvider);
    }

    private GenericTrackList<SoundCloudTrack> getTracksForSearch(final String search, int position, int offset, QueryType queryType)
            throws TrackSearchException {

        final Map<String, String> pagingParams = getPagingParams(position, offset);
        final String tracksJSON = clientIDRequest(api.getSearchForKeywords(search, clientID, pagingParams))
                .contentOrThrow();

        return soundCloudUtility.extractSoundCloudTracks(tracksJSON, queryType, search, this::provideNext, this::trackStreamProvider);
    }

    @Override
    public TrackList<SoundCloudTrack> getTracksForSearch(@NonNull final String search) throws TrackSearchException {
        GenericTrackList<SoundCloudTrack> trackList = getTracksForSearch(search, 0, TrackSearchConfig.playListOffset, QueryType.SEARCH);
        trackList.addQueryInformationValue(POSITION_KEY, 0);
        return trackList;
    }

    @Override
    public TrackList<SoundCloudTrack> getNext(@NonNull final TrackList<? extends Track> trackList) throws TrackSearchException {
        throwIfPagingValueMissing(this, trackList);

        final QueryType trackListQueryType = trackList.getQueryType();
        if (trackListQueryType.equals(QueryType.SEARCH) || trackListQueryType.equals(QueryType.PAGING)) {
            final int queryPosition = trackList.queryInformationAsInt(OFFSET_KEY);
            final int queryOffset = TrackSearchConfig.playListOffset;

            final GenericTrackList<SoundCloudTrack> nextTracksForSearch = getTracksForSearch(trackList.getQueryValue(), queryPosition, queryOffset, QueryType.PAGING);
            return nextTracksForSearch.updatePagingValues(trackList, POSITION_KEY, OFFSET_KEY);
        }
        throw unsupportedQueryTypeException(SoundCloudException::new, trackListQueryType);
    }

    @Override
    public TrackStream getTrackStream(@NonNull final SoundCloudTrack soundCloudTrack) throws TrackSearchException {
        final SoundCloudTrackFormat trackFormat = TrackFormatComparator.getBestSoundCloudTrackFormat(soundCloudTrack);
        final String trackFormatJSON = clientIDRequest(api.getForUrlWithClientID(trackFormat.getUrl(), clientID)).contentOrThrow();
        final String streamUrl = soundCloudUtility.extractStreamUrl(trackFormatJSON);
        return new TrackStream(streamUrl, trackFormat);
    }

    @Override
    public TrackStream getTrackStream(@NonNull SoundCloudTrack soundCloudTrack, final int retries) throws TrackSearchException {
        return tryResolveTrackStream(soundCloudTrack, retries)
                .orElseThrow(() -> noTrackStreamAfterRetriesException(SoundCloudException::new, retries));
    }

    private ResponseWrapper clientIDRequest(final Call<ResponseWrapper> call) throws SoundCloudException {
        return clientIDRequest(call, true);
    }

    private ResponseWrapper clientIDRequest(final Call<ResponseWrapper> call, final boolean firstRequest) throws SoundCloudException {

        final ResponseWrapper response = request(call);
        if (response.contentPresent() && !response.isHttpCode(UNAUTHORIZED)) {
            return response;
        }

        if (firstRequest) {
            refreshClientID();
            return clientIDRequest(call, false);
        }

        throw new SoundCloudException("ClientID is not available and cannot be refreshed");
    }

    public final void refreshClientID() {
        log.trace("Trying to get ClientID...");
        try {
            this.clientID = getClientID();
        } catch (TrackSearchException e) {
            log.error("Cannot refresh ClientID", e);
        }
    }

    private String getClientID() throws TrackSearchException {
        final ResponseWrapper response = request(api.getStartPage());
        final String content = response.contentOrThrow();
        final List<String> crossOriginScripts = soundCloudUtility.getCrossOriginScripts(content);
        for (final String scriptUrl : crossOriginScripts) {
            final ResponseWrapper scriptResponse = SharedClient.request(scriptUrl);
            if (scriptResponse.contentPresent()) {
                final Optional<String> clientID = soundCloudUtility.getClientID(scriptResponse.getContent());
                if (clientID.isPresent()) {
                    return clientID.get();
                }
            }
        }
        throw new SoundCloudException("ClientID not found");
    }

    private Map<String, String> getPagingParams(final int position, final int offset) {
        return Map.of(PAGING_OFFSET, String.valueOf(offset), PAGING_POSITION, String.valueOf(position));
    }

    @Override
    public boolean hasPagingValues(@NonNull final TrackList<? extends Track> trackList) {
        return trackList.hasQueryInformation(POSITION_KEY, OFFSET_KEY);
    }

    @Override
    public Logger log() {
        return log;
    }

}
