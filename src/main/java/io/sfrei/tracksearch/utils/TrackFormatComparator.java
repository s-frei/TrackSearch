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

package io.sfrei.tracksearch.utils;

import io.sfrei.tracksearch.exceptions.SoundCloudException;
import io.sfrei.tracksearch.exceptions.TrackSearchException;
import io.sfrei.tracksearch.exceptions.YouTubeException;
import io.sfrei.tracksearch.tracks.SoundCloudTrack;
import io.sfrei.tracksearch.tracks.YouTubeTrack;
import io.sfrei.tracksearch.tracks.metadata.MimeType;
import io.sfrei.tracksearch.tracks.metadata.SoundCloudTrackFormat;
import io.sfrei.tracksearch.tracks.metadata.YouTubeTrackFormat;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@UtilityClass
public class TrackFormatComparator {

    private static final String EXCEPTION_MESSAGE = "Could not determine applicable track format";

    public YouTubeTrackFormat getBestYouTubeTrackFormat(final YouTubeTrack youtubeTrack, final boolean includeVideo)
            throws TrackSearchException {

        final AtomicReference<YouTubeTrackFormat> bestFormat = new AtomicReference<>(null);
        final List<YouTubeTrackFormat> formats = youtubeTrack.getTrackInfo().getFormats();

        for (YouTubeTrackFormat trackFormat : formats) {

            final MimeType mimeType = trackFormat.getMimeType();

            if (mimeType == null || mimeType.equals(MimeType.UNKNOWN)) {
                continue;
            }

            if (mimeType.isVideo() && !includeVideo)
                continue;

            if (trackFormat.getAudioQuality() == null && !includeVideo) {
                continue;
            }

            if (bestFormat.get() == null) {
                bestFormat.set(trackFormat);
                continue;
            }

            final String currentAudioQuality = bestFormat.get().getAudioQuality();
            final String audioQuality = trackFormat.getAudioQuality();

            final boolean sameQuality = YouTubeAudioQuality.audioQualitySame(currentAudioQuality, audioQuality);

            if (!sameQuality && YouTubeAudioQuality.audioQualityBetter(currentAudioQuality, audioQuality)) {
                bestFormat.set(trackFormat);
                continue;
            }

            if (!includeVideo && trackFormat.getMimeType().equals(MimeType.AUDIO_WEBM) && !bestFormat.get().getMimeType().equals(MimeType.AUDIO_WEBM) && sameQuality) {
                bestFormat.set(trackFormat);
            }

            if (trackFormat.getAudioSampleRate() == null)
                continue;

            final int currentSampleRate = Integer.parseInt(bestFormat.get().getAudioSampleRate());
            final int sampleRate = Integer.parseInt(trackFormat.getAudioSampleRate());

            if (currentSampleRate < sampleRate && sameQuality) {
                bestFormat.set(trackFormat);
            }
        }

        final YouTubeTrackFormat bestYouTubeTrackFormat = bestFormat.get();
        if (bestYouTubeTrackFormat != null) {
            log.debug("Determined YT track format: {}", bestYouTubeTrackFormat);
            return bestYouTubeTrackFormat;
        }

        if (!includeVideo) {
            log.warn("No audio mime type found for: {} - {} - trying to get the video as alternative",
                    youtubeTrack.getCleanTitle(), youtubeTrack.getUrl());
            return getBestYouTubeTrackFormat(youtubeTrack, true);
        }

        throw new YouTubeException(EXCEPTION_MESSAGE);
    }

    public SoundCloudTrackFormat getBestSoundCloudTrackFormat(final SoundCloudTrack soundCloudTrack) throws SoundCloudException {

        final AtomicReference<SoundCloudTrackFormat> bestFormat = new AtomicReference<>(null);
        final List<SoundCloudTrackFormat> formats = soundCloudTrack.getTrackInfo().getFormats();

        for (SoundCloudTrackFormat trackFormat : formats) {

            if (bestFormat.get() == null) {
                bestFormat.set(trackFormat);
                continue;
            }

            // Available: 'progressive' and 'hls'
            if (trackFormat.getProtocol().equals("hls") && !bestFormat.get().getProtocol().equals("hls")) {
                bestFormat.set(trackFormat);
                continue;
            }

            if (trackFormat.getMimeType().equals(MimeType.AUDIO_OGG) && !bestFormat.get().getMimeType().equals(MimeType.AUDIO_OGG)) {
                bestFormat.set(trackFormat);
            }

        }

        final SoundCloudTrackFormat bestSoundCloudTrackFormat = bestFormat.get();
        if (bestSoundCloudTrackFormat != null) {
            log.debug("Determined SC track format: {}", bestSoundCloudTrackFormat);
            return bestSoundCloudTrackFormat;
        }

        throw new SoundCloudException(EXCEPTION_MESSAGE);
    }

    @Getter
    @RequiredArgsConstructor
    private enum YouTubeAudioQuality {
        LOW("AUDIO_QUALITY_LOW"),
        MEDIUM("AUDIO_QUALITY_MEDIUM"),
        HIGH("AUDIO_QUALITY_HIGH"); //never seen

        final String identifier;

        private static int getOrdinalForQuality(final String identifier) {
            return Arrays.stream(YouTubeAudioQuality.values())
                    .filter(quality -> quality.getIdentifier().equals(identifier))
                    .map(Enum::ordinal)
                    .findFirst()
                    .orElse(-1);
        }

        public static boolean audioQualitySame(final String first, final String second) {
            return YouTubeAudioQuality.getOrdinalForQuality(first) == YouTubeAudioQuality.getOrdinalForQuality(second);
        }

        public static boolean audioQualityBetter(final String current, final String other) {
            return YouTubeAudioQuality.getOrdinalForQuality(other) > YouTubeAudioQuality.getOrdinalForQuality(current);
        }
    }

}