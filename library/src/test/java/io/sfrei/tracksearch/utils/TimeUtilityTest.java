package io.sfrei.tracksearch.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class TimeUtilityTest {

    private static final String HOUR_TIME_STRING = "1:22:45";
    private static final String MINUTE_TIME_STRING = "3:45";

    private static final Long HOUR_TIME_STRING_SECONDS = 4965L;
    private static final Long MINUTE_TIME_STRING_SECONDS = 225L;

    @ParameterizedTest
    @ValueSource(strings = {HOUR_TIME_STRING, MINUTE_TIME_STRING})
    public void canTransform(String timeString) {

        final Long seconds = TimeUtility.getSecondsForTimeString(timeString);

        assertThat(seconds)
                .as("Seconds for %s should not be null", timeString)
                .isNotNull();

        assertThat(seconds)
                .as("Seconds for %s should ge greater than 0", timeString)
                .isGreaterThan(0);

    }

    @Test
    public void secondsCorrect() {

        assertThat(TimeUtility.getSecondsForTimeString(HOUR_TIME_STRING))
                .as("Seconds for %s should be exact", HOUR_TIME_STRING)
                .isEqualTo(HOUR_TIME_STRING_SECONDS);

        assertThat(TimeUtility.getSecondsForTimeString(MINUTE_TIME_STRING))
                .as("Seconds for %s should be exact", MINUTE_TIME_STRING)
                .isEqualTo(MINUTE_TIME_STRING_SECONDS);

    }

}
