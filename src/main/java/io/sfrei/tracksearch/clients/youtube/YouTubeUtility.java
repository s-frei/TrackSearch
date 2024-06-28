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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.sfrei.tracksearch.clients.common.QueryType;
import io.sfrei.tracksearch.exceptions.YouTubeException;
import io.sfrei.tracksearch.tracks.GenericTrackList;
import io.sfrei.tracksearch.tracks.TrackListProvider;
import io.sfrei.tracksearch.tracks.YouTubeTrack;
import io.sfrei.tracksearch.tracks.deserializer.youtube.YouTubeListTrackDeserializer;
import io.sfrei.tracksearch.tracks.deserializer.youtube.YouTubeURLTrackDeserializer;
import io.sfrei.tracksearch.utils.ObjectMapperBuilder;
import io.sfrei.tracksearch.utils.json.JsonElement;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
public final class YouTubeUtility {

    private static final ObjectMapper MAPPER = ObjectMapperBuilder.create()
            .addDeserializer(YouTubeTrack.ListYouTubeTrackBuilder.class, new YouTubeListTrackDeserializer())
            .addDeserializer(YouTubeTrack.URLYouTubeTrackBuilder.class, new YouTubeURLTrackDeserializer())
            .get();


    static YouTubeTrack extractYouTubeTrack(final String json)
            throws YouTubeException {

        final JsonElement trackJsonElement = JsonElement.readTreeCatching(MAPPER, json)
                .orElseThrow(() -> new YouTubeException("Cannot parse YouTubeTrack JSON"));

        return playerResponseFromTrackJSON(trackJsonElement)
                .mapCatching(MAPPER, YouTubeTrack.URLYouTubeTrackBuilder.class).getBuilder()
                .build();
    }

    static GenericTrackList<YouTubeTrack> extractYouTubeTracks(final String json, final QueryType queryType, final String query,
                                                               final TrackListProvider<YouTubeTrack> nextTrackListFunction)
            throws YouTubeException {

        final JsonElement rootElement = JsonElement.readTreeCatching(MAPPER, json)
                .orElseThrow(() -> new YouTubeException("Cannot parse YouTubeTracks JSON"));

        final JsonElement responseElement = rootElement.paths("response").orElse(rootElement).elementAtIndex(1).paths("response");

        final JsonElement defaultElement = responseElement.asUnresolved()
                .paths("contents", "twoColumnSearchResultsRenderer", "primaryContents", "sectionListRenderer", "contents");

        final JsonElement contentHolder = defaultElement
                .lastForPath("itemSectionRenderer") // Avoid sponsored
                .orElse(responseElement)
                .paths("onResponseReceivedCommands")
                .firstElement()
                .paths("appendContinuationItemsAction", "continuationItems")
                .firstElement()
                .paths("itemSectionRenderer")
                .orElse(responseElement)
                .paths("onResponseReceivedCommands")
                .firstElement()
                .paths("appendContinuationItemsAction", "continuationItems")
                .firstElement()
                .paths("itemSectionRenderer")
                .orElse(responseElement)
                .paths("continuationContents", "itemSectionContinuation", "itemSectionContinuation")
                .orElse(responseElement)
                .paths("continuationContents", "sectionListContinuation", "contents")
                .firstElement()
                .paths("itemSectionRenderer");

        final String cToken = extractCToken(responseElement, defaultElement, contentHolder);

        final JsonElement contents = contentHolder.asUnresolved().paths("contents");
        final List<YouTubeTrack> ytTracks = contents.elements()
                .filter(content -> content.paths("videoRenderer", "upcomingEventData").isNull()) // Avoid premieres
                .filter(content -> content.paths("promotedSparklesWebRenderer").isNull()) // Avoid ads
                .map(content -> content.paths("videoRenderer").orElse(content).paths("searchPyvRenderer", "ads").firstElement().paths("promotedVideoRenderer"))
                .filter(renderer -> renderer.asUnresolved().paths("lengthText").isPresent()) // Avoid live streams
                .map(renderer -> renderer.mapCatching(MAPPER, YouTubeTrack.ListYouTubeTrackBuilder.class))
                .filter(Objects::nonNull)
                .map(YouTubeTrack.ListYouTubeTrackBuilder::getBuilder)
                .map(YouTubeTrack.YouTubeTrackBuilder::build)
                .collect(Collectors.toList());

        final Map<String, String> queryInformation = YouTubeClient.makeQueryInformation(query, cToken);
        final GenericTrackList<YouTubeTrack> trackList = GenericTrackList.using(queryType, queryInformation, nextTrackListFunction)
                .withTracks(ytTracks);

        int tracksSize = ytTracks.size();
        trackList.addQueryInformationValue(YouTubeClient.OFFSET_KEY, tracksSize);
        log.debug("Found {} YouTube Tracks for {}: {}", tracksSize, queryType, query);
        return trackList;
    }

    private static String extractCToken(JsonElement responseElement, JsonElement defaultElement, JsonElement contentHolder) {
        if (contentHolder.nodePresent("continuations")) {
            return contentHolder.asUnresolved()
                    .paths("continuations")
                    .firstElement()
                    .paths("nextContinuationData")
                    .asString("continuation");
        }
        return responseElement.asUnresolved()
                .paths("onResponseReceivedCommands")
                .firstElement()
                .paths("appendContinuationItemsAction", "continuationItems")
                .elementAtIndex(1)
                .paths("continuationItemRenderer", "continuationEndpoint", "continuationCommand")
                .orElse(defaultElement)
                .findElement("continuationItemRenderer")
                .paths("continuationEndpoint", "continuationCommand")
                .asString("token");
    }

    private static JsonElement playerResponseFromTrackJSON(JsonElement jsonElement) {
        return jsonElement.elementAtIndex(2)
                .paths("playerResponse")
                .orElse(jsonElement)
                .paths("playerResponse")
                .orElse(jsonElement);
    }

}
