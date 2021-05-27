package io.sfrei.tracksearch_web.controller;

import io.sfrei.tracksearch.clients.setup.TrackSource;
import io.sfrei.tracksearch.tracks.SoundCloudTrack;
import io.sfrei.tracksearch.tracks.Track;
import io.sfrei.tracksearch.tracks.TrackList;
import io.sfrei.tracksearch.tracks.YouTubeTrack;
import io.sfrei.tracksearch_web.pojos.TrackDTO;
import io.sfrei.tracksearch_web.problems.TrackSearchProblem;
import io.sfrei.tracksearch_web.services.TrackSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Set;

import static io.sfrei.tracksearch_web.services.TrackSearchService.TrackSearchCookie;

@Tag(name = "TrackSearch Controller")
@Validated
@RestController
@RequestMapping("/ts")
@RequiredArgsConstructor
public class TrackSearchController {

    private final TrackSearchService trackSearchService;

    @GetMapping("/search")
    @Operation(summary = "Search for tracks my key words on provided sources")
    public Mono<ResponseEntity<Flux<TrackDTO>>> searchTracks(@Parameter(description = "Key words to search for")
                                                             @RequestParam String query,
                                                             @Parameter(description = "Sources to search on (defaults to all)")
                                                             @RequestParam(required = false) Set<TrackSource> sources) {

        return Mono
                .just(query)
                .map(q -> trackSearchService.getTracksForSearch(q, sources))
                .flatMap(this::tracksResponse);
    }

    @GetMapping("/next")
    @Operation(summary = "Get next tracks for last search (set cookies required)")
    public Mono<ResponseEntity<Flux<TrackDTO>>> nextTracks(@Parameter(hidden = true)
                                                           @CookieValue(TrackSearchCookie.QUERY_TYPE) String qt,
                                                           @Parameter(hidden = true)
                                                           @CookieValue(TrackSearchCookie.QUERY_INFORMATION) String qi) {

        return Mono
                .just(trackSearchService.cookie().tackListFromRequestHeaders(qt, qi))
                .map(trackSearchService::getNextTracks)
                .flatMap(this::tracksResponse);
    }

    @PostMapping("/stream")
    @Operation(summary = "Get stream URL for provided track")
    public Mono<ResponseEntity<String>> getStreamUrl(@Validated @RequestBody TrackDTO pojo) {
        return Mono
                .just(pojo)
                .map(this::fromDTO)
                .map(track -> ResponseEntity.ok(trackSearchService.getStreamUrl(track)));
    }

    private Mono<ResponseEntity<Flux<TrackDTO>>> tracksResponse(TrackList<Track> trackList) {
        return Mono
                .just(ResponseEntity.ok()
                        .headers(httpHeaders -> httpHeaders.addAll(trackSearchService.cookie()
                                .getCookieResponseHeaders(trackList)))
                        .body(toDTOs(trackList))
                );
    }

    private Flux<TrackDTO> toDTOs(TrackList<Track> trackList) {
        return Flux
                .fromIterable(trackList.getTracks())
                .map(track ->
                        TrackDTO.builder()
                                .title(track.getTitle()).cleanTitle(track.getCleanTitle())
                                .trackSource(track.getSource())
                                .length(track.getLength())
                                .url(track.getUrl())
                                .build()
                );
    }

    private Track fromDTO(TrackDTO track) {
        switch (track.getTrackSource()) {
            case Youtube:
                return new YouTubeTrack(track.getTitle(), track.getLength(), track.getUrl(), null);
            case Soundcloud:
                return new SoundCloudTrack(track.getTitle(), track.getLength(), track.getUrl(), null);
        }
        throw new TrackSearchProblem("Unknown track source");
    }

}
