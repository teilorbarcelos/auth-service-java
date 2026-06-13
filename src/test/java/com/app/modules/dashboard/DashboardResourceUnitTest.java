package com.app.modules.dashboard;

import com.app.utils.TestUtils;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@QuarkusTest
public class DashboardResourceUnitTest {

    @Test
    @DisplayName("Given authenticated admin, when requesting dashboard stats, then returns 200 and stats")
    void testGetStatsSuccess() {
        String token = TestUtils.getAdminToken();

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .when()
                .get("/v1/dashboard/stats")
                .then()
                .statusCode(200)
                .body("userCreationStats", notNullValue())
                .body("productCreationStats", notNullValue())
                .body("productsPerUser", notNullValue());
    }

    @Test
    @DisplayName("Given unauthenticated request, when requesting dashboard stats, then returns 401")
    void testGetStatsUnauthenticated() {
        given()
                .contentType(ContentType.JSON)
                .when()
                .get("/v1/dashboard/stats")
                .then()
                .statusCode(401);
    }

    @Test
    @DisplayName("Test DashboardService helper date parsing methods")
    void testDateParsing() {
        LocalDateTime start1 = DashboardService.parseStartDate(null);
        LocalDateTime start2 = DashboardService.parseStartDate("");
        LocalDateTime start3 = DashboardService.parseStartDate("invalid");
        LocalDateTime start4 = DashboardService.parseStartDate("2026-05-23");

        assertNotNull(start1);
        assertNotNull(start2);
        assertNotNull(start3);
        assertNotNull(start4);

        LocalDateTime end1 = DashboardService.parseEndDate(null);
        LocalDateTime end2 = DashboardService.parseEndDate("");
        LocalDateTime end3 = DashboardService.parseEndDate("invalid");
        LocalDateTime end4 = DashboardService.parseEndDate("2026-05-23");

        assertNotNull(end1);
        assertNotNull(end2);
        assertNotNull(end3);
        assertNotNull(end4);
    }

    @Test
    void testConstructor() {
        new DashboardResource(null);
    }
}
