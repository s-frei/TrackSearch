package io.sfrei.tracksearch.tracks.metadata;

import lombok.Getter;

@Getter
public enum FormatType {
    Unknown("unknown"),
    Audio("audio"),  //Default
    Video("video");  //Fallback

    private final String typeDef;

    FormatType(String typeDef) {
        this.typeDef = typeDef;
    }

    public static FormatType getFormatType(String mimeTypeDef) {
        String typeDef = mimeTypeDef.toLowerCase();
        if (typeDef.contains(Audio.getTypeDef())) {
            return Audio;
        } else if (typeDef.contains(Video.getTypeDef())) {
            return Video;
        } else
            return Unknown;
    }

}
