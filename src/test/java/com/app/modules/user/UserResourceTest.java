package com.app.modules.user;

import com.app.utils.TestUtils;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.is;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class UserResourceTest {

    private static String token;
    private static String userId;

    @BeforeEach
    public void setup() {
        if (token == null) {
            token = TestUtils.getAdminToken();
        }
    }

    @Test
    @Order(1)
    @DisplayName("Should list users")
    public void testListUsers() {
        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/v1/user")
                .then()
                .statusCode(200)
                .body("items", notNullValue());
    }

    @Test
    @Order(2)
    @DisplayName("Should create a new user")
    public void testCreateUser() {
        String uniqueEmail = "test-" + System.currentTimeMillis() + "@example.com";

        UserModel user = new UserModel();
        user.setName("Test User");
        user.setEmail(uniqueEmail);
        user.setIdRole("user"); // Using default 'user' role from bootstrap
        user.setActive(true);

        userId = given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(user)
                .when()
                .post("/v1/user")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("email", is(uniqueEmail))
                .extract().path("id");
    }

    @Test
    @Order(3)
    @DisplayName("Should update user")
    public void testUpdateUser() {
        UserModel update = new UserModel();
        update.setName("Updated User Name");
        update.setEmail("updated-" + System.currentTimeMillis() + "@example.com");
        update.setIdRole("user");

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(update)
                .when()
                .put("/v1/user/" + userId)
                .then()
                .statusCode(200)
                .body("name", is("Updated User Name"));
    }

    @Test
    @Order(4)
    @DisplayName("Should list all users without pagination")
    public void testListAllUsers() {
        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/v1/user/all")
                .then()
                .statusCode(200)
                .body("items", notNullValue());
    }

    @Test
    @Order(5)
    @DisplayName("Should get user by ID")
    public void testGetUserById() {
        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/v1/user/" + userId)
                .then()
                .statusCode(200)
                .body("id", is(userId));
    }

    @Test
    @Order(6)
    @DisplayName("Should toggle user status")
    public void testToggleUserStatus() {
        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(new UserSchemas.StatusRequest(false))
                .when()
                .patch("/v1/user/" + userId + "/status")
                .then()
                .statusCode(200)
                .body("active", is(false));
    }

    @Test
    @Order(7)
    @DisplayName("Should toggle user status with null body (defaults to active)")
    public void testToggleUserStatusWithNullBody() {
        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .when()
                .patch("/v1/user/" + userId + "/status")
                .then()
                .statusCode(200)
                .body("active", is(true));
    }

    @Test
    @Order(8)
    @DisplayName("Should delete user")
    public void testDeleteUser() {
        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .delete("/v1/user/" + userId)
                .then()
                .statusCode(204);
    }

    @Test
    @Order(9)
    @DisplayName("Should not allow export PDF when unauthenticated")
    public void testExportPdfUnauthorized() {
        given()
                .when()
                .get("/v1/user/export/pdf")
                .then()
                .statusCode(401);
    }

    @Test
    @Order(10)
    @DisplayName("Should export users as PDF successfully")
    public void testExportPdfSuccess() {
        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/v1/user/export/pdf?name=TestUser&orderDirection=asc")
                .then()
                .statusCode(200)
                .header("Content-Type", is("application/pdf"))
                .header("Content-Disposition", is("attachment; filename=\"usuarios.pdf\""))
                .body(notNullValue());
    }

    @Test
    @Order(11)
    @DisplayName("Should export users as PDF with default desc order successfully")
    public void testExportPdfSuccess_Desc() {
        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/v1/user/export/pdf")
                .then()
                .statusCode(200)
                .header("Content-Type", is("application/pdf"))
                .header("Content-Disposition", is("attachment; filename=\"usuarios.pdf\""))
                .body(notNullValue());
    }
}
