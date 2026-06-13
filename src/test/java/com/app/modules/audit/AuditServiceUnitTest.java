package com.app.modules.audit;

import com.app.infrastructure.auth.UserSession;
import com.app.modules.user.UserModel;
import com.app.modules.user.UserRepository;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.vertx.core.http.HttpServerRequest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@QuarkusTest
public class AuditServiceUnitTest {

    @Inject
    AuditService auditService;

    @InjectMock
    AuditRepository auditRepository;

    @InjectMock
    ErrorLogRepository errorLogRepository;

    @InjectMock
    UserRepository userRepository;

    @InjectMock
    UserSession userSession;

    @InjectMock
    HttpServerRequest request;

    @Test
    @DisplayName("Should cover audit logging branches")
    public void testAuditLoggingBranches() {
        // Setup request mock
        when(request.method()).thenReturn(io.vertx.core.http.HttpMethod.GET);
        when(request.uri()).thenReturn("/test-uri");
        when(request.remoteAddress()).thenReturn(io.vertx.core.net.SocketAddress.inetSocketAddress(8080, "127.0.0.1"));
        when(request.getHeader("X-Forwarded-For")).thenReturn("10.0.0.1, 192.168.0.1");

        // Case 1: UserSession has ID, User found in DB
        when(userSession.getUserId()).thenReturn("user-123");
        UserModel user = new UserModel();
        user.setName("John Doe");
        when(userRepository.findById("user-123")).thenReturn(user);
        
        auditService.log("CREATE", "user", "create", Map.of("id", 1), Map.of("id", 1), null);
        verify(auditRepository, times(1)).persist(any(AuditModel.class));

        // Case 2: UserSession has ID, User NOT found in DB
        reset(auditRepository);
        when(userRepository.findById("user-123")).thenReturn(null);
        auditService.log("UPDATE", "user", "update", null, null, "error-msg");
        verify(auditRepository, times(1)).persist(any(AuditModel.class));

        // Case 3: Empty X-Forwarded-For
        reset(auditRepository);
        when(request.getHeader("X-Forwarded-For")).thenReturn("");
        auditService.log("DELETE", "user", "delete", null, null, null);
        verify(auditRepository).persist(any(AuditModel.class));

        // Case 4: X-Forwarded-For is whitespace
        reset(auditRepository);
        when(request.getHeader("X-Forwarded-For")).thenReturn("  ");
        auditService.log("DELETE", "user", "delete", null, null, null);
        verify(auditRepository).persist(any(AuditModel.class));

        // Case 5: X-Forwarded-For is null (Covers branch return request.remoteAddress().host())
        reset(auditRepository);
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        auditService.log("DELETE", "user", "delete", null, null, null);
        verify(auditRepository).persist(any(AuditModel.class));

        // Case 6: UserSession is null or getUserId is null
        reset(auditRepository);
        when(userSession.getUserId()).thenReturn(null);
        auditService.log("DELETE", "user", "delete", null, null, null);
        verify(auditRepository).persist(any(AuditModel.class));

        // Case 7: Exception in logging (covers catch block)
        reset(auditRepository);
        doThrow(new RuntimeException("DB Fail")).when(auditRepository).persist(any(AuditModel.class));
        auditService.log("ERROR_PATH", "f", "m", null, null, null);
    }

    @Test
    @DisplayName("Should cover error logging branches")
    public void testErrorLoggingBranches() {
        when(request.uri()).thenReturn("/test-error-uri");
        
        // Case 1: User session present
        when(userSession.getUserId()).thenReturn("user-123");
        auditService.logError(new RuntimeException("Test Error"), null);
        verify(errorLogRepository).persist(any(ErrorLog.class));

        // Case 2: User session absent, explicit source, null message -> Should NOT persist
        reset(errorLogRepository);
        when(userSession.getUserId()).thenReturn(null);
        auditService.logError(new RuntimeException((String)null), "manual-source");
        verify(errorLogRepository, never()).persist(any(ErrorLog.class));

        // Case 3: Exception in error logging (covers catch block)
        reset(errorLogRepository);
        when(userSession.getUserId()).thenReturn("user-123");
        doThrow(new RuntimeException("DB Fail")).when(errorLogRepository).persist(any(ErrorLog.class));
        auditService.logError(new RuntimeException("Test 3"), null);

        // Case 4: Successful log with explicit manual source (covers source != null branch)
        reset(errorLogRepository);
        when(userSession.getUserId()).thenReturn("user-123");
        auditService.logError(new RuntimeException("Explicit Source Error"), "my-manual-source");
        org.mockito.ArgumentCaptor<ErrorLog> captor = org.mockito.ArgumentCaptor.forClass(ErrorLog.class);
        verify(errorLogRepository).persist(captor.capture());
        assertEquals("my-manual-source", captor.getValue().getSource());
    }

    @Test
    @DisplayName("Should cover audit model and error log entities")
    public void testAuditEntities() {
        AuditModel audit = new AuditModel();
        audit.setId("id-123");
        audit.setIdUser("user");
        audit.setUserName("John");
        audit.setActionType("action");
        audit.setExecuteType("exec");
        audit.setFunctionName("function");
        audit.setParams("params");
        audit.setRaw("raw");
        audit.setError("error");
        audit.setMethod("GET");
        audit.setIp("127.0.0.1");
        audit.setTableName("table");
        audit.setClassName("class");
        audit.setDiffValue("diff");
        audit.setHost("host");
        audit.setBaseUrl("base");
        audit.setHostname("hostname");
        audit.setOriginalUrl("orig");
        audit.setCreatedAt(java.time.LocalDateTime.now());
        
        assertEquals("id-123", audit.getId());
        assertEquals("user", audit.getIdUser());
        assertEquals("John", audit.getUserName());
        assertEquals("action", audit.getActionType());
        assertEquals("exec", audit.getExecuteType());
        assertEquals("function", audit.getFunctionName());
        assertEquals("params", audit.getParams());
        assertEquals("raw", audit.getRaw());
        assertEquals("error", audit.getError());
        assertEquals("GET", audit.getMethod());
        assertEquals("127.0.0.1", audit.getIp());
        assertEquals("table", audit.getTableName());
        assertEquals("class", audit.getClassName());
        assertEquals("diff", audit.getDiffValue());
        assertEquals("host", audit.getHost());
        assertEquals("base", audit.getBaseUrl());
        assertEquals("hostname", audit.getHostname());
        assertEquals("orig", audit.getOriginalUrl());
        assertNotNull(audit.getCreatedAt());

        // Test onCreate
        AuditModel audit2 = new AuditModel();
        audit2.onCreate();
        assertNotNull(audit2.getId());
        assertNotNull(audit2.getCreatedAt());

        ErrorLog error = new ErrorLog();
        error.setId("err-123");
        error.setErrorMessage("msg");
        error.setErrorData("data");
        error.setSource("/src");
        error.setIdUser("u1");
        error.setCreatedAt(java.time.LocalDateTime.now());
        
        assertEquals("err-123", error.getId());
        assertEquals("msg", error.getErrorMessage());
        assertEquals("data", error.getErrorData());
        assertEquals("/src", error.getSource());
        assertEquals("u1", error.getIdUser());
        assertNotNull(error.getCreatedAt());

        // Test onCreate combinations for AuditModel
        AuditModel a3 = new AuditModel();
        a3.setId("pre-id");
        a3.onCreate();
        assertEquals("pre-id", a3.getId());
        assertNotNull(a3.getCreatedAt());

        AuditModel a4 = new AuditModel();
        java.time.LocalDateTime preTime = java.time.LocalDateTime.now().minusDays(1);
        a4.setCreatedAt(preTime);
        a4.onCreate();
        assertNotNull(a4.getId());
        assertEquals(preTime, a4.getCreatedAt());

        // Test onCreate combinations for ErrorLog
        ErrorLog e3 = new ErrorLog();
        e3.setId("pre-err-id");
        e3.onCreate();
        assertEquals("pre-err-id", e3.getId());
        assertNotNull(e3.getCreatedAt());

        ErrorLog e4 = new ErrorLog();
        e4.setCreatedAt(preTime);
        e4.onCreate();
        assertNotNull(e4.getId());
        assertEquals(preTime, e4.getCreatedAt());
    }
}
