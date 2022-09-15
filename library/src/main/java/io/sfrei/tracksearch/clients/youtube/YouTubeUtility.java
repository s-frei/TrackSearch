package io.sfrei.tracksearch.clients.youtube;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.sfrei.tracksearch.clients.setup.QueryType;
import io.sfrei.tracksearch.clients.setup.ResponseWrapper;
import io.sfrei.tracksearch.exceptions.YouTubeException;
import io.sfrei.tracksearch.tracks.BaseTrackList;
import io.sfrei.tracksearch.tracks.YouTubeTrack;
import io.sfrei.tracksearch.tracks.metadata.FormatType;
import io.sfrei.tracksearch.tracks.metadata.YouTubeTrackFormat;
import io.sfrei.tracksearch.tracks.metadata.YouTubeTrackInfo;
import io.sfrei.tracksearch.utils.CacheMap;
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

    private static final ObjectMapper MAPPER = new ObjectMapper();
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

    // JSON Routes
    private static final String[] defaultRoute = {"contents", "twoColumnSearchResultsRenderer", "primaryContents",
            "sectionListRenderer", "contents"};
    private static final String[] continuationItemRenderer = {"continuationItemRenderer", "continuationEndpoint", "continuationCommand"};

    private final CacheMap<String, SignatureResolver> sigResolverCache;

    public YouTubeUtility() {
        sigResolverCache = new CacheMap<>();
    }

    private static String wrap(String functionContent) {
        return "(" + VAR_NAME + ":function" + functionContent + FUNCTION_END + ")";
    }

    protected BaseTrackList<YouTubeTrack> getYouTubeTracks(final String json, final QueryType queryType, final String query,
                                                           final Function<YouTubeTrack, String> streamUrlProvider)
            throws YouTubeException {

        final JsonElement jsonElement;
        try {
            jsonElement = JsonElement.read(MAPPER, json);
        } catch (JsonProcessingException e) {
            throw new YouTubeException("Error parsing YouTubeTracks JSON", e);
        }

        final JsonElement responseElement = jsonElement.get("response")
                .orElseGet(() -> jsonElement.getIndex(1).get("response"));

        final JsonElement defaultElement = responseElement
                .get(defaultRoute);

        final JsonElement contentHolder = defaultElement
                .getFirstField()
                .get("itemSectionRenderer")
                .orElseGet(() -> responseElement
                        .get("onResponseReceivedCommands")
                        .getFirstField()
                        .get("appendContinuationItemsAction", "continuationItems")
                        .getFirstField()
                        .get("itemSectionRenderer"))
                .orElseGet(() -> responseElement
                        .get("continuationContents", "itemSectionContinuation", "itemSectionContinuation"))
                .orElseGet(() -> responseElement
                        .get("continuationContents", "sectionListContinuation", "contents")
                        .getFirstField()
                        .get("itemSectionRenderer"));

        final String cToken;
        if (contentHolder.get("continuations").present()) {
            cToken = contentHolder
                    .get("continuations")
                    .getFirstField()
                    .get("nextContinuationData")
                    .getAsString("continuation");
        } else {
            cToken = responseElement
                    .get("onResponseReceivedCommands")
                    .getFirstField()
                    .get("appendContinuationItemsAction", "continuationItems")
                    .getIndex(1)
                    .get("continuationItemRenderer", "continuationEndpoint", "continuationCommand")
                    .orElseGet(() -> defaultElement
                            .firstElementFor("continuationItemRenderer")
                            .get("continuationEndpoint", "continuationCommand"))
                    .getAsString("token");
        }

        final JsonElement contents = contentHolder.get("contents");
        final List<YouTubeTrack> ytTracks = contents.elements()
                .filter(content -> Objects.isNull(content.get("videoRenderer", "upcomingEventData").getNode())) // Avoid premieres
                .filter(content -> Objects.isNull(content.get("promotedSparklesWebRenderer").getNode())) // Avoid ads
                .map(content -> content.get("videoRenderer").orElseGet(() -> content
                        .get("searchPyvRenderer", "ads")
                        .getFirstField()
                        .get("promotedVideoRenderer")))
                .filter(renderer -> Objects.nonNull(renderer.get("lengthText").getNode())) // Avoid live streams
                .map(renderer -> {
                    try {
                        return renderer.mapToObject(MAPPER, YouTubeTrack.class);
                    } catch (Exception e) {
                        log.error("Error parsing Youtube track JSON: {}", renderer.getNode().toString(), e);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .peek(youTubeTrack -> youTubeTrack.setStreamUrlProvider(streamUrlProvider))
                .collect(Collectors.toList());

        int foundTracks = ytTracks.size();
        final Map<String, String> queryInformation = YouTubeClient.makeQueryInformation(query, cToken);
        final BaseTrackList<YouTubeTrack> trackList = new BaseTrackList<>(ytTracks, queryType, queryInformation);
        trackList.addQueryInformationValue(YouTubeClient.OFFSET_KEY, foundTracks);
        log.debug("Found {} YouTube Tracks for {}: {}", foundTracks, queryType, query);
        return trackList;
    }

    protected YouTubeTrackInfo getTrackInfo(final String json, final String trackUrl, Function<String, ResponseWrapper> requester) {
        try {
            final JsonElement jsonElement = JsonElement.read(MAPPER, json);

            final JsonElement playerElement;
            if (jsonElement.isArray()) {
                playerElement = jsonElement.getIndex(2).get("player");
            } else {
                playerElement = jsonElement.firstElementFor("player");
            }

            AtomicReference<String> scriptUrl = new AtomicReference<>(null);

            final JsonElement streamingData;

            if (playerElement != null) {

                final JsonElement args = playerElement.get("args");
                if (playerElement.present() && args.present()) {
                    scriptUrl.set(playerElement.get("assets").getAsString("js"));
                    final JsonElement playerResponseTextNode = args.get("player_response").reRead(MAPPER);
                    streamingData = playerResponseTextNode.get("streamingData");
                } else {
                    streamingData = jsonElement.getIndex(2).get("playerResponse", "streamingData");
                }

            } else {

                streamingData = jsonElement.get("playerResponse", "streamingData");

            }

            final JsonElement formatsElement = streamingData.get("formats");
            final Stream<YouTubeTrackFormat> formats;
            if (formatsElement.present()) {
                final Stream<JsonElement> formatsStream = formatsElement.arrayElements();
                formats = getFormatsFromStream(formatsStream);
            } else
                formats = Stream.empty();

            final Stream<JsonElement> adaptiveFormatsStream = streamingData.get("adaptiveFormats").arrayElements();
            final Stream<YouTubeTrackFormat> adaptiveFormats = getFormatsFromStream(adaptiveFormatsStream);

            final List<YouTubeTrackFormat> trackFormats = Stream.concat(formats, adaptiveFormats).collect(Collectors.toList());

            if (trackFormats.stream().anyMatch(YouTubeTrackFormat::streamNotReady) && scriptUrl.get() == null) {
                log.trace("Try to get player script trough embedded URL");
                final String embeddedUrl = trackUrl.replace("youtube.com/", "youtube.com/embed/");
                final String embeddedPageContent = requester.apply(embeddedUrl).getContent();
                if (embeddedPageContent != null) {
                    final Matcher matcher = EMBEDDED_PLAYER_SCRIPT_PATTERN.matcher(embeddedPageContent);
                    if (matcher.find()) {
                        log.trace("Found player script in embedded URL");
                        scriptUrl.set(matcher.group(1));
                    }
                }
            }

            return new YouTubeTrackInfo(trackFormats, scriptUrl.get());

        } catch (JsonProcessingException e) {
            log.error("Error parsing Youtube info JSON: {}", e.getMessage());
            return null;
        }
    }

    private Stream<YouTubeTrackFormat> getFormatsFromStream(final Stream<JsonElement> formats) {
        return formats.map(format -> {
            final String mimeType = format.getAsString("mimeType");
            final FormatType formatType = FormatType.getFormatType(mimeType);
            final String audioQuality = format.getAsString("audioQuality");
            final String audioSampleRate = format.getAsString("audioSampleRate");

            final String cipher = format.getAsString("cipher", "signatureCipher");

            if (cipher == null) {
                final String url = format.getAsString("url");
                return YouTubeTrackFormat.builder()
                        .mimeType(mimeType)
                        .formatType(formatType)
                        .audioQuality(audioQuality)
                        .audioSampleRate(audioSampleRate)
                        .streamReady(true)
                        .url(url)
                        .sigParam(null)
                        .sigValue(null)
                        .build();
            } else {
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
        final HashMap<String, SignaturePart.SignatureOccurrence> obfuscateFunctionsDefinitios = new HashMap<>();
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

            obfuscateFunctionsDefinitios.put(functionName, occurrence);
        }
        return obfuscateFunctionsDefinitios;
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
