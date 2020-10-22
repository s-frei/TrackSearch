package io.sfrei.tracksearch.utils;

import io.sfrei.tracksearch.exceptions.TrackSearchException;
import io.sfrei.tracksearch.tracks.YouTubeTrack;
import io.sfrei.tracksearch.tracks.metadata.FormatType;
import io.sfrei.tracksearch.tracks.metadata.YouTubeTrackFormat;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public class TrackFormatUtility {

    public static YouTubeTrackFormat getBestTrackFormat(YouTubeTrack youtubeTrack, boolean includeVideo)
            throws TrackSearchException {

        final AtomicReference<YouTubeTrackFormat> bestFormat = new AtomicReference<>(null);
        final List<YouTubeTrackFormat> formats = youtubeTrack.getTrackInfo().getFormats();
        for (YouTubeTrackFormat trackFormat : formats) {

            final FormatType formatType = trackFormat.getFormatType();

            if (formatType == null || formatType.equals(FormatType.Unknown))
                continue;

            if (!formatType.equals(FormatType.Audio) && !includeVideo)
                continue;

            if (trackFormat.getAudioQuality() == null && !includeVideo) {
                continue;
            }

            if (bestFormat.get() == null) {
                bestFormat.set(trackFormat);
                continue;
            }

            final String currentAudioQuality = bestFormat.get().getAudioQuality();
            final String anotherAudioQuality = trackFormat.getAudioQuality();

            final boolean sameQuality = YoutubeAudioQualities.audioQualitySame(currentAudioQuality, anotherAudioQuality);

            if (!sameQuality) {
                if (YoutubeAudioQualities.audioQualityBetter(currentAudioQuality, anotherAudioQuality)) {
                    bestFormat.set(trackFormat);
                    continue;
                }
            }

            if (trackFormat.getAudioSampleRate() == null)
                continue;

            final int currentSampleRate = Integer.parseInt(bestFormat.get().getAudioSampleRate());
            final int anotherSampleRate = Integer.parseInt(trackFormat.getAudioSampleRate());
            if (currentSampleRate < anotherSampleRate && sameQuality) {
                bestFormat.set(trackFormat);
            }
        }

        if (bestFormat.get() != null) {
            return bestFormat.get();
        }

        if (!includeVideo) {
            log.warn("No audio mime type found for: {} - {} - trying to get the video as alternative",
                    youtubeTrack.getCleanTitle(), youtubeTrack.getUrl());
            return getBestTrackFormat(youtubeTrack, true);
        }

        throw new TrackSearchException("Could not get applicable track format");
    }

    private enum YoutubeAudioQualities {
        LOW("AUDIO_QUALITY_LOW"),
        MEDIUM("AUDIO_QUALITY_MEDIUM"),
        HIGH("AUDIO_QUALITY_HIGH"); //never seen

        final String qualityId;

        YoutubeAudioQualities(String qualityId) {
            this.qualityId = qualityId;
        }

        public String getQualityId() {
            return qualityId;
        }

        private static int getOrdinalForQuality(String qualityId) {
            for (YoutubeAudioQualities quality : YoutubeAudioQualities.values()) {
                if (quality.getQualityId().equals(qualityId))
                    return quality.ordinal();
            }
            return -1;
        }

        public static boolean audioQualitySame(String current, String other) {
            final int currentQuality = YoutubeAudioQualities.getOrdinalForQuality(current);
            final int otherQuality = YoutubeAudioQualities.getOrdinalForQuality(other);
            return otherQuality == currentQuality;
        }

        public static boolean audioQualityBetter(String current, String other) {
            final int currentQuality = YoutubeAudioQualities.getOrdinalForQuality(current);
            final int otherQuality = YoutubeAudioQualities.getOrdinalForQuality(other);
            return otherQuality > currentQuality;
        }
    }

}
