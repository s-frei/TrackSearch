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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.sfrei.tracksearch.clients.interfaces.functional.NextTrackListFunction;
import io.sfrei.tracksearch.clients.interfaces.functional.StreamURLFunction;
import io.sfrei.tracksearch.clients.setup.QueryType;
import io.sfrei.tracksearch.clients.setup.ResponseWrapper;
import io.sfrei.tracksearch.exceptions.YouTubeException;
import io.sfrei.tracksearch.tracks.GenericTrackList;
import io.sfrei.tracksearch.tracks.YouTubeTrack;
import io.sfrei.tracksearch.tracks.deserializer.youtube.YouTubeListTrackDeserializer;
import io.sfrei.tracksearch.tracks.deserializer.youtube.YouTubeURLTrackDeserializer;
import io.sfrei.tracksearch.tracks.metadata.FormatType;
import io.sfrei.tracksearch.tracks.metadata.YouTubeTrackFormat;
import io.sfrei.tracksearch.tracks.metadata.YouTubeTrackInfo;
import io.sfrei.tracksearch.utils.CacheMap;
import io.sfrei.tracksearch.utils.ObjectMapperBuilder;
import io.sfrei.tracksearch.utils.URLUtility;
import io.sfrei.tracksearch.utils.json.JsonElement;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
class YouTubeUtility {

    private static final String JS_SPLICE = ".splice";
    private static final String JS_SLICE = ".slice";
    private static final String JS_REVERSE = ".reverse";
    private static final String FUNCTION_CALL = "([a-zA-Z]+.([a-zA-Z0-9]+)\\(a,([0-9]+)\\))";
    private static final Pattern FUNCTION_CALL_PATTERN = Pattern.compile(FUNCTION_CALL);
    private static final Pattern OBFUSCATE_FUNCTIONS_CALLS_PATTERN = Pattern.compile(
            "function\\(a\\)\\{a=a.split\\(\"\"\\);([a-zA-Z1-9 =,-\\[\\]\"()]+);"
    );
    private static final String VAR_NAME = "[a-zA-Z0-9]+";
    private static final String FUNCTION_END = "}*,?\\n?";
    private static final String SLICE = wrap("\\(a,b\\)\\{return a\\.slice\\(b\\)");
    private static final String SPLICE = wrap("\\(a,b\\)\\{a\\.splice\\(0,b\\)");
    private static final String REVERSE = wrap("\\(a\\)\\{a\\.reverse\\(\\)");
    private static final String SWAP = wrap("\\(a,b\\)\\{var c=a\\[0\\];a\\[0\\]=a\\[b%a\\.length\\];a\\[b%a.length\\]=c");
    private static final Pattern OBFUSCATE_FUNCTIONS_PATTERN = Pattern.compile(
            "var " + VAR_NAME + "=\\{" +
                    "(("
                    + SLICE + "|" + SPLICE + "|" + REVERSE + "|" + SWAP +
                    ")+)"
    );
    private static final Pattern EMBEDDED_PLAYER_SCRIPT_PATTERN = Pattern.compile("src=\"(/[a-zA-Z0-9/-_.]+base.js)\"");

    private final CacheMap<String, SignatureResolver> sigResolverCache = new CacheMap<>();

    private static final ObjectMapper MAPPER = ObjectMapperBuilder.create()
            .addDeserializer(YouTubeTrack.ListYouTubeTrackBuilder.class, new YouTubeListTrackDeserializer())
            .addDeserializer(YouTubeTrack.URLYouTubeTrackBuilder.class, new YouTubeURLTrackDeserializer())
            .get();

    private static String wrap(String functionContent) {
        return "(" + VAR_NAME + ":function" + functionContent + FUNCTION_END + ")";
    }

    protected YouTubeTrack extractYouTubeTrack(final String json, final StreamURLFunction<YouTubeTrack> streamUrlFunction)
            throws YouTubeException {

        final JsonElement trackJsonElement = JsonElement.readTreeCatching(MAPPER, json)
                .orElseThrow(() -> new YouTubeException("Cannot parse YouTubeTrack JSON"));

        return playerResponseFromTrackJSON(trackJsonElement)
                .mapCatching(MAPPER, YouTubeTrack.URLYouTubeTrackBuilder.class).getBuilder()
                .streamUrlFunction(streamUrlFunction)
                .build();
    }

    protected GenericTrackList<YouTubeTrack> extractYouTubeTracks(final String json, final QueryType queryType, final String query,
                                                                  final NextTrackListFunction<YouTubeTrack> nextTrackListFunction,
                                                                  final StreamURLFunction<YouTubeTrack> streamUrlFunction)
            throws YouTubeException {

        final JsonElement rootElement = JsonElement.readTreeCatching(MAPPER, json)
                .orElseThrow(() -> new YouTubeException("Cannot parse YouTubeTracks JSON"));

        final JsonElement responseElement = rootElement.path("response").orElse(rootElement).elementAtIndex(1).path("response");

        final JsonElement defaultElement = responseElement.asUnresolved()
                .path("contents", "twoColumnSearchResultsRenderer", "primaryContents", "sectionListRenderer", "contents");

        final JsonElement contentHolder = defaultElement
                .firstElement()
                .path("itemSectionRenderer")
                .orElse(responseElement)
                .path("onResponseReceivedCommands")
                .firstElement()
                .path("appendContinuationItemsAction", "continuationItems")
                .firstElement()
                .path("itemSectionRenderer")
                .orElse(responseElement)
                .path("onResponseReceivedCommands")
                .firstElement()
                .path("appendContinuationItemsAction", "continuationItems")
                .firstElement()
                .path("itemSectionRenderer")
                .orElse(responseElement)
                .path("continuationContents", "itemSectionContinuation", "itemSectionContinuation")
                .orElse(responseElement)
                .path("continuationContents", "sectionListContinuation", "contents")
                .firstElement()
                .path("itemSectionRenderer");

        final String cToken = extractCToken(responseElement, defaultElement, contentHolder);

        final JsonElement contents = contentHolder.asUnresolved().path("contents");
        final List<YouTubeTrack> ytTracks = contents.elements()
                .filter(content -> content.path("videoRenderer", "upcomingEventData").isNull()) // Avoid premieres
                .filter(content -> content.path("promotedSparklesWebRenderer").isNull()) // Avoid ads
                .map(content -> content.path("videoRenderer").orElse(content).path("searchPyvRenderer", "ads").firstElement().path("promotedVideoRenderer"))
                .filter(renderer -> renderer.asUnresolved().path("lengthText").isPresent()) // Avoid live streams
                .map(renderer -> renderer.mapCatching(MAPPER, YouTubeTrack.ListYouTubeTrackBuilder.class))
                .filter(Objects::nonNull)
                .map(YouTubeTrack.ListYouTubeTrackBuilder::getBuilder)
                .peek(youTubeTrackBuilder -> youTubeTrackBuilder.streamUrlFunction(streamUrlFunction))
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
                    .path("continuations")
                    .firstElement()
                    .path("nextContinuationData")
                    .asString("continuation");
        }
        return responseElement.asUnresolved()
                .path("onResponseReceivedCommands")
                .firstElement()
                .path("appendContinuationItemsAction", "continuationItems")
                .elementAtIndex(1)
                .path("continuationItemRenderer", "continuationEndpoint", "continuationCommand")
                .orElse(defaultElement)
                .findElement("continuationItemRenderer")
                .path("continuationEndpoint", "continuationCommand")
                .asString("token");
    }

    protected YouTubeTrackInfo extractTrackInfo(final String json, final String trackUrl, Function<String, ResponseWrapper> requester)
            throws YouTubeException {

        try {
            final JsonElement jsonElement = JsonElement.readTree(MAPPER, json);

            final JsonElement playerElement;
            if (jsonElement.isArray()) {
                playerElement = jsonElement.elementAtIndex(2).path("player");
            } else {
                playerElement = jsonElement.findElement("player");
            }

            AtomicReference<String> scriptUrl = new AtomicReference<>(null);

            final JsonElement streamingData;

            final JsonElement playerArgs = playerElement.path("args");
            if (playerElement.isPresent() && playerArgs.isPresent()) {

                scriptUrl.set(playerElement.path("assets").asString("js"));

                streamingData = playerArgs.path("player_response")
                        .reReadTree(MAPPER)
                        .path("streamingData");

            } else {
                final JsonElement playerResponse = playerResponseFromTrackJSON(jsonElement);

                streamingData = playerResponse.asUnresolved().path("streamingData");
            }

            final JsonElement formatsElement = streamingData.path("formats");
            final Stream<YouTubeTrackFormat> formats = formatsElement.isPresent() ?
                    getFormatsFromStream(formatsElement.arrayElements()) : Stream.empty();

            final Stream<JsonElement> adaptiveFormatsStream = streamingData.path("adaptiveFormats").arrayElements();
            final Stream<YouTubeTrackFormat> adaptiveFormats = getFormatsFromStream(adaptiveFormatsStream);

            final List<YouTubeTrackFormat> trackFormats = Stream.concat(formats, adaptiveFormats).collect(Collectors.toList());

            if (trackFormats.stream().anyMatch(YouTubeTrackFormat::isStreamNotReady) && scriptUrl.get() == null) {
                log.trace("Try to get player script trough embedded URL");
                final String embeddedUrl = trackUrl.replace("youtube.com/", "youtube.com/embed/");
                final String embeddedPageContent = requester.apply(embeddedUrl).getContent();
                if (embeddedPageContent != null) {
                    final Matcher matcher = EMBEDDED_PLAYER_SCRIPT_PATTERN.matcher(embeddedPageContent);
                    if (matcher.find()) {
                        log.trace("Found player script in embedded URL: '{}'", embeddedUrl);
                        scriptUrl.set(matcher.group(1));
                    }
                }
            }

            return new YouTubeTrackInfo(trackFormats, scriptUrl.get());

        } catch (JsonProcessingException e) {
            throw new YouTubeException("Failed parsing Youtube track JSON", e);
        }
    }

    private static JsonElement playerResponseFromTrackJSON(JsonElement jsonElement) {
        return jsonElement.elementAtIndex(2)
                .path("playerResponse")
                .orElse(jsonElement)
                .path("playerResponse");
    }

    private Stream<YouTubeTrackFormat> getFormatsFromStream(final Stream<JsonElement> formats) {
        return formats.map(format -> {
            final String mimeType = format.asString("mimeType");
            final FormatType formatType = FormatType.getFormatType(mimeType);
            final String audioQuality = format.asString("audioQuality");
            final String audioSampleRate = format.asString("audioSampleRate");

            final JsonElement cipherElement = format.path("cipher")
                    .orElse(format)
                    .path("signatureCipher");

            if (cipherElement.isNull()) {
                final String url = format.asString("url");
                return YouTubeTrackFormat.builder()
                        .mimeType(mimeType)
                        .formatType(formatType)
                        .audioQuality(audioQuality)
                        .audioSampleRate(audioSampleRate)
                        .streamReady(true)
                        .url(url)
                        .build();
            } else {
                final String cipher = cipherElement.asString();
                final Map<String, String> params = URLUtility.splitParamsAndDecode(cipher);
                return YouTubeTrackFormat.builder()
                        .mimeType(mimeType)
                        .formatType(formatType)
                        .audioQuality(audioQuality)
                        .audioSampleRate(audioSampleRate)
                        .streamReady(false)
                        .url(params.get("url"))
                        .sigParam(params.getOrDefault("sp", "sig"))
                        .sigValue(params.get("s"))
                        .build();
            }
        });
    }

    private Map<String, SignaturePart.SignatureOccurrence> getObfuscateFunctionDefinitions(final String scriptPart) {
        final HashMap<String, SignaturePart.SignatureOccurrence> obfuscateFunctionsDefinitions = new HashMap<>();
        final String[] functions = scriptPart.split("\n");
        for (final String function : functions) {
            final String functionName = getFunctionName(function);
            if (functionName == null)
                continue;

            final SignaturePart.SignatureOccurrence occurrence;
            if (function.contains(JS_SLICE))
                occurrence = SignaturePart.SignatureOccurrence.SLICE;
            else if (function.contains(JS_SPLICE))
                occurrence = SignaturePart.SignatureOccurrence.SPLICE;
            else if (function.contains(JS_REVERSE))
                occurrence = SignaturePart.SignatureOccurrence.REVERSE;
            else
                occurrence = SignaturePart.SignatureOccurrence.SWAP;

            obfuscateFunctionsDefinitions.put(functionName, occurrence);
        }
        return obfuscateFunctionsDefinitions;
    }

    private String getFunctionName(final String wholeFunction) {
        final String[] split = wholeFunction.split(":function");
        return split.length > 0 ? split[0] : null;
    }

    protected String getSignature(final YouTubeTrackFormat youtubeTrackFormat, String scriptUrl, final String scriptBody)
            throws YouTubeException {

        final String sigValue = youtubeTrackFormat.getSigValue();

        if (sigResolverCache.containsKey(scriptUrl)) {
            log.trace("Use cached signature resolver for: {}", scriptUrl);
            return sigResolverCache.get(scriptUrl).resolveSignature(sigValue);
        }

        final Matcher obfuscateFunctionsMatcher = OBFUSCATE_FUNCTIONS_PATTERN.matcher(scriptBody);
        if (!obfuscateFunctionsMatcher.find())
            throw new YouTubeException("Was not able to find obfuscate functions");

        final String obfuscateFunctions = obfuscateFunctionsMatcher.group(1);
        final Map<String, SignaturePart.SignatureOccurrence> obfuscateFunctionDefinitions =
                getObfuscateFunctionDefinitions(obfuscateFunctions);

        final Matcher obfuscateFunctionsCallsMatcher = OBFUSCATE_FUNCTIONS_CALLS_PATTERN.matcher(scriptBody);
        if (!obfuscateFunctionsCallsMatcher.find())
            throw new YouTubeException("Was not able to find obfuscate functions calls");

        final Matcher obfuscateFunctionCallMatcher = FUNCTION_CALL_PATTERN.matcher(obfuscateFunctionsCallsMatcher.group(1));

        final SignatureResolver signatureResolver = new SignatureResolver();

        while (obfuscateFunctionCallMatcher.find()) {
            final String obfuscateFunctionName = obfuscateFunctionCallMatcher.group(2).trim();
            final SignaturePart.SignatureOccurrence signatureOccurrence = obfuscateFunctionDefinitions.get(obfuscateFunctionName);

            if (signatureOccurrence == null)
                continue;

            final Integer obfuscateFunctionParameter = Integer.parseInt(obfuscateFunctionCallMatcher.group(3));
            signatureResolver.addSignaturePart(obfuscateFunctionName, signatureOccurrence, obfuscateFunctionParameter);
        }

        if (!sigResolverCache.containsKey(scriptUrl))
            sigResolverCache.put(scriptUrl, signatureResolver);

        return signatureResolver.resolveSignature(sigValue);
    }

    @Value
    private static class SignaturePart {

        String functionName;
        SignatureOccurrence occurrence;
        Integer parameter;

        public enum SignatureOccurrence {
            SLICE, SPLICE, REVERSE, SWAP
        }
    }

    private static class SignatureResolver {

        private final List<SignaturePart> signatureParts;

        public SignatureResolver() {
            this.signatureParts = new ArrayList<>();
        }

        private void addSignaturePart(String functionName, SignaturePart.SignatureOccurrence occurrence, Integer parameter) {
            this.signatureParts.add(new SignaturePart(functionName, occurrence, parameter));
        }

        private String resolveSignature(final String signatureValue) {
            final StringBuilder signature = new StringBuilder(signatureValue);
            for (final SignaturePart signaturePart : signatureParts) {
                final Integer parameter = signaturePart.getParameter();
                switch (signaturePart.getOccurrence()) {
                    case SLICE:
                    case SPLICE:
                        signature.delete(0, parameter);
                        break;
                    case REVERSE:
                        signature.reverse();
                        break;
                    case SWAP:
                        int charPos = parameter % signatureValue.length();
                        char swapChar = signature.charAt(0);
                        signature.setCharAt(0, signature.charAt(charPos));
                        signature.setCharAt(charPos, swapChar);
                        break;
                    default:
                        break;
                }
            }
            return signature.toString();
        }
    }

}
