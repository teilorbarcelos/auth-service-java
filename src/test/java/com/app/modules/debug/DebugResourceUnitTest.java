package com.app.modules.debug;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;

@QuarkusTest
class DebugResourceUnitTest {

    @Test
    void testTriggerError() {
        String token = com.app.utils.TestUtils.getAdminToken();
        given()
                .header("Authorization", "Bearer " + token)
                .when().get("/v1/debug/error")
                .then()
                .statusCode(500);
    }

    @Test
    void testConstructor() {
        // Covers the default constructor line
        new DebugResource();
    }
}
