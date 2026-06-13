package com.app.modules.auth;

import com.app.core.exception.BadRequestException;
import com.app.infrastructure.auth.JwtService;
import com.app.infrastructure.email.EmailProvider;
import com.app.modules.auth.dto.AuthResponseDTO;
import com.app.modules.user.UserModel;
import com.app.modules.user.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.ws.rs.WebApplicationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mindrot.jbcrypt.BCrypt;

import jakarta.ws.rs.core.Response;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class AuthServiceUnitTest {

    private AuthService authService;
    private JwtService jwtService;
    private UserRepository userRepository;
    private EmailProvider emailProvider;
    private EntityManager em;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        jwtService = mock(JwtService.class);
        userRepository = mock(UserRepository.class);
        emailProvider = mock(EmailProvider.class);
        em = mock(EntityManager.class);
        objectMapper = new ObjectMapper();

        authService = new AuthService();
        authService.jwtService = jwtService;
        authService.userRepository = userRepository;
        authService.emailProvider = emailProvider;
        authService.em = em;
        authService.objectMapper = objectMapper;
    }

    @Test
    void testLogin_Success() {
        UserModel user = new UserModel();
        user.setId("123");
        user.setEmail("test@test.com");
        user.setIdRole("admin");
        user.setActive(true);
        com.app.modules.role.RoleModel role = new com.app.modules.role.RoleModel();
        role.setName("Admin Role");
        role.setDescription("Desc");
        user.setRole(role);

        AuthModel auth = new AuthModel();
        auth.setPassword(BCrypt.hashpw("password", BCrypt.gensalt()));

        when(userRepository.findByEmail("test@test.com")).thenReturn(user);
        when(em.find(AuthModel.class, "123")).thenReturn(auth);
        when(jwtService.createTokenPair(anyString(), any())).thenReturn(Map.of("token", "t", "refreshToken", "r"));
        
        jakarta.persistence.TypedQuery query = mock(jakarta.persistence.TypedQuery.class);
        when(em.createQuery(anyString(), any())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        
        com.app.modules.role.RoleFeatureModel rf = new com.app.modules.role.RoleFeatureModel();
        rf.setIdFeature("f1");
        rf.setPermissions("{\"view\": true, \"create\": true}");
        when(query.getResultList()).thenReturn(java.util.List.of(rf));

        AuthResponseDTO response = authService.login("test@test.com", "password");

        assertNotNull(response);
        assertEquals("t", response.getToken());
        assertEquals("Desc", response.getUser().getRole().getDescription());
    }

    @Test
    void testLogin_Failures() {
        // Blank/Null
        assertThrows(BadRequestException.class, () -> authService.login(null, "pass"));
        assertThrows(BadRequestException.class, () -> authService.login(" ", "pass"));
        assertThrows(BadRequestException.class, () -> authService.login("e", null));
        assertThrows(BadRequestException.class, () -> authService.login("e", " "));
        
        // Credentials
        when(userRepository.findByEmail(anyString())).thenReturn(null);
        assertThrows(WebApplicationException.class, () -> authService.login("w", "p"));
        
        UserModel user = new UserModel();
        user.setId("1");
        when(userRepository.findByEmail("e")).thenReturn(user);
        when(em.find(AuthModel.class, "1")).thenReturn(null);
        assertThrows(WebApplicationException.class, () -> authService.login("e", "p"));
        
        AuthModel auth = new AuthModel();
        auth.setPassword(BCrypt.hashpw("p", BCrypt.gensalt()));
        when(em.find(AuthModel.class, "1")).thenReturn(auth);
        assertThrows(WebApplicationException.class, () -> authService.login("e", "wrong"));
        
        // Disabled
        user.setActive(false);
        assertThrows(WebApplicationException.class, () -> authService.login("e", "p"));
    }

    @Test
    void testLogin_RoleDisabled() {
        UserModel user = new UserModel();
        user.setId("1");
        user.setEmail("e");
        user.setActive(true);
        
        com.app.modules.role.RoleModel role = new com.app.modules.role.RoleModel();
        role.setActive(false);
        user.setRole(role);

        AuthModel auth = new AuthModel();
        auth.setPassword(BCrypt.hashpw("p", BCrypt.gensalt()));

        when(userRepository.findByEmail("e")).thenReturn(user);
        when(em.find(AuthModel.class, "1")).thenReturn(auth);

        WebApplicationException ex = assertThrows(WebApplicationException.class, () -> authService.login("e", "p"));
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), ex.getResponse().getStatus());
        assertEquals("Role is disabled", ex.getMessage());
    }

    @Test
    void testLogin_RoleNull() {
        UserModel user = new UserModel();
        user.setId("1");
        user.setEmail("e");
        user.setActive(true);
        user.setRole(null); 

        AuthModel auth = new AuthModel();
        auth.setPassword(BCrypt.hashpw("p", BCrypt.gensalt()));

        when(userRepository.findByEmail("e")).thenReturn(user);
        when(em.find(AuthModel.class, "1")).thenReturn(auth);
        when(jwtService.createTokenPair(anyString(), any())).thenReturn(Map.of("token", "t", "refreshToken", "r"));
        
        jakarta.persistence.TypedQuery query = mock(jakarta.persistence.TypedQuery.class);
        when(em.createQuery(anyString(), any())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.getResultList()).thenReturn(java.util.Collections.emptyList());

        assertNotNull(authService.login("e", "p"));
    }

    @Test
    void testLogin_RoleEnabled() {
        UserModel user = new UserModel();
        user.setId("1");
        user.setEmail("e");
        user.setActive(true);
        
        com.app.modules.role.RoleModel role = new com.app.modules.role.RoleModel();
        role.setActive(true);
        user.setRole(role);

        AuthModel auth = new AuthModel();
        auth.setPassword(BCrypt.hashpw("p", BCrypt.gensalt()));

        when(userRepository.findByEmail("e")).thenReturn(user);
        when(em.find(AuthModel.class, "1")).thenReturn(auth);
        when(jwtService.createTokenPair(anyString(), any())).thenReturn(Map.of("token", "t", "refreshToken", "r"));
        
        jakarta.persistence.TypedQuery query = mock(jakarta.persistence.TypedQuery.class);
        when(em.createQuery(anyString(), any())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.getResultList()).thenReturn(java.util.Collections.emptyList());

        assertNotNull(authService.login("e", "p"));
    }

    @Test
    void testGetMe_Success() {
        UserModel user = new UserModel();
        user.setId("1");
        user.setIdRole("r");
        when(userRepository.findById("1")).thenReturn(user);
        when(jwtService.createTokenPair(anyString(), any())).thenReturn(Map.of("token", "t", "refreshToken", "r"));
        
        jakarta.persistence.TypedQuery query = mock(jakarta.persistence.TypedQuery.class);
        when(em.createQuery(anyString(), any())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.getResultList()).thenReturn(java.util.Collections.emptyList());

        AuthResponseDTO response = authService.getMe("1");
        assertNotNull(response);
        assertEquals("User", response.getUser().getRole().getName());
    }

    @Test
    void testGetMe_NullRoleFallback() {
        UserModel user = new UserModel();
        user.setId("1");
        user.setIdRole(null);
        when(userRepository.findById("1")).thenReturn(user);
        when(jwtService.createTokenPair(anyString(), any())).thenReturn(Map.of("token", "t", "refreshToken", "r"));
        
        jakarta.persistence.TypedQuery query = mock(jakarta.persistence.TypedQuery.class);
        when(em.createQuery(anyString(), any())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.getResultList()).thenReturn(java.util.Collections.emptyList());

        AuthResponseDTO response = authService.getMe("1");
        assertEquals("user", response.getUser().getRole().getId());
    }

    @Test
    void testGetMe_RoleNullDescription() {
        UserModel user = new UserModel();
        user.setId("1");
        user.setIdRole("r1");
        
        com.app.modules.role.RoleModel role = new com.app.modules.role.RoleModel();
        role.setName("Role with null desc");
        role.setDescription(null);
        user.setRole(role);

        when(userRepository.findById("1")).thenReturn(user);
        when(jwtService.createTokenPair(anyString(), any())).thenReturn(Map.of("token", "t", "refreshToken", "r"));
        
        jakarta.persistence.TypedQuery query = mock(jakarta.persistence.TypedQuery.class);
        when(em.createQuery(anyString(), any())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.getResultList()).thenReturn(java.util.Collections.emptyList());

        AuthResponseDTO response = authService.getMe("1");
        assertEquals("", response.getUser().getRole().getDescription());
    }

    @Test
    void testGetMe_NotFound() {
        when(userRepository.findById("none")).thenReturn(null);
        assertThrows(WebApplicationException.class, () -> authService.getMe("none"));
    }

    @Test
    void testRefreshToken_Success() {
        when(jwtService.validateToken("r")).thenReturn(Map.of("uid", "1", "sv", 1));
        when(jwtService.getSessionVersion("1")).thenReturn(1L);
        UserModel user = new UserModel();
        user.setId("1");
        user.setIdRole("r1");
        when(userRepository.findById("1")).thenReturn(user);
        when(jwtService.createTokenPair(anyString(), any())).thenReturn(Map.of("token", "t", "refreshToken", "r"));
        
        jakarta.persistence.TypedQuery query = mock(jakarta.persistence.TypedQuery.class);
        when(em.createQuery(anyString(), any())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.getResultList()).thenReturn(java.util.Collections.emptyList());

        assertNotNull(authService.refreshToken("r"));
    }

    @Test
    void testRefreshToken_Failures() {
        assertThrows(BadRequestException.class, () -> authService.refreshToken(null));
        assertThrows(BadRequestException.class, () -> authService.refreshToken(" "));
        when(jwtService.validateToken("inv")).thenReturn(null);
        assertThrows(WebApplicationException.class, () -> authService.refreshToken("inv"));
        when(jwtService.validateToken("norid")).thenReturn(Map.of("a", "b"));
        assertThrows(WebApplicationException.class, () -> authService.refreshToken("norid"));
        when(jwtService.validateToken("r")).thenReturn(Map.of("uid", "123", "sv", 1));
        when(jwtService.getSessionVersion("123")).thenReturn(2L);
        assertThrows(WebApplicationException.class, () -> authService.refreshToken("r"));
    }

    @Test
    void testRequestPasswordReset_Workflow() {
        when(userRepository.findByEmail("none")).thenReturn(null);
        authService.requestPasswordReset("none");
        
        UserModel user = new UserModel();
        user.setId("1");
        user.setName("John");
        user.setEmail("e");
        when(userRepository.findByEmail("e")).thenReturn(user);
        when(em.find(AuthModel.class, "1")).thenReturn(null);
        authService.requestPasswordReset("e");
        
        AuthModel auth = new AuthModel();
        when(em.find(AuthModel.class, "1")).thenReturn(auth);
        authService.requestPasswordReset("e");
        assertNotNull(auth.getRequestPasswordToken());
        verify(emailProvider).sendEmail(eq("e"), anyString(), anyString());
    }

    @Test
    void testResetPassword_Workflow() {
        UserModel user = new UserModel();
        user.setId("1");
        AuthModel auth = new AuthModel();
        auth.setRequestPasswordToken("123");
        auth.setRequestPasswordExpiration(java.time.LocalDateTime.now().plusHours(1));
        when(userRepository.findByEmail("e")).thenReturn(user);
        when(em.find(AuthModel.class, "1")).thenReturn(auth);

        // Validation
        assertTrue(authService.validateResetToken("e", "123"));
        assertThrows(WebApplicationException.class, () -> authService.validateResetToken("e", "wrong"));
        
        auth.setRequestPasswordExpiration(java.time.LocalDateTime.now().minusHours(1));
        assertThrows(WebApplicationException.class, () -> authService.validateResetToken("e", "123"));
        
        auth.setRequestPasswordExpiration(null);
        assertTrue(authService.validateResetToken("e", "123"));

        // Execution
        auth.setRequestPasswordExpiration(java.time.LocalDateTime.now().plusHours(1));
        authService.resetPassword("e", "123", "new");
        assertNull(auth.getRequestPasswordToken());
        assertEquals(0, auth.getRetries());
    }

    @Test
    void testResetPassword_Failures() {
        when(userRepository.findByEmail("none")).thenReturn(null);
        assertThrows(WebApplicationException.class, () -> authService.validateResetToken("none", "t"));
        
        UserModel user = new UserModel();
        user.setId("1");
        when(userRepository.findByEmail("e")).thenReturn(user);
        when(em.find(AuthModel.class, "1")).thenReturn(null);
        assertThrows(WebApplicationException.class, () -> authService.validateResetToken("e", "t"));
        
        AuthModel auth = new AuthModel();
        auth.setRequestPasswordToken("123");
        when(em.find(AuthModel.class, "1")).thenReturn(auth);
        // Test null token input
        assertThrows(WebApplicationException.class, () -> authService.validateResetToken("e", null));
    }

    @Test
    void testResetPassword_ReFetchFailures() {
        UserModel user = new UserModel();
        user.setId("1");
        AuthModel auth = new AuthModel();
        auth.setRequestPasswordToken("123");
        auth.setRequestPasswordExpiration(java.time.LocalDateTime.now().plusHours(1));

        // Case 1: user becomes null after validation
        when(userRepository.findByEmail("e")).thenReturn(user, (UserModel) null);
        when(em.find(AuthModel.class, "1")).thenReturn(auth);
        assertThrows(WebApplicationException.class, () -> authService.resetPassword("e", "123", "new"));

        // Case 2: auth becomes null after validation
        reset(userRepository);
        reset(em);
        when(userRepository.findByEmail("e")).thenReturn(user, user);
        when(em.find(AuthModel.class, "1")).thenReturn(auth, (AuthModel) null);
        assertThrows(WebApplicationException.class, () -> authService.resetPassword("e", "123", "new"));
    }

    @Test
    void testFormattedPermissions_EdgeCases() {
        UserModel user = new UserModel();
        user.setIdRole("r1");
        jakarta.persistence.TypedQuery query = mock(jakarta.persistence.TypedQuery.class);
        when(em.createQuery(anyString(), any())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        
        com.app.modules.role.RoleFeatureModel rf1 = new com.app.modules.role.RoleFeatureModel();
        rf1.setIdFeature("f1");
        rf1.setPermissions("{\"view\": true, \"create\": false, \"delete\": null}");
        
        com.app.modules.role.RoleFeatureModel rf2 = new com.app.modules.role.RoleFeatureModel();
        rf2.setIdFeature("f2");
        rf2.setPermissions(null);
        
        com.app.modules.role.RoleFeatureModel rf3 = new com.app.modules.role.RoleFeatureModel();
        rf3.setIdFeature("f3");
        rf3.setPermissions("invalid");

        com.app.modules.role.RoleFeatureModel rf4 = new com.app.modules.role.RoleFeatureModel();
        rf4.setIdFeature("f4");
        rf4.setPermissions("null"); // ObjectMapper might return null
        
        when(query.getResultList()).thenReturn(List.of(rf1, rf2, rf3, rf4));
        
        user.setId("1");
        when(userRepository.findById("1")).thenReturn(user);
        when(jwtService.createTokenPair(anyString(), any())).thenReturn(Map.of("token", "t", "refreshToken", "r"));

        AuthResponseDTO resp = authService.getMe("1");
        assertEquals(4, resp.getUser().getRole().getPermissions().size());
        
        // Verify rf4 (null perms from JSON "null") results in false for all bits
        Map<String, Object> p4 = resp.getUser().getRole().getPermissions().get(3);
        assertEquals(false, p4.get("view"));
    }

    @Test
    void testAuthModel_Full() {
        AuthModel auth = new AuthModel();
        auth.setId("1");
        auth.setPassword("p");
        auth.setRetries(5);
        auth.setFirstAccess(false);
        auth.setActive(false);
        auth.setRequestPasswordToken("t");
        auth.setRequestPasswordExpiration(null);
        auth.setCreatedAt(null);
        auth.setUpdatedAt(null);
        
        assertEquals("1", auth.getId());
        assertEquals("p", auth.getPassword());
        assertEquals(5, auth.getRetries());
        assertFalse(auth.getFirstAccess());
        assertFalse(auth.getActive());
        assertEquals("t", auth.getRequestPasswordToken());
        assertNull(auth.getRequestPasswordExpiration());
        assertNull(auth.getCreatedAt());
        assertNull(auth.getUpdatedAt());

        auth.onCreate();
        auth.onUpdate();
        assertNotNull(auth.getCreatedAt());
        assertNotNull(auth.getUpdatedAt());
    }

    @Test
    void testAuthResponseDTO_Full() {
        AuthResponseDTO dto = new AuthResponseDTO();
        dto.setMessage("m");
        dto.setValid(true);
        dto.setToken("t");
        dto.setRefreshToken("r");
        
        AuthResponseDTO.UserData user = new AuthResponseDTO.UserData();
        user.setId("u1");
        user.setName("n");
        user.setEmail("e");
        
        AuthResponseDTO.RoleData role = new AuthResponseDTO.RoleData();
        role.setId("r1");
        role.setName("rn");
        role.setDescription("rd");
        role.setPermissions(new ArrayList<>());
        
        user.setRole(role);
        dto.setUser(user);
        
        assertEquals("m", dto.getMessage());
        assertTrue(dto.isValid());
        assertEquals("t", dto.getToken());
        assertEquals("r", dto.getRefreshToken());
        assertEquals("u1", dto.getUser().getId());
        assertEquals("n", dto.getUser().getName());
        assertEquals("e", dto.getUser().getEmail());
        assertEquals("r1", dto.getUser().getRole().getId());
        assertEquals("rn", dto.getUser().getRole().getName());
        assertEquals("rd", dto.getUser().getRole().getDescription());
        assertNotNull(dto.getUser().getRole().getPermissions());
    }

    @Test
    void testRefreshToken_InvalidSvType() {
        when(jwtService.validateToken("r")).thenReturn(Map.of("uid", "1", "sv", "not-a-number"));
        assertThrows(WebApplicationException.class, () -> authService.refreshToken("r"));
    }

    @Test
    void testAuthResource_Sanity() {
        AuthResource resource = new AuthResource();
        resource.authService = authService;
        assertNotNull(resource);
    }
}
