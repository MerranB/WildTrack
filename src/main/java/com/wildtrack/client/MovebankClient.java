package com.wildtrack.client;

import com.wildtrack.exception.MovebankApiException;
import com.wildtrack.exception.MovebankRateLimitException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Component
@RequiredArgsConstructor
public class MovebankClient {

    private final RestClient restClient;

    private static final Logger log = LoggerFactory.getLogger(MovebankClient.class);

    public Path getData(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Study ID cannot be null");
        }

        Path target;
        try {
            target = Files.createTempFile("movebank-" + id + "-", ".csv");
        } catch (IOException _) {
            throw new MovebankApiException();
        }

        return restClient.get()
                .uri("movebank/service/direct-read?entity_type=event&study_id=" + id.toString())
                .header("Accept", "text/csv")
                .exchange((request, response) -> {
                    if (response.getStatusCode().isError()) {
                        String body = new String(response.getBody().readAllBytes());
                        if (body.contains("rate limiting")) {
                            log.error("Movebank API limit has been met");
                            throw new MovebankRateLimitException();
                        }
                        log.error("Movebank API call failed with response code {}, and body: {}",
                                response.getStatusCode(), body);
                        throw new MovebankApiException();
                    }
                    Files.copy(response.getBody(), target, StandardCopyOption.REPLACE_EXISTING);
                    log.info("Study {} downloaded to {} ({} bytes)", id, target, Files.size(target));
                    return target;
                });
    }
}
