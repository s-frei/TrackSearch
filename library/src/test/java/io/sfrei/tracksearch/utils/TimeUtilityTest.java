package io.sfrei.tracksearch.utils;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;

import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;

class TimeUtilityTest {

    private static final String HOUR_TIME_STRING = "1:22:45";
    private static final String MINUTE_TIME_STRING = "3:45";

    private static final long HOUR_TIME_STRING_SECONDS = 4965L;
    private static final long MINUTE_TIME_STRING_SECONDS = 225L;

    @ParameterizedTest
    @ValueSource(strings = {HOUR_TIME_STRING, MINUTE_TIME_STRING})
    public void canTransform(String timeString) {

        final Duration duration = TimeUtility.getDurationForTimeString(timeString);

        assertThat(duration)
                .as("Duration for %s should not be null", timeString)
                .isNotNull();

        assertThat(duration)
                .as("Duration for %s should ge greater than 0", timeString)
                .extracting(Duration::toSeconds, as(InstanceOfAssertFactories.LONG))
                .isGreaterThan(0);

    }

    @Test
    public void correctlyTransformed() {

        assertThat(TimeUtility.getDurationForTimeString(HOUR_TIME_STRING))
                .as("Seconds for %s should be exact", HOUR_TIME_STRING)
                .extracting(Duration::toSeconds, as(InstanceOfAssertFactories.LONG))
                .isEqualTo(HOUR_TIME_STRING_SECONDS);

        assertThat(TimeUtility.getDurationForTimeString(MINUTE_TIME_STRING))
                .as("Seconds for %s should be exact", MINUTE_TIME_STRING)
                .extracting(Duration::toSeconds, as(InstanceOfAssertFactories.LONG))
                .isEqualTo(MINUTE_TIME_STRING_SECONDS);

    }

}
