package io.sfrei.tracksearch_web.problems;

import io.sfrei.tracksearch.exceptions.TrackSearchException;
import org.zalando.problem.AbstractThrowableProblem;
import org.zalando.problem.Status;
import org.zalando.problem.StatusType;

import java.net.URI;

public class TrackSearchProblem extends AbstractThrowableProblem {

    public TrackSearchProblem(String title, StatusType statusType, String message, URI uri) {
        super(uri, title, statusType, message);
    }

    public TrackSearchProblem(String title, Exception exception) {
        this(
                title,
                Status.INTERNAL_SERVER_ERROR,
                exception.getMessage(),
                null
        );
    }

    public TrackSearchProblem(TrackSearchException exception) {
        this(
                "TrackSearch Exception",
                Status.INTERNAL_SERVER_ERROR,
                exception.getMessage(),
                null
        );
    }

    public TrackSearchProblem(String message) {
        this(
                "TrackSearch Exception",
                Status.INTERNAL_SERVER_ERROR,
                message,
                null
        );
    }

}
