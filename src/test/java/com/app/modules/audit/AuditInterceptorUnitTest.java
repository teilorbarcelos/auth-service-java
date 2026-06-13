package com.app.modules.audit;

import jakarta.interceptor.InvocationContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class AuditInterceptorUnitTest {

    private AuditInterceptor interceptor;
    private AuditService auditService;

    @BeforeEach
    void setup() {
        interceptor = new AuditInterceptor();
        auditService = mock(AuditService.class);
        interceptor.auditService = auditService;
    }

    @Test
    void testAudit_ShouldAudit() throws Exception {
        InvocationContext context = mock(InvocationContext.class);
        Method method = MockTarget.class.getMethod("createSomething");
        when(context.getMethod()).thenReturn(method);
        when(context.getTarget()).thenReturn(new MockTarget());
        when(context.proceed()).thenReturn("result");

        Object result = interceptor.audit(context);

        assertEquals("result", result);
        verify(auditService).log(eq("CREATESOMETHING"), eq("mock"), eq("createSomething"), any(), eq("result"), isNull());
    }

    @Test
    void testAudit_ShouldNotAudit() throws Exception {
        InvocationContext context = mock(InvocationContext.class);
        Method method = MockTarget.class.getMethod("getSomething");
        when(context.getMethod()).thenReturn(method);
        when(context.proceed()).thenReturn("result");

        Object result = interceptor.audit(context);

        assertEquals("result", result);
        verifyNoInteractions(auditService);
    }

    @Test
    void testAudit_WithException() throws Exception {
        InvocationContext context = mock(InvocationContext.class);
        Method method = MockTarget.class.getMethod("deleteSomething");
        when(context.getMethod()).thenReturn(method);
        when(context.getTarget()).thenReturn(new MockTarget());
        when(context.proceed()).thenThrow(new RuntimeException("Fail"));

        assertThrows(RuntimeException.class, () -> interceptor.audit(context));
        verify(auditService).log(eq("DELETESOMETHING"), eq("mock"), eq("deleteSomething"), any(), isNull(), eq("Fail"));
    }

    @Test
    void testAudit_VariousMethods() throws Exception {
        String[] methods = {"createSomething", "updateSomething", "deleteSomething", "toggleStatus", "setStatus"};
        for (String mName : methods) {
            InvocationContext context = mock(InvocationContext.class);
            Method method = MockTarget.class.getMethod(mName);
            when(context.getMethod()).thenReturn(method);
            when(context.getTarget()).thenReturn(new MockTarget());
            when(context.proceed()).thenReturn("ok");

            interceptor.audit(context);
            verify(auditService).log(eq(mName.toUpperCase()), eq("mock"), eq(mName), any(), eq("ok"), isNull());
            reset(auditService);
        }
    }

    @Test
    void testAudit_NoFeatureInHierarchy() throws Exception {
        InvocationContext context = mock(InvocationContext.class);
        Method method = NoAnnotationTarget.class.getMethod("createSomething");
        
        when(context.getMethod()).thenReturn(method);
        when(context.getTarget()).thenReturn(new NoAnnotationTarget());
        when(context.proceed()).thenReturn("ok");

        interceptor.audit(context);
        verify(auditService).log(any(), eq("unknown"), any(), any(), any(), any());
    }

    @Test
    void testAudit_DeepHierarchy() throws Exception {
        InvocationContext context = mock(InvocationContext.class);
        Method method = MockDeepTarget.class.getMethod("createSomething");
        when(context.getMethod()).thenReturn(method);
        when(context.getTarget()).thenReturn(new MockDeepTarget());
        when(context.proceed()).thenReturn("ok");

        interceptor.audit(context);
        verify(auditService).log(any(), eq("mock"), any(), any(), any(), any());
    }

    public static class MockTarget extends MockParent {
        public String createSomething() { return "ok"; }
        public String updateSomething() { return "ok"; }
        public String getSomething() { return "ok"; }
        public String deleteSomething() { return "ok"; }
        public String toggleStatus() { return "ok"; }
        public String setStatus() { return "ok"; }
    }

    @com.app.infrastructure.auth.ResourceFeature("mock")
    public static class MockParent {}

    public static class MockDeepTarget extends MockTarget {}

    public static class NoAnnotationTarget {
        public String createSomething() { return "ok"; }
    }
}
