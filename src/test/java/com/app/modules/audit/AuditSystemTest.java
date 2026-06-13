package com.app.modules.audit;

import com.app.utils.TestUtils;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@QuarkusTest
public class AuditSystemTest {

    @Inject
    EntityManager em;

    @Inject
    AuditRepository auditRepository;

    @Test
    @DisplayName("Should log error in tb_error_log when a 500 error occurs")
    public void testErrorLogging() {
        String token = TestUtils.getAdminToken();
        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/v1/debug/error")
                .then()
                .statusCode(500);

        // O log de erro é REQUIRES_NEW, então já deve estar no banco
        long count = em.createQuery("SELECT COUNT(e) FROM ErrorLog e", Long.class).getSingleResult();
        assertThat(count, is(greaterThan(0L)));
    }

    @Test
    @DisplayName("Should log action in tb_audit when a modifying action occurs")
    public void testActionAudit() {
        String token = TestUtils.getAdminToken();
        String uniqueSku = "AUDIT-" + System.currentTimeMillis();
        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body("{\"name\":\"Audit Test Item\", \"sku\":\"" + uniqueSku + "\", \"price\":50.0, \"active\":true}")
                .when()
                .post("/v1/product")
                .then()
                .statusCode(201);

        // Verificando se o log foi gerado
        List<AuditModel> audits = em
                .createQuery("SELECT a FROM AuditModel a WHERE a.tableName = 'product' ORDER BY a.createdAt DESC",
                        AuditModel.class)
                .setMaxResults(1)
                .getResultList();

        assertThat(audits, is(not(empty())));
        assertThat(audits.get(0).getActionType(), is("CREATE"));
    }
}
