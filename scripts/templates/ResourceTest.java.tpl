package {{package}};

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
public class {{module_name}}ResourceTest {

    private static String token;
    private static String entityId;

    @BeforeEach
    public void setup() {
        if (token == null) {
            token = TestUtils.getAdminToken();
        }
    }

    @Test
    @Order(1)
    @DisplayName("Should list {{module_snake}}s")
    public void testList() {
        given()
            .header("Authorization", "Bearer " + token)
        .when()
            .get("/v1/{{module_snake}}")
        .then()
            .statusCode(200)
            .body("items", notNullValue());
    }

    @Test
    @Order(2)
    @DisplayName("Should list all {{module_snake}}s")
    public void testListAll() {
        given()
            .header("Authorization", "Bearer " + token)
        .when()
            .get("/v1/{{module_snake}}/all")
        .then()
            .statusCode(200)
            .body("items", notNullValue());
    }

    @Test
    @Order(3)
    @DisplayName("Should create {{module_snake}}")
    public void testCreate() {
        {{module_name}}Model entity = new {{module_name}}Model();
{{test_resource_create_setup}}

        entityId = given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(entity)
        .when()
            .post("/v1/{{module_snake}}")
        .then()
            .statusCode(201)
            .body("id", notNullValue())
            .extract().path("id");
    }

    @Test
    @Order(4)
    @DisplayName("Should get {{module_snake}} by ID")
    public void testGetById() {
        given()
            .header("Authorization", "Bearer " + token)
        .when()
            .get("/v1/{{module_snake}}/" + entityId)
        .then()
            .statusCode(200)
            .body("id", is(entityId));
    }

    @Test
    @Order(5)
    @DisplayName("Should update {{module_snake}}")
    public void testUpdate() {
        {{module_name}}Model update = new {{module_name}}Model();
        update.setActive(true);

        given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(update)
        .when()
            .put("/v1/{{module_snake}}/" + entityId)
        .then()
            .statusCode(200)
            .body("id", is(entityId));
    }

    @Test
    @Order(6)
    @DisplayName("Should toggle status")
    public void testToggleStatus() {
        given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(Map.of("active", false))
        .when()
            .patch("/v1/{{module_snake}}/" + entityId + "/status")
        .then()
            .statusCode(200)
            .body("active", is(false));
    }

    @Test
    @Order(7)
    @DisplayName("Should toggle status with null body")
    public void testToggleStatusNullBody() {
        given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
        .when()
            .patch("/v1/{{module_snake}}/" + entityId + "/status")
        .then()
            .statusCode(200)
            .body("active", is(true));
    }

    @Test
    @Order(8)
    @DisplayName("Should delete {{module_snake}}")
    public void testDelete() {
        given()
            .header("Authorization", "Bearer " + token)
        .when()
            .delete("/v1/{{module_snake}}/" + entityId)
        .then()
            .statusCode(204);
    }
}
