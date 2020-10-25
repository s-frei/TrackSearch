package io.sfrei.tracksearch.clients.youtube;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.sfrei.tracksearch.clients.setup.QueryType;
import io.sfrei.tracksearch.exceptions.YouTubeException;
import io.sfrei.tracksearch.tracks.BaseTrackList;
import io.sfrei.tracksearch.tracks.YouTubeTrack;
import io.sfrei.tracksearch.tracks.metadata.FormatType;
import io.sfrei.tracksearch.tracks.metadata.YouTubeTrackFormat;
import io.sfrei.tracksearch.tracks.metadata.YouTubeTrackInfo;
import io.sfrei.tracksearch.utils.URLUtility;
import io.sfrei.tracksearch.utils.json.JsonElement;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
    private static final String FUNCTION_CALL = "([a-zA-Z]+.([a-zA-Z0-9]+)\\(a,([0-9]+)\\);)";
    private static final Pattern FUNCTION_CALL_PATTERN = Pattern.compile(FUNCTION_CALL);
    private static final Pattern OBFUSCATE_FUNCTIONS_CALLS_PATTERN = Pattern.compile(
            "function\\(a\\)\\{a=a\\.split\\(\"\"\\);" + FUNCTION_CALL + "+"
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
    private static final String[] defaultRoute = {"contents", "twoColumnSearchResultsRenderer", "primaryContents",
            "sectionListRenderer", "contents"};
    private static final String[] continuationItemRenderer = {"continuationItemRenderer", "continuationEndpoint", "continuationCommand"};

    private static String wrap(String functionContent) {
        return "(" + VAR_NAME + ":function" + functionContent + FUNCTION_END + ")";
    }

    protected BaseTrackList<YouTubeTrack> getYouTubeTracks(String json, QueryType queryType, String query,
                                                           Function<YouTubeTrack, String> streamUrlProvider)
            throws YouTubeException {

        final JsonElement responseElement;
        try {
            responseElement = JsonElement.read(MAPPER, json).getIndex(1).get("response");
        } catch (JsonProcessingException e) {
            throw new YouTubeException("GetYouTubeTubeTracks - " + e.getMessage());
        }

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
                    .orElseGet(() -> defaultElement
                            .getIndex(1))
                    .get(continuationItemRenderer)
                    .getAsString("token");
        }

        final JsonElement contents = contentHolder.get("contents");
        final List<YouTubeTrack> ytTracks = contents.elements().map(content -> {
            try {
                return content.mapToObject(MAPPER, YouTubeTrack.class);
            } catch (JsonProcessingException e) {
                return null;
            }
        }).filter(Objects::nonNull)
                .peek(youTubeTrack -> youTubeTrack.setStreamUrlProvider(streamUrlProvider))
                .collect(Collectors.toList());

        int foundTracks = ytTracks.size();
        final Map<String, String> queryInformation = YouTubeClient.makeQueryInformation(query, cToken);
        final BaseTrackList<YouTubeTrack> trackList = new BaseTrackList<>(ytTracks, queryType, queryInformation);
        trackList.addQueryInformationValue(YouTubeClient.OFFSET_KEY, foundTracks);
        log.debug("Found {} YouTube Tracks", foundTracks);
        return trackList;
    }

    public YouTubeTrackInfo getTrackInfo(String json) {
        try {
            JsonElement jsonElement = JsonElement.read(MAPPER, json);

            JsonElement playerElement;
            if (!jsonElement.isArray()) {
                playerElement = jsonElement.firstElementFor("player");
            } else {
                playerElement = jsonElement.getIndex(2).get("player");
            }

            JsonElement args = playerElement.get("args");

            String scriptUrl;
            final JsonElement streamingData;
            if (playerElement.present() && args.present()) {
                scriptUrl = playerElement.get("assets").getAsString("js");
                JsonElement playerResponseTextNode = args.get("player_response").reRead(MAPPER);
                streamingData = playerResponseTextNode.get("streamingData");
            } else {
                scriptUrl = null;
                streamingData = jsonElement.getIndex(2).get("playerResponse", "streamingData");
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

            final List<YouTubeTrackFormat> trackFormats = Stream.concat(formats, adaptiveFormats)
                    .collect(Collectors.toList());

            if (scriptUrl == null)
                log.debug("");

            return new YouTubeTrackInfo(trackFormats, scriptUrl);

        } catch (JsonProcessingException e) {
            log.error("Error parsing Youtube track JSON: " + e.getMessage());
            return null;
        }
    }

    private Stream<YouTubeTrackFormat> getFormatsFromStream(Stream<JsonElement> formats) {
        return formats.map(format -> {
            String mimeType = format.getAsString("mimeType");
            FormatType formatType = FormatType.getFormatType(mimeType);
            String audioQuality = format.getAsString("audioQuality");
            String audioSampleRate = format.getAsString("audioSampleRate");

            String cipher = format.getAsString("cipher", "signatureCipher");

            if (cipher == null) {
                String url = format.getAsString("url");
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
                Map<String, String> params = URLUtility.splitAndDecodeUrl(cipher);
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

    private List<SignaturePart> getObfuscateFunctionDefinitions(String scriptPart) {
        List<SignaturePart> obfuscateFunctionDefinitions = new ArrayList<>();
        String[] functions = scriptPart.split("\n");
        for (String function : functions) {
            String functionName = getFunctionName(function);
            if (functionName == null)
                continue;

            SignaturePart obfuscateFunctionDefinition = SignaturePart.builder().functionName(functionName).build();
            if (function.contains(JS_SLICE))
                obfuscateFunctionDefinition.setOccurrence(SignaturePart.SignatureOccurrence.SLICE);
            else if (function.contains(JS_SPLICE))
                obfuscateFunctionDefinition.setOccurrence(SignaturePart.SignatureOccurrence.SPLICE);
            else if (function.contains(JS_REVERSE))
                obfuscateFunctionDefinition.setOccurrence(SignaturePart.SignatureOccurrence.REVERSE);
            else
                obfuscateFunctionDefinition.setOccurrence(SignaturePart.SignatureOccurrence.SWAP);

            obfuscateFunctionDefinitions.add(obfuscateFunctionDefinition);
        }
        return obfuscateFunctionDefinitions;
    }

    private String getFunctionName(String wholeFunction) {
        String[] split = wholeFunction.split(":function");
        return split.length > 0 ? split[0] : null;
    }

    private SignaturePart getSignaturePart(List<SignaturePart> parts, String functionName, Integer parameter) {
        for (SignaturePart part : parts) {
            if (part.getFunctionName().equals(functionName))
                return new SignaturePart(part.getFunctionName(), part.getOccurrence(), parameter);
        }
        return null;
    }

    public String getSignature(YouTubeTrackFormat youtubeTrackFormat, String scriptBody) throws YouTubeException {

        Matcher obfuscateFunctionsMatcher = OBFUSCATE_FUNCTIONS_PATTERN.matcher(scriptBody);
        if (!obfuscateFunctionsMatcher.find())
            throw new YouTubeException("Was not able to find obfuscate functions");

        String obfuscateFunctions = obfuscateFunctionsMatcher.group(1);
        List<SignaturePart> obfuscateFunctionDefinitions = getObfuscateFunctionDefinitions(obfuscateFunctions);

        Matcher obfuscateFunctionsCallsMatcher = OBFUSCATE_FUNCTIONS_CALLS_PATTERN.matcher(scriptBody);
        if (!obfuscateFunctionsCallsMatcher.find())
            throw new YouTubeException("Was not able to find obfuscate functions calls");

        Matcher obfuscateFunctionCallsMatcher = FUNCTION_CALL_PATTERN.matcher(obfuscateFunctionsCallsMatcher.group());

        SignatureResolver signatureResolver = new SignatureResolver();
        while (obfuscateFunctionCallsMatcher.find()) {
            String obfuscateFunctionName = obfuscateFunctionCallsMatcher.group(2);
            Integer obfuscateFunctionParameter = Integer.parseInt(obfuscateFunctionCallsMatcher.group(3));

            SignaturePart signaturePart = getSignaturePart(obfuscateFunctionDefinitions, obfuscateFunctionName, obfuscateFunctionParameter);
            if (signaturePart != null)
                signatureResolver.addSignaturePart(signaturePart);
        }

        return signatureResolver.resolveSignature(youtubeTrackFormat.getSigValue());
    }

    @Data
    @Builder
    @AllArgsConstructor
    private static class SignaturePart {

        private final String functionName;
        private SignatureOccurrence occurrence;
        private Integer parameter;

        public enum SignatureOccurrence {
            SLICE, SPLICE, REVERSE, SWAP
        }
    }

    private static class SignatureResolver {

        private final List<SignaturePart> signatureParts;

        public SignatureResolver() {
            this.signatureParts = new ArrayList<>();
        }

        private void addSignaturePart(SignaturePart signaturePart) {
            signatureParts.add(signaturePart);
        }

        private String resolveSignature(String signatureValue) {
            StringBuilder signature = new StringBuilder(signatureValue);
            for (SignaturePart signaturePart : signatureParts) {
                Integer parameter = signaturePart.getParameter();
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
                }
            }
            return signature.toString();
        }
    }

}
