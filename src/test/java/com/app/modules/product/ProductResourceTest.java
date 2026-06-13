package com.app.modules.product;

import com.app.utils.TestUtils;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ProductResourceTest {

    private static String token;
    private static String productId;

    @BeforeEach
    public void setup() {
        if (token == null) {
            token = TestUtils.getAdminToken();
        }
    }

    @Test
    @Order(1)
    @DisplayName("Should list products")
    public void testListProducts() {
        given()
            .header("Authorization", "Bearer " + token)
        .when()
            .get("/v1/product")
        .then()
            .statusCode(200)
            .body("items", notNullValue())
            .body("total", greaterThanOrEqualTo(0));
    }

    @Test
    @Order(2)
    @DisplayName("Should create a new product")
    public void testCreateProduct() {
        ProductModel product = new ProductModel();
        product.setName("Test Product");
        product.setSku("PROD-" + System.currentTimeMillis());
        product.setPrice(new java.math.BigDecimal("99.90"));
        product.setActive(true);

        productId = given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(product)
        .when()
            .post("/v1/product")
        .then()
            .statusCode(201)
            .body("id", notNullValue())
            .body("name", is("Test Product"))
            .extract().path("id");
    }

    @Test
    @Order(3)
    @DisplayName("Should update product")
    public void testUpdateProduct() {
        ProductModel update = new ProductModel();
        update.setName("Updated Product");
        update.setSku("PROD-UPDATED-" + System.currentTimeMillis());
        update.setPrice(new java.math.BigDecimal("150.00"));
        update.setDescription("Updated Description");
        update.setCategory("Electronics");
        update.setStock(100);
        update.setActive(true);

        given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(update)
        .when()
            .put("/v1/product/" + productId)
        .then()
            .statusCode(200)
            .body("name", is("Updated Product"))
            .body("description", is("Updated Description"))
            .body("category", is("Electronics"))
            .body("stock", is(100))
            .body("price", is(notNullValue()));
    }

    @Test
    @Order(4)
    @DisplayName("Should list all products without pagination")
    public void testListAllProducts() {
        given()
            .header("Authorization", "Bearer " + token)
        .when()
            .get("/v1/product/all")
        .then()
            .statusCode(200)
            .body("items", notNullValue());
    }

    @Test
    @Order(5)
    @DisplayName("Should get product by ID")
    public void testGetProductById() {
        given()
            .header("Authorization", "Bearer " + token)
        .when()
            .get("/v1/product/" + productId)
        .then()
            .statusCode(200)
            .body("id", is(productId));
    }

    @Test
    @Order(6)
    @DisplayName("Should toggle product status")
    public void testToggleStatus() {
        given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(Map.of("active", false))
        .when()
            .patch("/v1/product/" + productId + "/status")
        .then()
            .statusCode(200)
            .body("active", is(false));

        // Toggle back to active
        given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(Map.of("active", true))
        .when()
            .patch("/v1/product/" + productId + "/status")
        .then()
            .statusCode(200)
            .body("active", is(true));
    }

    @Test
    @Order(7)
    @DisplayName("Should toggle product status with null body (defaults to active)")
    public void testToggleStatusWithNullBody() {
        given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
        .when()
            .patch("/v1/product/" + productId + "/status")
        .then()
            .statusCode(200)
            .body("active", is(true));
    }

    @Test
    @Order(8)
    @DisplayName("Should return 404 when product not found")
    public void testGetNotFound() {
        given()
            .header("Authorization", "Bearer " + token)
        .when()
            .get("/v1/product/non-existent-id")
        .then()
            .statusCode(404);
    }

    @Test
    @Order(9)
    @DisplayName("Should search products with filters")
    public void testSearchProducts() {
        given()
            .header("Authorization", "Bearer " + token)
            .queryParam("name", "Updated Product")
        .when()
            .get("/v1/product")
        .then()
            .statusCode(200)
            .body("total", is(greaterThanOrEqualTo(1)));
    }

    @Test
    @Order(10)
    @DisplayName("Should search products with global search")
    public void testGlobalSearch() {
        given()
            .header("Authorization", "Bearer " + token)
            .queryParam("searchWord", "Updated")
            .queryParam("searchFields", "name,sku")
        .when()
            .get("/v1/product")
        .then()
            .statusCode(200)
            .body("total", is(greaterThanOrEqualTo(1)));
    }

    @Test
    @Order(11)
    @DisplayName("Should search products by date range")
    public void testSearchByDateRange() {
        String today = java.time.LocalDate.now().toString();
        given()
            .header("Authorization", "Bearer " + token)
            .queryParam("createdAt_start", today)
            .queryParam("createdAt_end", today)
        .when()
            .get("/v1/product")
        .then()
            .statusCode(200)
            .body("total", is(greaterThanOrEqualTo(1)));
    }

    @Test
    @Order(12)
    @DisplayName("Should delete product")
    public void testDeleteProduct() {
        given()
            .header("Authorization", "Bearer " + token)
        .when()
            .delete("/v1/product/" + productId)
        .then()
            .statusCode(204);

        // Verify it's not in the list anymore
        given()
            .header("Authorization", "Bearer " + token)
        .when()
            .get("/v1/product/" + productId)
        .then()
            .statusCode(404);
    }
}
