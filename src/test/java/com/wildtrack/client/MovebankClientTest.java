package com.wildtrack.client;

import com.wildtrack.exception.MovebankApiException;
import com.wildtrack.exception.MovebankRateLimitException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class MovebankClientTest {

    private MockRestServiceServer server;
    private MovebankClient movebankClient;

    @BeforeEach
    void setup() {
        RestTemplate restTemplate = new RestTemplate();
        server = MockRestServiceServer.createServer(restTemplate);
        RestClient restClient = RestClient.create(restTemplate);
        movebankClient = new MovebankClient(restClient);
    }

    @Test
    void getData_success() throws IOException {
        server.expect(requestTo(containsString("study_id=19186107")))
                .andRespond(withSuccess("timestamp,location_lat", MediaType.TEXT_PLAIN));

        Path downloaded = movebankClient.getData(19186107L);

        try {
            assertThat(downloaded).exists();
            assertThat(Files.readString(downloaded)).contains("timestamp");
        } finally {
            Files.deleteIfExists(downloaded);
        }
    }

    @Test
    void getData_fail() {
        server.expect(requestTo(containsString("study_id=19186107")))
                .andRespond(withServerError());

        assertThrows(MovebankApiException.class, () -> movebankClient.getData(19186107L));
    }

    @Test
    void getData_rateLimited_throwsMovebankRateLimitException() {
        server.expect(requestTo(containsString("study_id=19186107")))
                .andRespond(withServerError()
                        .body("rate limiting")
                        .contentType(MediaType.TEXT_PLAIN));

        assertThrows(MovebankRateLimitException.class, () -> movebankClient.getData(19186107L));
    }

    @Test
    void getData_invalid_ID() {
        assertThrows(IllegalArgumentException.class, () -> movebankClient.getData(null));
    }
}
