package com.app.modules.health;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

@QuarkusTest
public class HealthResourceTest {

    @Test
    @DisplayName("Should return UP status for health check")
    public void testHealthCheck() {
        given()
        .when()
            .get("/health")
        .then()
            .statusCode(200)
            .body("status", is("UP"));
    }

    @Test
    @DisplayName("Should return welcome message at root")
    public void testRoot() {
        given()
        .when()
            .get("/")
        .then()
            .statusCode(200)
            .body("message", is("API is running"));
    }
}
