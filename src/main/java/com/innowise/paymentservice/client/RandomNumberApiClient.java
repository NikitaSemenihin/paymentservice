package com.innowise.paymentservice.client;

import com.innowise.paymentservice.exception.ExternalServiceException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class RandomNumberApiClient implements RandomNumberClient {
    private final RestClient restClient;
    private final int min;
    private final int max;

    public RandomNumberApiClient(
            RestClient randomNumberRestClient,
            @Value("${app.random-number.min}") int min,
            @Value("${app.random-number.max}") int max
    ) {
        this.restClient = randomNumberRestClient;
        this.min = min;
        this.max = max;
    }

    @Override
    public int getRandomNumber() {
        try {
            int[] response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v1.0/random")
                            .queryParam("min", min)
                            .queryParam("max", max)
                            .queryParam("count", 1)
                            .build())
                    .retrieve()
                    .body(int[].class);

            if (response == null || response.length == 0) {
                throw new ExternalServiceException("Random number API returned an empty response");
            }
            return response[0];
        } catch (Exception exception) {
            throw new ExternalServiceException("Failed to fetch random number", exception);
        }
    }
}
