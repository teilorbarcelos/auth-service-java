package com.app.modules.feature;

import com.app.utils.TestUtils;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.is;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class FeatureResourceTest {

    private static String token;
    private static final String FEATURE_ID = "user"; // Standard feature from bootstrap

    @BeforeEach
    public void setup() {
        if (token == null) {
            token = TestUtils.getAdminToken();
        }
    }

    @Test
    @Order(1)
    @DisplayName("Should list features with pagination")
    void testListFeatures() {
        given()
            .header("Authorization", "Bearer " + token)
        .when()
            .get("/v1/feature")
        .then()
            .statusCode(200)
            .body("items", notNullValue());
    }

    @Test
    @Order(2)
    @DisplayName("Should list all features without pagination")
    void testListAllFeatures() {
        given()
            .header("Authorization", "Bearer " + token)
        .when()
            .get("/v1/feature/all")
        .then()
            .statusCode(200)
            .body("items", notNullValue());
    }

    @Test
    @Order(3)
    @DisplayName("Should get feature by ID")
    void testGetFeatureById() {
        given()
            .header("Authorization", "Bearer " + token)
        .when()
            .get("/v1/feature/" + FEATURE_ID)
        .then()
            .statusCode(200)
            .body("id", is(FEATURE_ID));
    }

    @Test
    @Order(4)
    @DisplayName("Should toggle feature status")
    void testToggleFeatureStatus() {
        // Toggle to false
        given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(new FeatureSchemas.StatusRequest(false))
        .when()
            .patch("/v1/feature/" + FEATURE_ID + "/status")
        .then()
            .statusCode(200)
            .body("active", is(false));

        // Toggle back to true
        given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(new FeatureSchemas.StatusRequest(true))
        .when()
            .patch("/v1/feature/" + FEATURE_ID + "/status")
        .then()
            .statusCode(200)
            .body("active", is(true));
    }

    @Test
    @Order(5)
    @DisplayName("Should toggle feature status with null body (defaults to active)")
    void testToggleFeatureStatusWithNullBody() {
        given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
        .when()
            .patch("/v1/feature/" + FEATURE_ID + "/status")
        .then()
            .statusCode(200)
            .body("active", is(true));
    }
}
