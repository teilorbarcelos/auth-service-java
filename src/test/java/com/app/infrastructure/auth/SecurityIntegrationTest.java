package com.app.infrastructure.auth;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;

@QuarkusTest
public class SecurityIntegrationTest {

    @Test
    @DisplayName("Should return 401 when no token is provided")
    public void testUnauthorized() {
        given()
                .when()
                .get("/v1/auth/me")
                .then()
                .statusCode(401);
    }

    @Test
    @DisplayName("Should return 401 when invalid token is provided")
    public void testForbidden() {
        given()
                .header("Authorization", "Bearer invalid_token")
                .when()
                .get("/v1/auth/me")
                .then()
                .statusCode(401);
    }
}
