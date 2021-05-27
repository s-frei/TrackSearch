package io.sfrei.tracksearch_web.config;

import io.sfrei.tracksearch.clients.MultiSearchClient;
import io.sfrei.tracksearch.clients.MultiTrackSearchClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TrackSearchConfig {

    @Bean
    public MultiTrackSearchClient getTrackSearchClient() {
        return new MultiSearchClient();
    }

}
