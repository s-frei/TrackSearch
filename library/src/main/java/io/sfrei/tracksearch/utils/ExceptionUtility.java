package io.sfrei.tracksearch.utils;

public class ExceptionUtility {

    /**
     * Unified message for failed stream URL resolving after several retries.
     * @param retries the retries which were taken.
     * @return the exception message.
     */
    public static String noStreamUrlAfterRetriesMessage(int retries) {
        return String.format("Not able to get stream URL after %s tries", retries + 1);
    }

}
