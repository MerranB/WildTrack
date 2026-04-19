package com.wildtrack.client;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;

@Component
public class MovebankClient {

    private static final Logger log = LogManager.getLogger(MovebankClient.class);
    @Value("${movebank.username}")
    private String MBUS;

    @Value("${movebank.password}")
    private String MBPW;

    CookieManager cookieManager;
    HttpClient client;

    public MovebankClient() {
        this.cookieManager = new CookieManager();
        this.client = HttpClient.newBuilder()
                .cookieHandler(this.cookieManager)
                .build();
    }

    public String getData(Long id) throws RuntimeException {
        String encodedAuth = Base64.getEncoder().encodeToString((MBUS + ":" + MBPW).getBytes());

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://www.movebank.org/movebank/service/direct-read?entity_type=event&study_id=" + id.toString()))
                    .GET().header("Authorization", "Basic " + encodedAuth)
                    .header("Accept", "text/csv")
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            log.error(response.statusCode());
            log.error(response.body());
            throw new RuntimeException(String.valueOf(response.statusCode()));
        } else {
            return "Data retrieved successfully";
        }

        }
        catch (Exception e) {
            log.error(e);
            throw new RuntimeException(e);
        }
    }
}
