package io.sfrei.tracksearch.utils;

import io.sfrei.tracksearch.clients.setup.QueryType;

public class ExceptionUtility {

    /**
     * Unified message for failed stream URL resolving after several retries.
     * @param retries the retries which were taken.
     * @return the exception message.
     */
    public static String noStreamUrlAfterRetriesMessage(int retries) {
        return String.format("Not able to get stream URL after %s tries", retries + 1);
    }

    /**
     * Unified message when query type is not supported for some circumstances.
     * @param queryType the unsupported query type.
     * @return the exception message.
     */
    public static String unsupportedQueryTypeMessage(QueryType queryType) {
        return String.format("Query type %s not supported", queryType);
    }

}
