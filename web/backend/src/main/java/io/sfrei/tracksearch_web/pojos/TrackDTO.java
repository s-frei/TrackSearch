package io.sfrei.tracksearch_web.pojos;

import io.sfrei.tracksearch.clients.setup.TrackSource;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@Data
@Builder
@Schema
@NoArgsConstructor
@AllArgsConstructor
public class TrackDTO {

    @Schema(example = "Your artist @ home", description = "Track title")
    private String title;

    @Schema(example = "Your artist at home", description = "Track title without unnecessary stuff")
    private String cleanTitle;

    @NotNull(
            message = "TrackSource is required"
    )
    @Schema(example = "null", description = "Source the track was retrieved from")
    private TrackSource trackSource;

    @Schema(example = "347", description = "Track length in seconds")
    private Long length;

    @NotEmpty(
            message = "URL is required"
    )
    @Schema(example = "https://trackUrl", description = "URL to the original track")
    private String url;

}
