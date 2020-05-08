package io.sfrei.tracksearch.clients.setup;

import io.sfrei.tracksearch.exceptions.ResponseException;
import io.sfrei.tracksearch.exceptions.TrackSearchException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
public class ResponseWrapper {

    private int code;
    private String content;

    public boolean hasContent() {
        return content != null;
    }

    public boolean isHttpCode(int code) {
        return this.code == code;
    }

    public static ResponseWrapper empty() {
        return new ResponseWrapper();
    }

    public String getContentOrThrow() throws TrackSearchException {
        if (hasContent())
            return content;
        throw new ResponseException("No content - code: " + code);
    }

    public String getContent() {
        return this.content;
    }

}
