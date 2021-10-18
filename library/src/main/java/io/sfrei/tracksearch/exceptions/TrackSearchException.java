package io.sfrei.tracksearch.exceptions;

public class TrackSearchException extends Exception {

    public TrackSearchException(String message) {
        super(message);
    }

    public TrackSearchException(Throwable cause) {
        super(cause);
    }

    public TrackSearchException(String message, Throwable cause) {
        super(message, cause);
    }

}
