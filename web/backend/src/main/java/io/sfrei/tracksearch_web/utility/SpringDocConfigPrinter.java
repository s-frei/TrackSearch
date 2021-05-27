package io.sfrei.tracksearch_web.utility;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.SwaggerUiConfigProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import static org.springdoc.core.Constants.SPRINGDOC_SWAGGER_UI_ENABLED;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = SPRINGDOC_SWAGGER_UI_ENABLED, matchIfMissing = true)
public class SpringDocConfigPrinter {

    private final ServerProperties serverProperties;
    private final SwaggerUiConfigProperties swaggerProperties;

    @EventListener(ApplicationStartedEvent.class)
    public void printSwaggerPath() {
        log.info("");
        log.info("SwaggerUI up and running: (:{}){}",
                serverProperties.getPort(),
                swaggerProperties.getPath());
        log.info("");
    }

}
