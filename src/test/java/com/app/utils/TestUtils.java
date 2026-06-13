package com.app.utils;

import io.restassured.http.ContentType;
import java.util.HashMap;
import java.util.Map;
import static io.restassured.RestAssured.given;

public class TestUtils {

    public static String getAuthToken(String email, String password) {
        Map<String, String> credentials = new HashMap<>();
        credentials.put("email", email);
        credentials.put("password", password);

        return given()
                .contentType(ContentType.JSON)
                .body(credentials)
            .when()
                .post("/v1/auth/login")
            .then()
                .statusCode(200)
                .extract()
                .path("token");
    }

    public static String getAdminToken() {
        return getAuthToken("admin@email.com", "admin@123");
    }
}
