package com.app.modules.role;

import com.app.utils.TestUtils;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.is;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class RoleResourceTest {

    private static String token;
    private static String roleId;

    @BeforeEach
    public void setup() {
        if (token == null) {
            token = TestUtils.getAdminToken();
        }
    }

    @Test
    @Order(1)
    @DisplayName("Should list roles")
    public void testListRoles() {
        given()
            .header("Authorization", "Bearer " + token)
        .when()
            .get("/v1/role")
        .then()
            .statusCode(200)
            .body("items", notNullValue());
    }

    @Test
    @Order(2)
    @DisplayName("Should create a new role")
    public void testCreateRole() {
        String uniqueId = "role-" + System.currentTimeMillis();
        
        RoleModel role = new RoleModel();
        role.setId(uniqueId);
        role.setName("Test Role");
        role.setDescription("Description for test role");

        roleId = given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(role)
        .when()
            .post("/v1/role")
        .then()
            .statusCode(201)
            .body("id", is(uniqueId))
            .extract().path("id");
    }

    @Test
    @Order(3)
    @DisplayName("Should list features")
    public void testListFeatures() {
        given()
            .header("Authorization", "Bearer " + token)
        .when()
            .get("/v1/role/features")
        .then()
            .statusCode(200)
            .body("$", notNullValue());
    }

    @Test
    @Order(4)
    @DisplayName("Should update role with permissions")
    public void testUpdateRole() {
        Map<String, Object> body = Map.of(
            "name", "Updated Role Name",
            "permissions", java.util.List.of(
                Map.of("id_feature", "user", "view", true)
            )
        );

        given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(body)
        .when()
            .put("/v1/role/" + roleId)
        .then()
            .statusCode(200)
            .body("name", is("Updated Role Name"));
    }

    @Test
    @Order(5)
    @DisplayName("Should return 404 for non-existent role")
    public void testUpdateNonExistentRole() {
        given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(Map.of("name", "Ghost"))
        .when()
            .put("/v1/role/ghost-id")
        .then()
            .statusCode(404);
    }

    @Test
    @Order(6)
    @DisplayName("Should list all roles without pagination")
    public void testListAllRoles() {
        given()
            .header("Authorization", "Bearer " + token)
        .when()
            .get("/v1/role/all")
        .then()
            .statusCode(200)
            .body("items", notNullValue());
    }

    @Test
    @Order(7)
    @DisplayName("Should get role by ID")
    public void testGetRoleById() {
        given()
            .header("Authorization", "Bearer " + token)
        .when()
            .get("/v1/role/" + roleId)
        .then()
            .statusCode(200)
            .body("id", is(roleId));
    }

    @Test
    @Order(8)
    @DisplayName("Should toggle role status")
    public void testToggleRoleStatus() {
        given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(new RoleSchemas.StatusRequest(false))
        .when()
            .patch("/v1/role/" + roleId + "/status")
        .then()
            .statusCode(200)
            .body("active", is(false));
    }

    @Test
    @Order(9)
    @DisplayName("Should toggle role status with null body (defaults to active)")
    public void testToggleRoleStatusWithNullBody() {
        given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
        .when()
            .patch("/v1/role/" + roleId + "/status")
        .then()
            .statusCode(200)
            .body("active", is(true));
    }

    @Test
    @Order(10)
    @DisplayName("Should delete role")
    public void testDeleteRole() {
        given()
            .header("Authorization", "Bearer " + token)
        .when()
            .delete("/v1/role/" + roleId)
        .then()
            .statusCode(204);
    }
}
