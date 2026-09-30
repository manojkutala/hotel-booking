package com.example.hotelbooking;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"debug=false", "logging.level.root=WARN", "spring.main.banner-mode=off"})
@Import(BookingFlowTest.FixedClockConfiguration.class)
class BookingFlowTest {
    @Value("${local.server.port}")
    private int port;

    @Autowired
    private ObjectMapper json;

    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    @AfterEach
    void closeClient() {
        client.close();
    }

    @Test
    void onboardSearchBookPayCancelAndSearchAgain() throws Exception {
        String city = "Flow-" + UUID.randomUUID();
        var property = onboard(city);
        String propertyId = property.path("id").asString();
        String roomId = property.path("roomTypes").get(0).path("id").asString();
        String search = "/properties?city=" + city + "&checkIn=2030-01-10&checkOut=2030-01-12&guests=2"
                + "&amenities=WIFI&minStars=4&minPrice=2000&maxPrice=2000";
        assertThat(get(search, 200).size()).isEqualTo(1);
        assertThat(get("/properties/" + propertyId, 200).path("ownerId").asString()).isNotBlank();

        var booking = post("/bookings", bookingRequest(propertyId, roomId), 201);
        String bookingPath = "/bookings/" + booking.path("id").asString();
        assertThat(booking.path("status").asString()).isEqualTo("PENDING_PAYMENT");
        assertThat(booking.path("total").path("amount").decimalValue()).isEqualByComparingTo("4000");
        assertThat(booking.path("total").path("currency").asString()).isEqualTo("INR");
        assertThat(get(search, 200).size()).isZero();
        assertThat(post("/bookings", bookingRequest(propertyId, roomId), 409).path("code").asString())
                .isEqualTo("NO_AVAILABILITY");

        var declined = post(bookingPath + "/payments", Map.of("method", "CARD", "paymentToken", "decline"), 200);
        assertThat(declined.path("status").asString()).isEqualTo("PENDING_PAYMENT");
        assertThat(declined.path("payments").get(0).path("status").asString()).isEqualTo("FAILED");
        var paid = post(bookingPath + "/payments", Map.of("method", "UPI", "paymentToken", "ok"), 200);
        assertThat(paid.path("status").asString()).isEqualTo("CONFIRMED");
        var repeatedPayment = post(bookingPath + "/payments", Map.of("method", "UPI", "paymentToken", "ok"), 200);
        assertThat(repeatedPayment).isEqualTo(paid);

        var cancelled = post(bookingPath + "/cancellations", null, 200);
        assertThat(cancelled.path("status").asString()).isEqualTo("CANCELLED");
        assertThat(cancelled.path("refund").path("amount").path("amount").decimalValue())
                .isEqualByComparingTo("4000");
        assertThat(post(bookingPath + "/cancellations", null, 200)).isEqualTo(cancelled);
        assertThat(get(bookingPath, 200)).isEqualTo(cancelled);
        assertThat(get(search, 200).size()).isEqualTo(1);
        assertThat(post(bookingPath + "/payments", Map.of("method", "CARD", "paymentToken", "ok"), 409)
                .path("code").asString()).isEqualTo("INVALID_STATE");
    }

    @Test
    void refundFailureRetainsInventoryAndConfirmedState() throws Exception {
        String city = "Refund-" + UUID.randomUUID();
        var property = onboard(city);
        var booking = post("/bookings", bookingRequest(property.path("id").asString(),
                property.path("roomTypes").get(0).path("id").asString()), 201);
        String path = "/bookings/" + booking.path("id").asString();
        post(path + "/payments", Map.of("method", "WALLET", "paymentToken", "refund-fail"), 200);
        assertThat(post(path + "/cancellations", null, 502).path("code").asString()).isEqualTo("REFUND_FAILED");
        assertThat(get(path, 200).path("status").asString()).isEqualTo("CONFIRMED");
        assertThat(get("/properties?city=" + city + "&checkIn=2030-01-10&checkOut=2030-01-12&guests=1", 200).size())
                .isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/properties",
            "/properties?checkIn=not-a-date&checkOut=2030-01-12&guests=1",
            "/properties?checkIn=2030-01-10&checkOut=2030-01-10&guests=1",
            "/properties?checkIn=2030-01-10&checkOut=2030-01-12&guests=0",
            "/properties?checkIn=2030-01-10&checkOut=2030-01-12&guests=1&minPrice=100&maxPrice=99",
            "/properties?checkIn=2029-12-31&checkOut=2030-01-12&guests=1",
            "/properties?checkIn=2030-01-10&checkOut=2030-01-12&guests=1&minStars=6",
            "/bookings/not-a-uuid"
    })
    void invalidQueriesReturnConsistentClientErrors(String path) throws Exception {
        assertThat(get(path, 400).path("code").asString()).isEqualTo("INVALID_REQUEST");
    }

    @Test
    void validatesBodiesAndReportsMissingResources() throws Exception {
        var error = post("/owners", Map.of("name", "", "email", "invalid"), 400);
        assertThat(error.path("fieldErrors").has("name")).isTrue();
        assertThat(error.path("fieldErrors").has("email")).isTrue();
        assertThat(post("/bookings", Map.of(), 400).path("code").asString()).isEqualTo("INVALID_REQUEST");
        assertThat(get("/bookings/" + UUID.randomUUID(), 404).path("code").asString()).isEqualTo("NOT_FOUND");
        var owner = post("/owners", Map.of("name", "Owner", "email", "owner@example.com"), 201);
        var invalidProperty = Map.of("name", "Hotel", "city", "City", "locality", "Locality", "stars", 4,
                "amenities", List.of("wifi"), "roomTypes", List.of(Map.of("name", "Room", "guestCapacity", 0,
                        "totalRooms", 1, "nightlyPrice", 100)));
        var nestedError = post("/owners/" + owner.path("id").asString() + "/properties", invalidProperty, 400);
        assertThat(nestedError.path("fieldErrors").has("roomTypes[0].guestCapacity")).isTrue();
    }

    private JsonNode onboard(String city) throws Exception {
        var owner = post("/owners", Map.of("name", "Hotel Group", "email", "owner@example.com"), 201);
        return post("/owners/" + owner.path("id").asString() + "/properties",
                Map.of("name", "Garden Hotel", "city", city, "locality", "Centre", "stars", 4,
                        "amenities", List.of("wifi", "parking"), "roomTypes", List.of(Map.of(
                                "name", "Deluxe", "guestCapacity", 2, "totalRooms", 1, "nightlyPrice", 2000))), 201);
    }

    private Map<String, Object> bookingRequest(String propertyId, String roomId) {
        return Map.of("propertyId", propertyId, "roomTypeId", roomId,
                "checkIn", "2030-01-10", "checkOut", "2030-01-12", "guests", 2,
                "guestName", "Guest", "guestEmail", "guest@example.com");
    }

    private JsonNode get(String path, int status) throws Exception {
        return send(HttpRequest.newBuilder(uri(path)).timeout(Duration.ofSeconds(10)).GET().build(), status);
    }

    private JsonNode post(String path, Object body, int status) throws Exception {
        var publisher = body == null ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body));
        return send(HttpRequest.newBuilder(uri(path)).timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json").POST(publisher).build(), status);
    }

    private JsonNode send(HttpRequest request, int expectedStatus) throws Exception {
        var response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).as("%s %s: %s", request.method(), request.uri(), response.body())
                .isEqualTo(expectedStatus);
        return json.readTree(response.body());
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class FixedClockConfiguration {
        @Bean
        @Primary
        Clock testClock() {
            return TestFixture.CLOCK;
        }
    }
}
