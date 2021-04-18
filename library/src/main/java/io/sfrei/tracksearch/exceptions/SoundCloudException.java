package io.sfrei.tracksearch.exceptions;

public class SoundCloudException extends TrackSearchException {

    public SoundCloudException(String message) {
        super(message);
    }

    public SoundCloudException(Throwable cause) {
        super(cause);
    }

    public SoundCloudException(String message, Throwable cause) {
        super(message, cause);
    }
}
