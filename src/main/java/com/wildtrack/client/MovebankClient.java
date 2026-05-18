package com.wildtrack.client;

import com.wildtrack.exception.MovebankApiException;
import com.wildtrack.exception.MovebankRateLimitException;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class MovebankClient {

    private final RestClient restClient;

    private static final Logger log = LogManager.getLogger(MovebankClient.class);

    public String getData(Long id) {

        if(id==null){
            throw new IllegalArgumentException("Study ID cannot be null");
        }

        return restClient.get()
                .uri("movebank/service/direct-read?entity_type=event&study_id=" + id.toString())
                .header("Accept", "text/csv")
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, response) -> {
                    String body = new String(response.getBody().readAllBytes());
                    if (body.contains("rate limiting")) {
                        log.error("Movebank API limit has been met");
                        throw new MovebankRateLimitException();
                    }
                    log.error("Movebank API call failed with response code {}, and body: {}", response.getStatusCode(), body);
                    throw new MovebankApiException();
                })
                .body(String.class);
    }
}
