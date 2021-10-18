package io.sfrei.tracksearch.exceptions;

public class YouTubeException extends TrackSearchException {

    public YouTubeException(String message) {
        super(message);
    }

    public YouTubeException(Throwable cause) {
        super(cause);
    }

    public YouTubeException(String message, Throwable cause) {
        super(message, cause);
    }
}
