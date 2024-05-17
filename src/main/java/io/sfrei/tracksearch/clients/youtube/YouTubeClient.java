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
import io.sfrei.tracksearch.clients.common.SharedClient;
import io.sfrei.tracksearch.config.TrackSearchConfig;
import io.sfrei.tracksearch.exceptions.TrackSearchException;
import io.sfrei.tracksearch.exceptions.YouTubeException;
import io.sfrei.tracksearch.tracks.GenericTrackList;
import io.sfrei.tracksearch.tracks.Track;
import io.sfrei.tracksearch.tracks.TrackList;
import io.sfrei.tracksearch.tracks.YouTubeTrack;
import io.sfrei.tracksearch.tracks.metadata.TrackStream;
import io.sfrei.tracksearch.tracks.metadata.YouTubeTrackFormat;
import io.sfrei.tracksearch.tracks.metadata.YouTubeTrackInfo;
import io.sfrei.tracksearch.utils.CacheMap;
import io.sfrei.tracksearch.utils.TrackFormatComparator;
import io.sfrei.tracksearch.utils.URLModifier;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import retrofit2.Retrofit;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    private static final Map<String, String> VIDEO_SEARCH_PARAMS = Map.of("sp", "EgIQAQ%3D%3D");
    public static final Map<String, String> TRACK_PARAMS = Map.of("pbj", "1", "hl", "en", "alt", "json");

    private static final Map<String, String> DEFAULT_SEARCH_PARAMS = Stream.of(VIDEO_SEARCH_PARAMS.entrySet(), TRACK_PARAMS.entrySet())
            .flatMap(Set::stream)
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    private static final Set<String> VALID_URL_PREFIXES = Set.of(URL); // TODO: Extend

    private final YouTubeAPI api;

    private final CacheMap<String, String> scriptCache;

    public YouTubeClient() {

        final Retrofit base = new Retrofit.Builder()
                .baseUrl(URL)
                .client(OK_HTTP_CLIENT)
                .addConverterFactory(ResponseProviderFactory.create())
                .build();

        api = base.create(YouTubeAPI.class);
        scriptCache = new CacheMap<>();
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

        final String trackJSON = request(api.getForUrlWithParams(url, TRACK_PARAMS)).contentOrThrow();
        final YouTubeTrack youTubeTrack = YouTubeUtility.extractYouTubeTrack(trackJSON, this::trackStreamProvider);
        final YouTubeTrackInfo trackInfo = YouTubeUtility.extractTrackInfo(trackJSON, url);
        youTubeTrack.setTrackInfo(trackInfo);
        return youTubeTrack;
    }

    private GenericTrackList<YouTubeTrack> getTracksForSearch(@NonNull final String search, @NonNull final Map<String, String> params, QueryType queryType)
            throws TrackSearchException {

        final String tracksJSON = request(api.getSearchForKeywords(search, params)).contentOrThrow();
        return YouTubeUtility.extractYouTubeTracks(tracksJSON, queryType, search, this::provideNext, this::trackStreamProvider);
    }

    @Override
    public TrackList<YouTubeTrack> getTracksForSearch(@NonNull final String search) throws TrackSearchException {
        final TrackList<YouTubeTrack> trackList = getTracksForSearch(search, DEFAULT_SEARCH_PARAMS, QueryType.SEARCH);
        trackList.addQueryInformationValue(POSITION_KEY, 0);
        return trackList;
    }

    @Override
    public TrackList<YouTubeTrack> getNext(@NonNull final TrackList<? extends Track> trackList) throws TrackSearchException {
        throwIfPagingValueMissing(this, trackList);

        final QueryType trackListQueryType = trackList.getQueryType();
        if (trackListQueryType.equals(QueryType.SEARCH) || trackListQueryType.equals(QueryType.PAGING)) {
            final HashMap<String, String> params = new HashMap<>();
            params.putAll(getPagingParams(trackList.getQueryInformation()));
            params.putAll(DEFAULT_SEARCH_PARAMS);

            final GenericTrackList<YouTubeTrack> nextTracksForSearch = getTracksForSearch(trackList.getQueryValue(), params, QueryType.PAGING);
            return nextTracksForSearch.updatePagingValues(trackList, POSITION_KEY, OFFSET_KEY);
        }
        throw unsupportedQueryTypeException(YouTubeException::new, trackListQueryType);
    }

    @Override
    public void refreshTrackInfo(YouTubeTrack youTubeTrack) throws TrackSearchException {
        final String trackJSON = request(api.getForUrlWithParams(youTubeTrack.getUrl(), TRACK_PARAMS)).contentOrThrow();
        final YouTubeTrackInfo trackInfo = YouTubeUtility.extractTrackInfo(trackJSON, youTubeTrack.getUrl());
        youTubeTrack.setTrackInfo(trackInfo);
    }

    @Override
    public TrackStream getTrackStream(@NonNull final YouTubeTrack youtubeTrack) throws TrackSearchException {
        final YouTubeTrackFormat trackFormat = TrackFormatComparator.getBestYouTubeTrackFormat(youtubeTrack, false);

        if (trackFormat.isStreamReady()) {
            final String streamUrl = URLModifier.decode(trackFormat.getUrl());
            return new TrackStream(streamUrl, trackFormat);
        }

        final String scriptUrl = youtubeTrack.getTrackInfo().getScriptUrlOrThrow();

        final String scriptContent;
        if (scriptCache.containsKey(scriptUrl)) {
            log.trace("Use cached script for: {}", scriptUrl);
            scriptContent = scriptCache.get(scriptUrl);
        } else {
            scriptContent = SharedClient.request(URL + scriptUrl).contentOrThrow();
            scriptCache.put(scriptUrl, scriptContent);
        }

        final String signature = YouTubeUtility.getSignature(trackFormat, scriptUrl, scriptContent);
        final String trackFormatUrl = trackFormat.getUrl();

        final String streamUrl = URLModifier.addRequestParam(trackFormatUrl, trackFormat.getSigParam(), signature);
        return new TrackStream(streamUrl, trackFormat);
    }

    @Override
    public TrackStream getTrackStream(@NonNull final YouTubeTrack youtubeTrack, final int retries) throws TrackSearchException {
        return tryResolveTrackStream(youtubeTrack, retries)
                .orElseThrow(() -> noTrackStreamAfterRetriesException(YouTubeException::new, retries));
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
