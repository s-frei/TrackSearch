/*
 * Copyright (C) 2023 s-frei (sfrei.io)
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

import io.sfrei.tracksearch.exceptions.TrackSearchException;
import io.sfrei.tracksearch.tracks.YouTubeTrack;
import io.sfrei.tracksearch.tracks.metadata.FormatType;
import io.sfrei.tracksearch.tracks.metadata.YouTubeTrackFormat;
import lombok.Getter;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@UtilityClass
public class TrackFormatUtility {

    public YouTubeTrackFormat getBestTrackFormat(final YouTubeTrack youtubeTrack, final boolean includeVideo)
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

            if (!sameQuality && YoutubeAudioQualities.audioQualityBetter(currentAudioQuality, anotherAudioQuality)) {
                bestFormat.set(trackFormat);
                continue;
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

    @Getter
    private enum YoutubeAudioQualities {
        LOW("AUDIO_QUALITY_LOW"),
        MEDIUM("AUDIO_QUALITY_MEDIUM"),
        HIGH("AUDIO_QUALITY_HIGH"); //never seen

        final String qualityId;

        YoutubeAudioQualities(String qualityId) {
            this.qualityId = qualityId;
        }

        private static int getOrdinalForQuality(final String qualityId) {
            for (final YoutubeAudioQualities quality : YoutubeAudioQualities.values()) {
                if (quality.getQualityId().equals(qualityId))
                    return quality.ordinal();
            }
            return -1;
        }

        public static boolean audioQualitySame(final String current, final String other) {
            final int currentQuality = YoutubeAudioQualities.getOrdinalForQuality(current);
            final int otherQuality = YoutubeAudioQualities.getOrdinalForQuality(other);
            return otherQuality == currentQuality;
        }

        public static boolean audioQualityBetter(final String current, final String other) {
            final int currentQuality = YoutubeAudioQualities.getOrdinalForQuality(current);
            final int otherQuality = YoutubeAudioQualities.getOrdinalForQuality(other);
            return otherQuality > currentQuality;
        }
    }

}
