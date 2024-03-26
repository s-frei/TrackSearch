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

package io.sfrei.tracksearch.clients.soundcloud;


import io.sfrei.tracksearch.clients.interfaces.ClientHelper;
import io.sfrei.tracksearch.clients.interfaces.Provider;
import io.sfrei.tracksearch.clients.setup.*;
import io.sfrei.tracksearch.config.TrackSearchConfig;
import io.sfrei.tracksearch.exceptions.SoundCloudException;
import io.sfrei.tracksearch.exceptions.TrackSearchException;
import io.sfrei.tracksearch.exceptions.UniformClientException;
import io.sfrei.tracksearch.tracks.GenericTrackList;
import io.sfrei.tracksearch.tracks.SoundCloudTrack;
import io.sfrei.tracksearch.tracks.Track;
import io.sfrei.tracksearch.tracks.TrackList;
import io.sfrei.tracksearch.utils.TrackListUtility;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import retrofit2.Call;
import retrofit2.Retrofit;

import java.net.CookiePolicy;
import java.util.*;

@Slf4j
public class SoundCloudClient extends SingleSearchClient<SoundCloudTrack>
        implements ClientHelper, Provider<SoundCloudTrack>, UniformClientException {

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

        super(CookiePolicy.ACCEPT_ALL, null);

        final Retrofit base = new Retrofit.Builder()
                .baseUrl(URL)
                .client(okHttpClient)
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

        final String trackHTML = requestRefreshingClientId(api.getForUrlWithClientID(url, clientID)).getContentOrThrow();
        final String trackURL = soundCloudUtility.extractTrackURL(trackHTML);
        final String trackJSON = requestRefreshingClientId(api.getForUrlWithClientID(trackURL, clientID)).getContentOrThrow();
        return soundCloudUtility.extractSoundCloudTrack(trackJSON, this::streamURLProvider);
    }

    private GenericTrackList<SoundCloudTrack> getTracksForSearch(final String search, int position, int offset, QueryType queryType)
            throws TrackSearchException {

        final Map<String, String> pagingParams = getPagingParams(position, offset);
        final ResponseWrapper response = requestRefreshingClientId(api.getSearchForKeywords(search, clientID, pagingParams));

        final String content = response.getContentOrThrow();
        return soundCloudUtility.extractSoundCloudTracks(content, queryType, search, this::provideNext, this::streamURLProvider);
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
            return TrackListUtility.updatePagingValues(nextTracksForSearch, trackList, POSITION_KEY, OFFSET_KEY);
        }
        throw unsupportedQueryTypeException(SoundCloudException::new, trackListQueryType);
    }

    @Override
    public String getStreamUrl(@NonNull final SoundCloudTrack soundCloudTrack) throws TrackSearchException {
        final String url = soundCloudTrack.getUrl();
        final ResponseWrapper trackResponse = requestRefreshingClientId(api.getForUrlWithClientID(url, clientID));
        final String streamUrl = soundCloudUtility.extractURLForStream(trackResponse.getContentOrThrow());
        final ResponseWrapper streamUrlResponse = requestRefreshingClientId(api.getForUrlWithClientID(streamUrl, clientID));
        return soundCloudUtility.extractStreamUrl(streamUrlResponse.getContentOrThrow());
    }

    @Override
    public String getStreamUrl(@NonNull SoundCloudTrack soundCloudTrack, final int retries) throws TrackSearchException {
        return getStreamUrl(this, soundCloudTrack, this::requestAndGetCode, retries)
                .orElseThrow(() -> noStreamUrlAfterRetriesException(SoundCloudException::new, retries));
    }

    private ResponseWrapper requestRefreshingClientId(final Call<ResponseWrapper> call) throws SoundCloudException {
        return requestRefreshingClientId(call, true);
    }

    private ResponseWrapper requestRefreshingClientId(final Call<ResponseWrapper> call, final boolean firstRequest) throws SoundCloudException {

        final ResponseWrapper response = Client.request(call);
        if (response.contentPresent() && !response.isHttpCode(Client.UNAUTHORIZED)) {
            return response;
        }

        if (firstRequest) {
            refreshClientID();
            return requestRefreshingClientId(call, false);
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
        final ResponseWrapper response = Client.request(api.getStartPage());
        final String content = response.getContentOrThrow();
        final List<String> crossOriginScripts = soundCloudUtility.getCrossOriginScripts(content);
        for (final String scriptUrl : crossOriginScripts) {
            final ResponseWrapper scriptResponse = requestURL(scriptUrl);
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
        return TrackListUtility.hasQueryInformation(trackList, POSITION_KEY, OFFSET_KEY);
    }

    @Override
    public Logger log() {
        return log;
    }

}
