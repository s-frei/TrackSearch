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

import io.sfrei.tracksearch.clients.interfaces.ClientHelper;
import io.sfrei.tracksearch.clients.interfaces.Provider;
import io.sfrei.tracksearch.clients.setup.Client;
import io.sfrei.tracksearch.clients.setup.QueryType;
import io.sfrei.tracksearch.clients.setup.ResponseProviderFactory;
import io.sfrei.tracksearch.clients.setup.SingleSearchClient;
import io.sfrei.tracksearch.config.TrackSearchConfig;
import io.sfrei.tracksearch.exceptions.TrackSearchException;
import io.sfrei.tracksearch.exceptions.UniformClientException;
import io.sfrei.tracksearch.exceptions.YouTubeException;
import io.sfrei.tracksearch.tracks.GenericTrackList;
import io.sfrei.tracksearch.tracks.Track;
import io.sfrei.tracksearch.tracks.TrackList;
import io.sfrei.tracksearch.tracks.YouTubeTrack;
import io.sfrei.tracksearch.tracks.metadata.TrackStream;
import io.sfrei.tracksearch.tracks.metadata.YouTubeTrackFormat;
import io.sfrei.tracksearch.tracks.metadata.YouTubeTrackInfo;
import io.sfrei.tracksearch.utils.CacheMap;
import io.sfrei.tracksearch.utils.TrackFormatUtility;
import io.sfrei.tracksearch.utils.TrackListUtility;
import io.sfrei.tracksearch.utils.URLUtility;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import retrofit2.Retrofit;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class YouTubeClient extends SingleSearchClient<YouTubeTrack>
        implements ClientHelper, Provider<YouTubeTrack>, UniformClientException {

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
    private final YouTubeUtility youTubeUtility;

    private final CacheMap<String, String> scriptCache;

    public YouTubeClient() {

        super(
                (uri, cookie) -> cookie.getName().equals("YSC") || cookie.getName().equals("VISITOR_INFO1_LIVE") || cookie.getName().equals("GPS"),
                Map.of("Cookie", "CONSENT=YES+cb.20210328-17-p0.en+FX+478")
        );

        final Retrofit base = new Retrofit.Builder()
                .baseUrl(URL)
                .client(okHttpClient)
                .addConverterFactory(ResponseProviderFactory.create())
                .build();

        api = base.create(YouTubeAPI.class);
        youTubeUtility = new YouTubeUtility();
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

        final String trackJSON = Client.request(api.getForUrlWithParams(url, TRACK_PARAMS)).contentOrThrow();
        final YouTubeTrack youTubeTrack = youTubeUtility.extractYouTubeTrack(trackJSON, this::trackStreamProvider);
        final YouTubeTrackInfo trackInfo = youTubeUtility.extractTrackInfo(trackJSON, url, this::requestURL);
        youTubeTrack.setTrackInfo(trackInfo);
        return youTubeTrack;
    }

    private GenericTrackList<YouTubeTrack> getTracksForSearch(@NonNull final String search, @NonNull final Map<String, String> params, QueryType queryType)
            throws TrackSearchException {

        final String tracksJSON = Client.request(api.getSearchForKeywords(search, params)).contentOrThrow();
        return youTubeUtility.extractYouTubeTracks(tracksJSON, queryType, search, this::provideNext, this::trackStreamProvider);
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
            return TrackListUtility.updatePagingValues(nextTracksForSearch, trackList, POSITION_KEY, OFFSET_KEY);
        }
        throw unsupportedQueryTypeException(YouTubeException::new, trackListQueryType);
    }

    @Override
    public TrackStream getTrackStream(@NonNull final YouTubeTrack youtubeTrack) throws TrackSearchException {

        // Always load new track info to make retries possible - TODO: Not required on first try
        final String trackJSON = Client.request(api.getForUrlWithParams(youtubeTrack.getUrl(), TRACK_PARAMS)).contentOrThrow();
        final YouTubeTrackInfo trackInfo = youTubeUtility.extractTrackInfo(trackJSON, youtubeTrack.getUrl(), this::requestURL);
        youtubeTrack.setTrackInfo(trackInfo);

        final YouTubeTrackFormat trackFormat = TrackFormatUtility.getBestYouTubeTrackFormat(youtubeTrack, false);

        if (trackFormat.isStreamReady()) {
            final String streamUrl = URLUtility.decode(trackFormat.getUrl());
            return new TrackStream(streamUrl, trackFormat);
        }

        final String scriptUrl = youtubeTrack.getTrackInfo().getScriptUrlOrThrow();

        final String scriptContent;
        if (scriptCache.containsKey(scriptUrl)) {
            log.trace("Use cached script for: {}", scriptUrl);
            scriptContent = scriptCache.get(scriptUrl);
        } else {
            scriptContent = requestURL(URL + scriptUrl).contentOrThrow();
            scriptCache.put(scriptUrl, scriptContent);
        }

        final String signature = youTubeUtility.getSignature(trackFormat, scriptUrl, scriptContent);
        final String trackFormatUrl = trackFormat.getUrl();

        final String streamUrl = URLUtility.addRequestParam(trackFormatUrl, trackFormat.getSigParam(), signature);
        return new TrackStream(streamUrl, trackFormat);
    }

    @Override
    public TrackStream getTrackStream(@NonNull final YouTubeTrack youtubeTrack, final int retries) throws TrackSearchException {
        return getTrackStream(this, youtubeTrack, this::requestAndGetCode, retries)
                .orElseThrow(() -> noTrackStreamAfterRetriesException(YouTubeException::new, retries));
    }

    private Map<String, String> getPagingParams(final Map<String, String> queryInformation) {
        final String pagingToken = queryInformation.get(PAGING_INFORMATION);
        return Map.of(PAGING_KEY, pagingToken, ADDITIONAL_PAGING_KEY, pagingToken);
    }

    @Override
    public boolean hasPagingValues(@NonNull final TrackList<? extends Track> trackList) {
        return TrackListUtility.hasQueryInformation(trackList, POSITION_KEY, OFFSET_KEY, PAGING_INFORMATION);
    }

    @Override
    public Logger log() {
        return log;
    }

}
