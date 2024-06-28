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
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
public final class YouTubeUtility {

    public static final String VAR_YT_INITIAL_DATA = "var ytInitialData =";
    public static final String VAR_YT_INITIAL_PLAYER_RESPONSE = "var ytInitialPlayerResponse =";

    private static final ObjectMapper MAPPER = ObjectMapperBuilder.create()
            .addDeserializer(YouTubeTrack.ListYouTubeTrackBuilder.class, new YouTubeListTrackDeserializer())
            .addDeserializer(YouTubeTrack.URLYouTubeTrackBuilder.class, new YouTubeURLTrackDeserializer())
            .get();

    private static String extractJsonFromHtml(String html, String varType) throws YouTubeException {
        final Document document = Jsoup.parse(html);
        return document.select("script[nonce]").stream()
                .map(Element::data)
                .filter(data -> data.startsWith(varType))
                .findFirst()
                .map(scriptContent -> scriptContent.replaceFirst(varType, ""))
                .map(scriptContent -> scriptContent.substring(0, scriptContent.lastIndexOf("}") + 1))
                .orElseThrow(() -> new YouTubeException("Could not extract JSON data from HTML"));
    }

    static YouTubeTrack extractYouTubeTrack(final String html)
            throws YouTubeException {

        final String json = extractJsonFromHtml(html, VAR_YT_INITIAL_PLAYER_RESPONSE);
        final JsonElement trackJsonElement = JsonElement.readTreeCatching(MAPPER, json)
                .orElseThrow(() -> new YouTubeException("Cannot parse YouTubeTrack JSON"));

        return trackJsonElement.mapCatching(MAPPER, YouTubeTrack.URLYouTubeTrackBuilder.class)
                .getBuilder()
                .build();
    }

    static GenericTrackList<YouTubeTrack> extractYouTubeTracks(final String html, final QueryType queryType, final String query,
                                                               final TrackListProvider<YouTubeTrack> nextTrackListFunction)
            throws YouTubeException {

        final String json = extractJsonFromHtml(html, VAR_YT_INITIAL_DATA);

        final JsonElement rootElement = JsonElement.readTreeCatching(MAPPER, json)
                .orElseThrow(() -> new YouTubeException("Cannot parse YouTubeTracks JSON"));

        final JsonElement defaultElement = rootElement.asUnresolved()
                .paths("contents", "twoColumnSearchResultsRenderer", "primaryContents", "sectionListRenderer", "contents");

        final JsonElement contentHolder = defaultElement
                .lastForPath("itemSectionRenderer") // Avoid sponsored
                .orElse(rootElement)
                .paths("onResponseReceivedCommands")
                .firstElement()
                .paths("appendContinuationItemsAction", "continuationItems")
                .firstElement()
                .paths("itemSectionRenderer")
                .orElse(rootElement)
                .paths("onResponseReceivedCommands")
                .firstElement()
                .paths("appendContinuationItemsAction", "continuationItems")
                .firstElement()
                .paths("itemSectionRenderer")
                .orElse(rootElement)
                .paths("continuationContents", "itemSectionContinuation", "itemSectionContinuation")
                .orElse(rootElement)
                .paths("continuationContents", "sectionListContinuation", "contents")
                .firstElement()
                .paths("itemSectionRenderer");

        final String cToken = extractCToken(rootElement, defaultElement);

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

    // onResponseReceivedCommands[0].appendContinuationItemsAction.continuationItems[1].continuationItemRenderer.continuationEndpoint.continuationCommand.token
    // contents.twoColumnSearchResultsRenderer.primaryContents.sectionListRenderer.contents[1].continuationItemRenderer.continuationEndpoint.continuationCommand.token
    private static String extractCToken(JsonElement rootElement, JsonElement defaultElement) {
        final JsonElement continuationCommand = rootElement.asUnresolved()
                .paths("onResponseReceivedCommands")
                .firstElement()
                .paths("appendContinuationItemsAction", "continuationItems")
                .elementAtIndex(1)
                .paths("continuationItemRenderer", "continuationEndpoint", "continuationCommand")
                .orElse(defaultElement)
                .findElement("continuationItemRenderer")
                .paths("continuationEndpoint", "continuationCommand");
        return continuationCommand.asUnresolved().asString("token");
    }

}
