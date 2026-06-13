package com.app.modules.auth;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.is;

@QuarkusTest
public class AuthResourceTest {

    @Test
    @DisplayName("Should login successfully with valid credentials")
    public void testLoginSuccess() {
        Map<String, String> credentials = new HashMap<>();
        credentials.put("email", "admin@email.com");
        credentials.put("password", "admin@123");

        given()
            .contentType(ContentType.JSON)
            .body(credentials)
        .when()
            .post("/v1/auth/login")
        .then()
            .statusCode(200)
            .body("token", notNullValue())
            .body("refreshToken", notNullValue())
            .body("user.email", is("admin@email.com"));
    }

    @Test
    @DisplayName("Should fail login with invalid password")
    public void testLoginInvalidPassword() {
        Map<String, String> credentials = new HashMap<>();
        credentials.put("email", "admin@email.com");
        credentials.put("password", "wrong_password");

        given()
            .contentType(ContentType.JSON)
            .body(credentials)
        .when()
            .post("/v1/auth/login")
        .then()
            .statusCode(401);
    }
}
