package com.app.infrastructure.auth;

import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.keys.KeyCommands;
import io.quarkus.redis.datasource.value.ValueCommands;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.UriInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class AuthInfrastructureUnitTest {

    private JwtService jwtService;
    private RedisDataSource redisDataSource;
    private ValueCommands<String, String> valueCommands;
    private KeyCommands<String> keyCommands;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setup() {
        redisDataSource = mock(RedisDataSource.class);
        valueCommands = mock(ValueCommands.class);
        keyCommands = mock(KeyCommands.class);

        when(redisDataSource.value(eq(String.class), eq(String.class))).thenReturn(valueCommands);
        when(redisDataSource.key(eq(String.class))).thenReturn(keyCommands);

        jwtService = new JwtService();
        jwtService.redisDataSource = redisDataSource;
        jwtService.jwtSecret = "very_long_secret_for_hmac_sha256_at_least_32_chars";
        jwtService.jwtExpiration = 3600;
        jwtService.issuer = "http://test";
    }

    @Test
    public void testJwtFlow_Exhaustive() {
        // Normal flow
        String token = jwtService.createToken("123", Map.of("email", "e", "roleId", "admin"), 3600);
        Map<String, Object> claims = jwtService.validateToken(token);
        assertEquals("123", claims.get("uid"));
        assertEquals("admin", claims.get("roleId"));

        // Blank/Null
        assertNull(jwtService.validateToken(null));
        assertNull(jwtService.validateToken(" "));

        // Invalid signature/format
        assertNull(jwtService.validateToken("invalid.token.here")); // > 10 chars
        assertNull(jwtService.validateToken("abc")); // < 10 chars
        assertNull(jwtService.validateToken("1234567890")); // == 10 chars
    }

    @Test
    public void testJwtTokenPair() {
        Map<String, String> pair = jwtService.createTokenPair("123", Map.of());
        assertNotNull(pair.get("token"));
        assertNotNull(pair.get("refreshToken"));
    }

    @Test
    void testRedisOperations() {
        jwtService.saveSessionVersion("u1", 1L);
        verify(valueCommands).setex(eq("session:user:u1"), eq(7L * 24L * 3600L), eq("1"));

        when(valueCommands.get("session:user:u1")).thenReturn("1");
        assertEquals(1L, jwtService.getSessionVersion("u1"));

        jwtService.deleteSessionVersion("u1");
        verify(keyCommands).del("session:user:u1");
    }

    @Test
    void testGetSessionVersion_NotFound() {
        when(valueCommands.get(anyString())).thenReturn(null);
        assertEquals(-1L, jwtService.getSessionVersion("unknown"));
    }

    @Test
    void testUserSession_Exhaustive() {
        UserSession session = new UserSession();

        // Null user
        session.setUser(null);
        assertNull(session.getUser());
        assertNull(session.getUserId());
        assertFalse(session.isAdmin());
        assertFalse(session.hasPermission("f", "a"));

        // Admin via roleId
        session.setUser(Map.of("roleId", "admin", "uid", "u1"));
        assertTrue(session.isAdmin());
        assertEquals("u1", session.getUserId());
        assertTrue(session.hasPermission("f", "a")); // Admin has all

        // Admin via id_role
        session.setUser(Map.of("id_role", "administrator"));
        assertTrue(session.isAdmin());

        // Normal user with permissions
        List<Map<String, Object>> perms = new ArrayList<>();
        Map<String, Object> p1 = new java.util.HashMap<>();
        p1.put("feature", "feat1");
        p1.put("view", true);
        p1.put("edit", false);
        perms.add(p1);

        session.setUser(Map.of("permissions", perms, "uid", "u2"));
        assertFalse(session.isAdmin());
        assertTrue(session.hasPermission("feat1", "view"));
        assertFalse(session.hasPermission("feat1", "edit"));
        assertFalse(session.hasPermission("feat1", "delete"));
        assertFalse(session.hasPermission("feat2", "view"));

        // Permissions edge cases
        // 1. Invalid type
        session.setUser(Map.of("permissions", "invalid-type"));
        assertFalse(session.hasPermission("f", "a"));

        // 2. Not a map in list
        session.setUser(Map.of("permissions", List.of("not-a-map")));
        assertFalse(session.hasPermission("f", "a"));

        // 3. Explicit null permissions
        Map<String, Object> nullPermissions = new java.util.HashMap<>();
        nullPermissions.put("permissions", null);
        session.setUser(nullPermissions);
        assertFalse(session.hasPermission("f", "a"));

        // 4. Missing permissions key
        session.setUser(Map.of("uid", "u3"));
        assertFalse(session.hasPermission("f", "a"));

        // 5. Null feature in permission map
        List<Map<String, Object>> pList = new ArrayList<>();
        Map<String, Object> pNull = new java.util.HashMap<>();
        pNull.put("feature", null);
        pList.add(pNull);
        session.setUser(Map.of("permissions", pList));
        assertFalse(session.hasPermission("f", "a"));
    }

    @Test
    void testAuthFilter_Exhaustive() throws Exception {
        AuthFilter filter = new AuthFilter();
        filter.resourceInfo = mock(ResourceInfo.class);
        filter.jwtService = mock(JwtService.class);
        filter.userSession = mock(UserSession.class);

        ContainerRequestContext ctx = mock(ContainerRequestContext.class);
        UriInfo uriInfo = mock(UriInfo.class);
        when(ctx.getUriInfo()).thenReturn(uriInfo);

        // 1. resourceInfo null
        filter.resourceInfo = null;
        filter.filter(ctx);
        verify(ctx, never()).abortWith(any());

        // 1b. resourceInfo present but resourceMethod null
        filter.resourceInfo = mock(ResourceInfo.class);
        when(filter.resourceInfo.getResourceMethod()).thenReturn(null);
        filter.filter(ctx);
        verify(ctx, never()).abortWith(any());

        // Restore resourceInfo and mock a method so it doesn't return early at line 45
        filter.resourceInfo = mock(ResourceInfo.class);
        Method dummyMethod = getClass().getMethod("testJwtTokenPair");
        when(filter.resourceInfo.getResourceMethod()).thenReturn(dummyMethod);
        when(filter.resourceInfo.getResourceClass()).thenReturn((Class) getClass());

        // 2. Health check bypasses
        when(uriInfo.getPath()).thenReturn("/v1/health");
        filter.filter(ctx);
        verify(ctx, never()).abortWith(any());

        when(uriInfo.getPath()).thenReturn("/v1/health/live");
        filter.filter(ctx);
        verify(ctx, never()).abortWith(any());

        when(uriInfo.getPath()).thenReturn("/v1/health/ready");
        filter.filter(ctx);
        verify(ctx, never()).abortWith(any());

        // 3. Login bypass
        when(uriInfo.getPath()).thenReturn("/v1/auth/login");
        filter.filter(ctx);
        verify(ctx, never()).abortWith(any());

        // 4. Secure path but NO annotation
        when(uriInfo.getPath()).thenReturn("/v1/public");
        Method publicMethod = getClass().getMethod("testJwtTokenPair"); // Any method without @Authenticated
        when(filter.resourceInfo.getResourceMethod()).thenReturn(publicMethod);
        when(filter.resourceInfo.getResourceClass()).thenReturn((Class) getClass());
        filter.filter(ctx);
        verify(ctx, never()).abortWith(any());

        // 5. Secure path WITH @Authenticated - Missing Header
        class Sec {
            @Authenticated
            public void m() {
            }

            @RequiresPermission(action = "v")
            public void perm() {
            }

            @Authenticated
            @RequiresPermission(action = "v")
            public void both() {
            }
        }
        when(filter.resourceInfo.getResourceMethod()).thenReturn(Sec.class.getMethod("m"));
        when(filter.resourceInfo.getResourceClass()).thenReturn((Class) Sec.class);
        when(uriInfo.getPath()).thenReturn("/v1/secure");
        when(ctx.getHeaderString("Authorization")).thenReturn(null);
        filter.filter(ctx);
        verify(ctx).abortWith(argThat(r -> r.getStatus() == 401));

        // 6. Invalid Header Format
        reset(ctx);
        when(ctx.getUriInfo()).thenReturn(uriInfo);
        when(ctx.getHeaderString("Authorization")).thenReturn("Wrong token");
        filter.filter(ctx);
        verify(ctx).abortWith(any());

        // 7. JWT Validation Fails
        reset(ctx);
        when(ctx.getUriInfo()).thenReturn(uriInfo);
        when(ctx.getHeaderString("Authorization")).thenReturn("Bearer invalid");
        when(filter.jwtService.validateToken("invalid")).thenReturn(null);
        filter.filter(ctx);
        verify(ctx).abortWith(argThat(r -> r.getStatus() == 401));

        // 8. Session Revoked (Not in Redis)
        reset(ctx);
        when(ctx.getUriInfo()).thenReturn(uriInfo);
        when(ctx.getHeaderString("Authorization")).thenReturn("Bearer t1");
        when(filter.jwtService.validateToken("t1")).thenReturn(Map.of("uid", "u1", "sv", 1));
        when(filter.jwtService.getSessionVersion("u1")).thenReturn(-1L);
        filter.filter(ctx);
        verify(ctx).abortWith(argThat(r -> r.getStatus() == 401));

        // 9. Success (using 'sub' claim)
        reset(ctx);
        when(ctx.getUriInfo()).thenReturn(uriInfo);
        when(ctx.getHeaderString("Authorization")).thenReturn("Bearer t2");
        when(filter.jwtService.validateToken("t2")).thenReturn(Map.of("sub", "u2", "sv", 1));
        when(filter.jwtService.getSessionVersion("u2")).thenReturn(1L);
        filter.filter(ctx);
        verify(filter.userSession).setUser(anyMap());
        verify(ctx, never()).abortWith(any());

        // 10. Claims present but NO uid or sub
        reset(ctx);
        when(ctx.getUriInfo()).thenReturn(uriInfo);
        when(ctx.getHeaderString("Authorization")).thenReturn("Bearer t3");
        when(filter.jwtService.validateToken("t3")).thenReturn(Map.of("other", "val"));
        filter.filter(ctx);
        verify(ctx).abortWith(argThat(r -> r.getStatus() == 401));

        // 11. UriInfo is null (edge case for line 40 and 51-52)
        reset(ctx);
        when(ctx.getUriInfo()).thenReturn(null);
        // Use a method that DOES NOT require auth to reach line 51
        publicMethod = getClass().getMethod("testJwtTokenPair");
        when(filter.resourceInfo.getResourceMethod()).thenReturn(publicMethod);
        when(filter.resourceInfo.getResourceClass()).thenReturn((Class) getClass());
        filter.filter(ctx);
        verify(ctx, never()).abortWith(any());
        // 12. getPath() is null (edge case for refactored path extraction)
        reset(ctx);
        when(ctx.getUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getPath()).thenReturn(null);
        when(filter.resourceInfo.getResourceMethod()).thenReturn(publicMethod);
        when(filter.resourceInfo.getResourceClass()).thenReturn((Class) getClass());
        filter.filter(ctx);
        verify(ctx, never()).abortWith(any());

        // 13. Secure path WITH @RequiresPermission only
        reset(ctx);
        when(ctx.getUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getPath()).thenReturn("/v1/secure");
        when(filter.resourceInfo.getResourceMethod()).thenReturn(Sec.class.getMethod("perm"));
        when(filter.resourceInfo.getResourceClass()).thenReturn((Class) Sec.class);
        filter.filter(ctx);
        verify(ctx).abortWith(argThat(r -> r.getStatus() == 401));

        // 14. Secure path WITH BOTH annotations
        reset(ctx);
        when(ctx.getUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getPath()).thenReturn("/v1/secure");
        when(filter.resourceInfo.getResourceMethod()).thenReturn(Sec.class.getMethod("both"));
        when(filter.resourceInfo.getResourceClass()).thenReturn((Class) Sec.class);
        filter.filter(ctx);
        verify(ctx).abortWith(argThat(r -> r.getStatus() == 401));
    }

    @Test
    void testPermissionFilter_Exhaustive() {
        UserSession session = mock(UserSession.class);
        com.app.modules.audit.AuditService audit = mock(com.app.modules.audit.AuditService.class);

        // Use anonymous subclass to override Arc lookups
        PermissionFilter filter = new PermissionFilter("feat", "act") {
            @Override
            protected UserSession getUserSession() {
                return session;
            }

            @Override
            protected com.app.modules.audit.AuditService getAuditService() {
                return audit;
            }
        };

        ContainerRequestContext ctx = mock(ContainerRequestContext.class);

        // 1. Auth already aborted
        when(ctx.getProperty("auth_aborted")).thenReturn(true);
        filter.filter(ctx);
        verify(ctx, never()).abortWith(any());

        // 2. No user session
        when(ctx.getProperty("auth_aborted")).thenReturn(false);
        when(session.getUser()).thenReturn(null);
        filter.filter(ctx);
        verify(ctx).abortWith(argThat(r -> r.getStatus() == 401));

        // 3. Denied
        reset(ctx);
        when(session.getUser()).thenReturn(Map.of("uid", "u1"));
        when(session.hasPermission("feat", "act")).thenReturn(false);
        filter.filter(ctx);
        verify(ctx).abortWith(argThat(r -> r.getStatus() == 403));
        verify(audit).log(anyString(), anyString(), anyString(), any(), any(), anyString());

        // 4. Allowed
        reset(ctx);
        when(session.hasPermission("feat", "act")).thenReturn(true);
        filter.filter(ctx);
        verify(ctx, never()).abortWith(any());
    }

    @Test
    public void testSecurityDynamicFeature_Exhaustive() throws Exception {
        SecurityDynamicFeature feature = new SecurityDynamicFeature();
        ResourceInfo info = mock(ResourceInfo.class);
        jakarta.ws.rs.core.FeatureContext ctx = mock(jakarta.ws.rs.core.FeatureContext.class);

        // 1. Method with feature explicitly set
        @ResourceFeature("res")
        class Target1 {
            @RequiresPermission(feature = "f1", action = "v")
            public void m() {
            }
        }
        when(info.getResourceMethod()).thenReturn(Target1.class.getMethod("m"));
        when(info.getResourceClass()).thenReturn((Class) Target1.class);
        feature.configure(info, ctx);
        verify(ctx).register(any(PermissionFilter.class), anyInt());

        // 2. Method with empty feature, fallback to Class @ResourceFeature
        reset(ctx);
        class Target2 {
            @RequiresPermission(action = "v")
            public void m() {
            }
        }
        @ResourceFeature("class-feat")
        class Target3 extends Target2 {
        }
        when(info.getResourceMethod()).thenReturn(Target3.class.getMethod("m"));
        when(info.getResourceClass()).thenReturn((Class) Target3.class);
        feature.configure(info, ctx);
        verify(ctx).register(any(PermissionFilter.class), anyInt());

        // 3. Method with empty feature, NO class annotation (should NOT register)
        reset(ctx);
        class Target4 {
            @RequiresPermission(action = "v")
            public void m() {
            }
        }
        when(info.getResourceMethod()).thenReturn(Target4.class.getMethod("m"));
        when(info.getResourceClass()).thenReturn((Class) Target4.class);
        feature.configure(info, ctx);
        verify(ctx, never()).register(any(), anyInt());

        // 4. Method is null
        reset(ctx);
        when(info.getResourceMethod()).thenReturn(null);
        feature.configure(info, ctx);
        verify(ctx, never()).register(any(), anyInt());
    }

    @Test
    public void testSecurityDynamicFeature_Hierarchy() throws Exception {
        SecurityDynamicFeature feature = new SecurityDynamicFeature();
        ResourceInfo info = mock(ResourceInfo.class);
        jakarta.ws.rs.core.FeatureContext ctx = mock(jakarta.ws.rs.core.FeatureContext.class);

        // 1. Deep Hierarchy: GrandParent has the annotation
        class GrandParent {
            @RequiresPermission(action = "gp")
            public void m1() {
            }
        }
        class Parent extends GrandParent {
            @Override
            public void m1() {
            }
        }
        @ResourceFeature("child")
        class Child extends Parent {
            @Override
            public void m1() {
            }
        }

        when(info.getResourceMethod()).thenReturn(Child.class.getMethod("m1"));
        when(info.getResourceClass()).thenReturn((Class) Child.class);
        feature.configure(info, ctx);
        verify(ctx).register(any(PermissionFilter.class), anyInt());

        // 2. Interface Hierarchy
        reset(ctx);
        interface RootInterface {
            @RequiresPermission(action = "iface")
            void ifaceMethod();
        }
        class Implementation implements RootInterface {
            @Override
            public void ifaceMethod() {
            }
        }
        @ResourceFeature("iface-res")
        class SubImplementation extends Implementation {
        }

        when(info.getResourceMethod()).thenReturn(SubImplementation.class.getMethod("ifaceMethod"));
        when(info.getResourceClass()).thenReturn((Class) SubImplementation.class);
        feature.configure(info, ctx);
        verify(ctx).register(any(PermissionFilter.class), anyInt());

        // 3. No permission anywhere (exhausting the hierarchy)
        reset(ctx);
        class NoPerm {
            public void m() {
            }
        }
        when(info.getResourceMethod()).thenReturn(NoPerm.class.getMethod("m"));
        when(info.getResourceClass()).thenReturn((Class) NoPerm.class);
        feature.configure(info, ctx);
        verify(ctx, never()).register(any(), anyInt());

        // 4. Method is null (explicit call to findRequiresPermission coverage)
        reset(ctx);
        when(info.getResourceMethod()).thenReturn(null);
        feature.configure(info, ctx);
        verify(ctx, never()).register(any(), anyInt());

        // 5. Explicit feature in annotation (skips ResourceFeature fallback)
        reset(ctx);
        class ExplicitFeat {
            @RequiresPermission(feature = "explicit", action = "v")
            public void m() {
            }
        }
        when(info.getResourceMethod()).thenReturn(ExplicitFeat.class.getMethod("m"));
        when(info.getResourceClass()).thenReturn((Class) ExplicitFeat.class);
        feature.configure(info, ctx);
        verify(ctx).register(any(PermissionFilter.class), anyInt());

        // 6. Empty feature and NO ResourceFeature on class
        reset(ctx);
        class NoClassFeat {
            @RequiresPermission(action = "v")
            public void m() {
            }
        }
        when(info.getResourceMethod()).thenReturn(NoClassFeat.class.getMethod("m"));
        when(info.getResourceClass()).thenReturn((Class) NoClassFeat.class);
        feature.configure(info, ctx);
        verify(ctx, never()).register(any(), anyInt());

        // 7. Object.class method (superclass is null)
        reset(ctx);
        when(info.getResourceMethod()).thenReturn(Object.class.getMethod("toString"));
        when(info.getResourceClass()).thenReturn((Class) Object.class);
        feature.configure(info, ctx);
        verify(ctx, never()).register(any(), anyInt());
    }

    @Test
    public void testSecurityDynamicFeature_FindInClass_BranchCoverage() throws Exception {
        SecurityDynamicFeature feature = new SecurityDynamicFeature();
        ResourceInfo info = mock(ResourceInfo.class);
        jakarta.ws.rs.core.FeatureContext ctx = mock(jakarta.ws.rs.core.FeatureContext.class);

        // Parent class containing methods to exercise all branch logic in findInClass:
        // 1. Method with different name (m.getName().equals false)
        // 2. Method with same name but different parameter count (m.getParameterCount() == params.length false)
        // 3. Method with same name, same parameter count, but incompatible parameter type (isAssignableFrom false, match = false)
        // 4. Method that is assignable but does not have the RequiresPermission annotation (isAnnotationPresent false)
        // 5. Method that matches all criteria and has the RequiresPermission annotation
        class BranchParent {
            public void otherName() {}
            
            public void m1() {}
            
            public void m1(Integer i) {}
            
            public void m1(String s) {}
            
            @RequiresPermission(action = "test")
            public void m2(String s) {}
        }
        
        @ResourceFeature("test")
        class BranchChild extends BranchParent {
            @Override
            public void m1(String s) {}
            
            @Override
            public void m2(String s) {}
        }

        // Test with m1 (no annotation found because m1 in parent lacks it)
        when(info.getResourceMethod()).thenReturn(BranchChild.class.getMethod("m1", String.class));
        when(info.getResourceClass()).thenReturn((Class) BranchChild.class);
        feature.configure(info, ctx);
        verify(ctx, never()).register(any(), anyInt());

        // Test with m2 (annotation found on parent!)
        reset(ctx);
        when(info.getResourceMethod()).thenReturn(BranchChild.class.getMethod("m2", String.class));
        when(info.getResourceClass()).thenReturn((Class) BranchChild.class);
        feature.configure(info, ctx);
        verify(ctx).register(any(PermissionFilter.class), anyInt());
    }
}
