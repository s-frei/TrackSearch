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
import io.sfrei.tracksearch.tracks.BaseTrackList;
import io.sfrei.tracksearch.tracks.SoundCloudTrack;
import io.sfrei.tracksearch.tracks.Track;
import io.sfrei.tracksearch.tracks.TrackList;
import io.sfrei.tracksearch.utils.TrackListHelper;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import retrofit2.Call;
import retrofit2.Retrofit;

import java.net.CookiePolicy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
public class SoundCloudClient extends SingleSearchClient<SoundCloudTrack>
        implements ClientHelper, Provider<SoundCloudTrack>, UniformClientException {

    public static final String HOSTNAME = "https://soundcloud.com";
    private static final String INFORMATION_PREFIX = "sc";
    public static final String POSITION_KEY = INFORMATION_PREFIX + TrackSearchConfig.POSITION_KEY_SUFFIX;
    public static final String OFFSET_KEY = INFORMATION_PREFIX + TrackSearchConfig.OFFSET_KEY_SUFFIX;
    private static final String PAGING_OFFSET = "limit";
    private static final String PAGING_POSITION = "position";

    private final SoundCloudAPI api;
    private final SoundCloudUtility soundCloudUtility;

    private String clientID;

    public SoundCloudClient() {

        super(CookiePolicy.ACCEPT_ALL, null);

        final Retrofit base = new Retrofit.Builder()
                .baseUrl(HOSTNAME)
                .client(okHttpClient)
                .addConverterFactory(ResponseProviderFactory.create())
                .build();

        api = base.create(SoundCloudAPI.class);
        soundCloudUtility = new SoundCloudUtility();
        refreshClientID();
    }

    public static Map<String, String> makeQueryInformation(final String query) {
        return new HashMap<>(Map.of(TrackList.QUERY_PARAM, query));
    }

    private BaseTrackList<SoundCloudTrack> getTracksForSearch(final String search, int position, int offset, QueryType queryType)
            throws TrackSearchException {

        checkClientIDAvailableOrThrow();

        final Map<String, String> pagingParams = getPagingParams(position, offset);
        final ResponseWrapper response = getSearch(search, true, pagingParams);

        final String content = response.getContentOrThrow();
        return soundCloudUtility.getSoundCloudTracks(content, queryType, search, this::provideNext, this::provideStreamUrl);
    }

    @Nullable
    public TrackList<SoundCloudTrack> provideNext(final TrackList<? extends Track> trackList) {
        try {
            return getNext(trackList);
        } catch (TrackSearchException e) {
            log.error("Error occurred acquiring next tracklist", e);
        }
        return null;
    }

    @Override
    public TrackList<SoundCloudTrack> getNext(@NonNull final TrackList<? extends Track> trackList) throws TrackSearchException {
        throwIfPagingValueMissing(this, trackList);

        final QueryType trackListQueryType = trackList.getQueryType();
        if (trackListQueryType.equals(QueryType.SEARCH) || trackListQueryType.equals(QueryType.PAGING)) {
            final int queryPosition = trackList.getQueryInformationIntValue(OFFSET_KEY);
            final int queryOffset = TrackSearchConfig.playListOffset;

            final BaseTrackList<SoundCloudTrack> nextTracksForSearch = getTracksForSearch(trackList.getQueryParam(), queryPosition, queryOffset, QueryType.PAGING);
            return TrackListHelper.updatePagingValues(nextTracksForSearch, trackList, POSITION_KEY, OFFSET_KEY);
        }
        throw unsupportedQueryTypeException(SoundCloudException::new, trackListQueryType);
    }

    @Override
    @Nullable
    public String provideStreamUrl(final SoundCloudTrack track) {
        try {
            return getStreamUrl(track, TrackSearchConfig.resolvingRetries);
        } catch (TrackSearchException e) {
            log.error("Error occurred acquiring stream URL", e);
        }
        return null;
    }

    @Override
    public String getStreamUrl(@NonNull final SoundCloudTrack soundCloudTrack) throws TrackSearchException {
        checkClientIDAvailableOrThrow();

        final ResponseWrapper trackResponse = getForUrlWitClientId(soundCloudTrack.getUrl(), true);
        final Optional<String> streamUrl = soundCloudUtility.getUrlForStream(trackResponse.getContentOrThrow());
        if (streamUrl.isEmpty())
            throw new TrackSearchException("Progressive stream URL not found");

        final ResponseWrapper streamUrlResponse = getForUrlWitClientId(streamUrl.get(), true);
        return soundCloudUtility.extractStreamUrl(streamUrlResponse.getContentOrThrow());
    }

    @Override
    public String getStreamUrl(@NonNull SoundCloudTrack soundCloudTrack, final int retries) throws TrackSearchException {
        return getStreamUrl(this, soundCloudTrack, this::requestAndGetCode, retries)
                .orElseThrow(() -> noStreamUrlAfterRetriesException(SoundCloudException::new, retries));
    }

    private ResponseWrapper getSearch(final String search, final boolean firstRequest, final Map<String, String> pagingParams) {
        final Call<ResponseWrapper> request = api.getSearchForKeywords(search, clientID, pagingParams);

        final ResponseWrapper response = Client.request(request);
        if (!firstRequest || response.hasContent() || (response.isHttpCode(Client.UNAUTHORIZED) && !refreshClientID())) {
            return response;
        }

        return getSearch(search, false, pagingParams);
    }

    private ResponseWrapper getForUrlWitClientId(final String url, final boolean firstRequest) {
        final Call<ResponseWrapper> request = api.getForUrlWithClientID(url, clientID);

        final ResponseWrapper response = Client.request(request);
        if (!firstRequest || response.hasContent() || (response.isHttpCode(Client.UNAUTHORIZED) && !refreshClientID())) {
            return response;
        }
        return getForUrlWitClientId(url, false);
    }

    private void checkClientIDAvailableOrThrow() throws TrackSearchException {
        if (clientID == null && !refreshClientID())
            throw new SoundCloudException("ClientID is not available and can not be found");
    }

    public final boolean refreshClientID() {
        log.debug("Trying to get ClientID");
        final String clientID;
        try {
            clientID = getClientID();
        } catch (TrackSearchException e) {
            log.error("Cannot refresh client ID", e);
            return false;
        }
        this.clientID = clientID;
        return true;
    }

    private String getClientID() throws TrackSearchException {
        final ResponseWrapper response = Client.request(api.getStartPage());
        final String content = response.getContentOrThrow();
        final List<String> crossOriginScripts = soundCloudUtility.getCrossOriginScripts(content);
        for (final String scriptUrl : crossOriginScripts) {
            final ResponseWrapper scriptResponse = requestURL(scriptUrl);
            if (scriptResponse.hasContent()) {
                final Optional<String> clientID = soundCloudUtility.getClientID(scriptResponse.getContent());
                if (clientID.isPresent()) {
                    return clientID.get();
                }
            }
        }
        throw new SoundCloudException("ClientId can not be found");
    }

    @Override
    public TrackList<SoundCloudTrack> getTracksForSearch(@NonNull final String search) throws TrackSearchException {
        BaseTrackList<SoundCloudTrack> trackList = getTracksForSearch(search, 0, TrackSearchConfig.playListOffset, QueryType.SEARCH);
        trackList.addQueryInformationValue(POSITION_KEY, 0);
        return trackList;
    }

    private Map<String, String> getPagingParams(final int position, final int offset) {
        return Map.of(PAGING_OFFSET, String.valueOf(offset), PAGING_POSITION, String.valueOf(position));
    }

    @Override
    public boolean hasPagingValues(@NonNull final TrackList<? extends Track> trackList) {
        return TrackListHelper.hasQueryInformation(trackList, POSITION_KEY, OFFSET_KEY);
    }

    @Override
    public Logger log() {
        return log;
    }

}
