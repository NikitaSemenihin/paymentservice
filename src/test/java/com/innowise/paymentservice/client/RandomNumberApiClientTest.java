package com.innowise.paymentservice.client;

import com.innowise.paymentservice.exception.ExternalServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RandomNumberApiClientTest {
    @Mock
    private RestClient restClient;

    @Mock
    private RestClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private RestClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    private RandomNumberApiClient randomNumberApiClient;

    @BeforeEach
    void setUp() {
        randomNumberApiClient = new RandomNumberApiClient(restClient, 0, 100);
    }

    @Test
    void getRandomNumberShouldReturnFirstValueFromResponse() {
        mockHappyPath(new int[]{8});

        int actual = randomNumberApiClient.getRandomNumber();

        assertThat(actual).isEqualTo(8);
    }

    @Test
    void getRandomNumberShouldFailForHttpError() {
        mockRequestChain();
        when(responseSpec.body(int[].class)).thenThrow(new RestClientResponseException(
                "Bad Gateway",
                HttpStatus.BAD_GATEWAY.value(),
                HttpStatus.BAD_GATEWAY.getReasonPhrase(),
                null,
                null,
                StandardCharsets.UTF_8
        ));

        assertThatThrownBy(() -> randomNumberApiClient.getRandomNumber())
                .isInstanceOf(ExternalServiceException.class)
                .hasMessageContaining("HTTP 502");
    }

    @Test
    void getRandomNumberShouldFailForNetworkError() {
        mockRequestChain();
        when(responseSpec.body(int[].class)).thenThrow(new ResourceAccessException("Connection refused"));

        assertThatThrownBy(() -> randomNumberApiClient.getRandomNumber())
                .isInstanceOf(ExternalServiceException.class)
                .hasMessageContaining("unavailable");
    }

    @Test
    void getRandomNumberShouldFailForUnexpectedError() {
        mockRequestChain();
        when(responseSpec.body(int[].class)).thenThrow(new IllegalStateException("boom"));

        assertThatThrownBy(() -> randomNumberApiClient.getRandomNumber())
                .isInstanceOf(ExternalServiceException.class)
                .hasMessageContaining("Unexpected error");
    }

    private void mockHappyPath(int[] response) {
        mockRequestChain();
        when(responseSpec.body(int[].class)).thenReturn(response);
    }

    @SuppressWarnings("unchecked")
    private void mockRequestChain() {
        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(java.util.function.Function.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
    }
}
