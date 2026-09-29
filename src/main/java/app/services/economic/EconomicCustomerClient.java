package app.services.economic;

import app.dto.EconomicApiErrorResponse;
import app.dto.EconomicCustomerListResponse;
import app.dto.EconomicCustomerRequest;
import app.dto.EconomicCustomerResponse;
import app.exceptions.ApiException;
import app.services.routeSecurity.ObjectMapperService;
import app.utils.Utils;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class EconomicCustomerClient {
    private static final Logger logger = LoggerFactory.getLogger(EconomicCustomerClient.class);
    private static final String DEFAULT_BASE_URL = "https://restapi.e-conomic.com";
    private final ObjectMapper objectMapper = ObjectMapperService.getMapper();
    private final HttpClient httpClient;
    private final String baseUrl;
    private final String appSecretToken;
    private final String agreementGrantToken;

    public EconomicCustomerClient() {
        this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build(),
                getConfigValue("ECONOMIC_BASE_URL", DEFAULT_BASE_URL),
                getConfigValue("ECONOMIC_APP_SECRET_TOKEN", null),
                getConfigValue("ECONOMIC_AGREEMENT_GRANT_TOKEN", null));
    }

    public EconomicCustomerClient(HttpClient httpClient, String baseUrl, String appSecretToken, String agreementGrantToken) {
        this.httpClient = httpClient;
        this.baseUrl = stripTrailingSlash(baseUrl == null || baseUrl.isBlank() ? DEFAULT_BASE_URL : baseUrl.trim());
        this.appSecretToken = appSecretToken;
        this.agreementGrantToken = agreementGrantToken;
    }

    public EconomicCustomerListResponse listCustomers(int pageSize, int skipPages) {
        HttpRequest request = requestBuilder("/customers?pagesize=" + pageSize + "&skippages=" + skipPages)
                .GET()
                .build();
        return send(request, EconomicCustomerListResponse.class, "listEconomicCustomers");
    }

    public EconomicCustomerResponse getCustomer(Integer customerNumber) {
        String encodedCustomerNumber = URLEncoder.encode(String.valueOf(customerNumber), StandardCharsets.UTF_8);
        HttpRequest request = requestBuilder("/customers/" + encodedCustomerNumber)
                .GET()
                .build();
        return send(request, EconomicCustomerResponse.class, "getEconomicCustomer");
    }

    public EconomicCustomerResponse createCustomer(EconomicCustomerRequest customerRequest, String idempotencyKey) {
        try {
            HttpRequest request = requestBuilder("/customers")
                    .header("Idempotency-Key", idempotencyKey)
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(customerRequest)))
                    .build();
            return send(request, EconomicCustomerResponse.class, "createEconomicCustomer");
        } catch (IOException e) {
            throw new ApiException(500, "Could not serialize e-conomic customer request");
        }
    }

    private HttpRequest.Builder requestBuilder(String pathAndQuery) {
        requireCredentials();
        return HttpRequest.newBuilder(URI.create(baseUrl + pathAndQuery))
                .timeout(Duration.ofSeconds(20))
                .header("X-AppSecretToken", appSecretToken)
                .header("X-AgreementGrantToken", agreementGrantToken)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json");
    }

    private <T> T send(HttpRequest request, Class<T> responseType, String operationName) {
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return objectMapper.readValue(response.body(), responseType);
            }
            throw convertError(response, operationName);
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            logger.warn("operation={} upstreamStatus=network economicLogId=unavailable", operationName, e);
            throw new ApiException(502, "Could not connect to e-conomic");
        }
    }

    private ApiException convertError(HttpResponse<String> response, String operationName) {
        EconomicApiErrorResponse error = parseError(response.body());
        String logId = error.getLogId() == null ? "unavailable" : error.getLogId();
        logger.warn("operation={} upstreamStatus={} economicLogId={}", operationName, response.statusCode(), logId);
        return new ApiException(toApplicationStatus(response.statusCode()), safeMessage(response.statusCode()));
    }

    private EconomicApiErrorResponse parseError(String body) {
        try {
            return objectMapper.readValue(body == null || body.isBlank() ? "{}" : body, EconomicApiErrorResponse.class);
        } catch (IOException e) {
            return new EconomicApiErrorResponse();
        }
    }

    private int toApplicationStatus(int upstreamStatus) {
        return switch (upstreamStatus) {
            case 400 -> 400;
            case 401 -> 401;
            case 403 -> 403;
            case 404 -> 404;
            case 409 -> 409;
            case 429 -> 429;
            default -> 502;
        };
    }

    private String safeMessage(int upstreamStatus) {
        return switch (upstreamStatus) {
            case 400 -> "e-conomic rejected the customer request";
            case 401 -> "Invalid e-conomic credentials";
            case 403 -> "Insufficient e-conomic permissions";
            case 404 -> "e-conomic customer not found";
            case 409 -> "e-conomic customer conflict";
            case 429 -> "e-conomic rate limit exceeded";
            default -> "e-conomic request failed";
        };
    }

    private void requireCredentials() {
        if (appSecretToken == null || appSecretToken.isBlank() || agreementGrantToken == null || agreementGrantToken.isBlank()) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR.getCode(), "e-conomic credentials are not configured");
        }
    }

    private static String stripTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private static String getConfigValue(String key, String defaultValue) {
        String propertyValue = System.getProperty(key);
        if (propertyValue != null && !propertyValue.isBlank()) return propertyValue.trim();
        String envValue = System.getenv(key);
        if (envValue != null && !envValue.isBlank()) return envValue.trim();
        try {
            return Utils.getPropertyValue(key, "config.properties");
        } catch (ApiException e) {
            return defaultValue;
        }
    }
}
