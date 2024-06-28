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

package io.sfrei.tracksearch.clients.youtube;

import io.sfrei.tracksearch.clients.SearchClient;
import io.sfrei.tracksearch.clients.common.QueryType;
import io.sfrei.tracksearch.clients.common.ResponseProviderFactory;
import io.sfrei.tracksearch.config.TrackSearchConfig;
import io.sfrei.tracksearch.exceptions.TrackSearchException;
import io.sfrei.tracksearch.exceptions.YouTubeException;
import io.sfrei.tracksearch.tracks.GenericTrackList;
import io.sfrei.tracksearch.tracks.Track;
import io.sfrei.tracksearch.tracks.TrackList;
import io.sfrei.tracksearch.tracks.YouTubeTrack;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import retrofit2.Retrofit;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static io.sfrei.tracksearch.clients.common.SharedClient.OK_HTTP_CLIENT;
import static io.sfrei.tracksearch.clients.common.SharedClient.request;

@Slf4j
public class YouTubeClient implements SearchClient<YouTubeTrack> {

    public static final String URL = "https://www.youtube.com";
    public static final String PAGING_KEY = "ctoken";
    private static final String INFORMATION_PREFIX = "yt";
    public static final String POSITION_KEY = INFORMATION_PREFIX + TrackSearchConfig.POSITION_KEY_SUFFIX;
    public static final String OFFSET_KEY = INFORMATION_PREFIX + TrackSearchConfig.OFFSET_KEY_SUFFIX;
    private static final String PAGING_INFORMATION = INFORMATION_PREFIX + "PagingToken";
    private static final String ADDITIONAL_PAGING_KEY = "continuation";

    private static final Set<String> VALID_URL_PREFIXES = Set.of(URL); // TODO: Extend

    private final YouTubeAPI api;

    public YouTubeClient() {

        final Retrofit base = new Retrofit.Builder()
                .baseUrl(URL)
                .client(OK_HTTP_CLIENT)
                .addConverterFactory(ResponseProviderFactory.create())
                .build();

        api = base.create(YouTubeAPI.class);
    }

    public static Map<String, String> makeQueryInformation(final String query, final String pagingToken) {
        return new HashMap<>(Map.of(TrackList.QUERY_KEY, query, PAGING_INFORMATION, pagingToken));
    }

    @Override
    public Set<String> validURLPrefixes() {
        return VALID_URL_PREFIXES;
    }

    @Override
    public YouTubeTrack getTrack(@NonNull final String url) throws TrackSearchException {
        if (!isApplicableForURL(url))
            throw new YouTubeException(String.format("%s not applicable for URL: %s", this.getClass().getSimpleName(), url));

        final String trackJSON = request(api.getForUrlWithParams(url, Map.of())).contentOrThrow();
        return YouTubeUtility.extractYouTubeTrack(trackJSON);
    }

    private GenericTrackList<YouTubeTrack> getTracksForSearch(@NonNull final String search, @NonNull final Map<String, String> params, QueryType queryType)
            throws TrackSearchException {

        final String tracksJSON = request(api.getSearchForKeywords(search, params)).contentOrThrow();
        return YouTubeUtility.extractYouTubeTracks(tracksJSON, queryType, search, this::provideNext);
    }

    @Override
    public TrackList<YouTubeTrack> getTracksForSearch(@NonNull final String search) throws TrackSearchException {
        final TrackList<YouTubeTrack> trackList = getTracksForSearch(search, Map.of(), QueryType.SEARCH);
        trackList.addQueryInformationValue(POSITION_KEY, 0);
        return trackList;
    }

    @Override
    public TrackList<YouTubeTrack> getNext(@NonNull final TrackList<? extends Track> trackList) throws TrackSearchException {
        throwIfPagingValueMissing(this, trackList);

        final QueryType trackListQueryType = trackList.getQueryType();
        if (trackListQueryType.equals(QueryType.SEARCH) || trackListQueryType.equals(QueryType.PAGING)) {
            final Map<String, String> paginParams = getPagingParams(trackList.getQueryInformation());

            final GenericTrackList<YouTubeTrack> nextTracksForSearch = getTracksForSearch(trackList.getQueryValue(), paginParams, QueryType.PAGING);
            return nextTracksForSearch.updatePagingValues(trackList, POSITION_KEY, OFFSET_KEY);
        }
        throw unsupportedQueryTypeException(YouTubeException::new, trackListQueryType);
    }

    private Map<String, String> getPagingParams(final Map<String, String> queryInformation) {
        final String pagingToken = queryInformation.get(PAGING_INFORMATION);
        return Map.of(PAGING_KEY, pagingToken, ADDITIONAL_PAGING_KEY, pagingToken);
    }

    @Override
    public boolean hasPagingValues(@NonNull final TrackList<? extends Track> trackList) {
        return trackList.hasQueryInformation(POSITION_KEY, OFFSET_KEY, PAGING_INFORMATION);
    }

    @Override
    public Logger log() {
        return log;
    }

}
