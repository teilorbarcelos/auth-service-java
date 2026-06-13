package com.app.modules.user;

import com.app.infrastructure.auth.JwtService;
import com.app.modules.auth.AuthModel;
import com.app.infrastructure.pdf.PdfProvider;
import com.app.infrastructure.pdf.PdfRequestDTO;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class UserServiceUnitTest {

    private UserService userService;
    private UserRepository userRepository;
    private EntityManager em;
    private JwtService jwtService;
    private PdfProvider pdfProvider;

    @BeforeEach
    void setup() {
        userRepository = mock(UserRepository.class);
        em = mock(EntityManager.class);
        jwtService = mock(JwtService.class);
        pdfProvider = mock(PdfProvider.class);
        userService = new UserService(userRepository, em);
        userService.jwtService = jwtService;
        userService.pdfProvider = pdfProvider;
    }

    @Test
    void testCreateUser() {
        UserModel user = new UserModel();
        user.setEmail("test@test.com");
        user.setPassword("pass123");

        UserModel savedUser = new UserModel();
        savedUser.setId("uuid-123");
        savedUser.setEmail("test@test.com");

        userService.create(user);

        verify(userRepository).persist(user);
        verify(em).persist(any(AuthModel.class));
    }

    @Test
    void testCreateUser_DefaultPassword() {
        UserModel user = new UserModel();
        user.setEmail("test@test.com");
        user.setPassword(null);

        userService.create(user);

        verify(em).persist(argThat(auth -> {
            AuthModel a = (AuthModel) auth;
            return a.getActive();
        }));
    }

    @Test
    void testUpdate_AllFields() {
        UserModel existing = new UserModel();
        existing.setId("123");
        existing.setEmail("old@test.com");
        existing.setIdRole("old-role");
        existing.setActive(true);

        when(userRepository.findById("123")).thenReturn(existing);

        UserModel incoming = new UserModel();
        incoming.setName("New Name");
        incoming.setEmail("new@test.com");
        incoming.setIdRole("new-role");
        incoming.setActive(false);
        incoming.setPassword("new-pass");

        when(em.find(AuthModel.class, "123")).thenReturn(new AuthModel());

        userService.update("123", incoming);

        assertEquals("New Name", existing.getName());
        assertEquals("new@test.com", existing.getEmail());
        assertEquals("new-role", existing.getIdRole());
        assertFalse(existing.getActive());
        verify(jwtService).deleteSessionVersion("123");
    }

    @Test
    void testUpdateUser_WithPassword_NewAuth() {
        UserModel existing = new UserModel();
        existing.setId("123");
        when(userRepository.findById("123")).thenReturn(existing);
        when(em.find(AuthModel.class, "123")).thenReturn(null);

        UserModel incoming = new UserModel();
        incoming.setPassword("new-pass");

        userService.update("123", incoming);

        verify(em).persist(any(AuthModel.class));
        verify(jwtService).deleteSessionVersion("123");
    }

    @Test
    void testSetStatus() {
        UserModel user = new UserModel();
        user.setId("123");
        user.setActive(true);

        when(userRepository.setStatus(eq("123"), anyBoolean())).thenReturn(user);

        userService.setStatus("123", false);
        verify(jwtService).deleteSessionVersion("123");
    }

    @Test
    void testSetStatus_UserNotFound() {
        when(userRepository.setStatus(anyString(), anyBoolean())).thenReturn(null);
        userService.setStatus("nonexistent", true);
        verify(jwtService, never()).deleteSessionVersion(anyString());
    }

    @Test
    void testUpdate_EmptyFields() {
        UserModel existing = new UserModel();
        existing.setId("123");
        when(userRepository.findById("123")).thenReturn(existing);

        UserModel incoming = new UserModel(); // Everything null
        incoming.setActive(null); // Override BaseEntity default true
        userService.update("123", incoming);

        verify(jwtService, never()).deleteSessionVersion(anyString());
    }

    @Test
    void testMergeFields() {
        // Just to cover the empty method
        userService.mergeFields(new UserModel(), new UserModel());
    }

    @Test
    void testUpdate_UserNotFound() {
        when(userRepository.findById("nonexistent")).thenReturn(null);
        UserModel result = userService.update("nonexistent", new UserModel());
        assertNull(result);
    }

    @Test
    void testCreateUser_BlankPassword() {
        UserModel user = new UserModel();
        user.setEmail("test@test.com");
        user.setPassword("   "); // Blank string

        userService.create(user);

        verify(em).persist(argThat(auth -> {
            AuthModel a = (AuthModel) auth;
            return a.getActive();
        }));
    }

    @Test
    void testUpdateUser_BlankPassword() {
        UserModel existing = new UserModel();
        existing.setId("123");
        when(userRepository.findById("123")).thenReturn(existing);

        UserModel incoming = new UserModel();
        incoming.setActive(null);
        incoming.setPassword(""); // Empty

        userService.update("123", incoming);

        verify(em, never()).find(AuthModel.class, "123");
        verify(jwtService, never()).deleteSessionVersion("123");
    }

    @Test
    void testUserModel_GettersSetters() {
        UserModel user = new UserModel();
        user.setName("Name");
        user.setEmail("email@test.com");
        user.setPassword("pass");
        user.setIdRole("role");
        user.setPhone("123");
        user.setCognitoId("cog");
        user.setDocument("doc");
        user.setAvatar("ava");
        user.setRole(new com.app.modules.role.RoleModel());
        user.setAuth(new com.app.modules.auth.AuthModel());

        assertEquals("Name", user.getName());
        assertEquals("email@test.com", user.getEmail());
        assertEquals("pass", user.getPassword());
        assertEquals("role", user.getIdRole());
        assertEquals("123", user.getPhone());
        assertEquals("cog", user.getCognitoId());
        assertEquals("doc", user.getDocument());
        assertEquals("ava", user.getAvatar());
        assertNotNull(user.getRole());
        assertNotNull(user.getAuth());
    }

    @Test
    void testUserResourceConstructor() {
        UserResource resource = new UserResource(userService);
        assertNotNull(resource);
    }

    @Test
    void testDelete_UserNotFound() {
        when(userRepository.findById("nonexistent")).thenReturn(null);
        boolean result = userService.delete("nonexistent");
        assertFalse(result);
    }

    @Test
    void testDelete_UserExists_WithAuth() {
        UserModel user = new UserModel();
        user.setId("123");
        user.setName("John Doe");
        user.setEmail("john@email.com");
        user.setActive(true);

        when(userRepository.findById("123")).thenReturn(user);

        AuthModel auth = new AuthModel();
        auth.setId("123");
        auth.setActive(true);
        when(em.find(AuthModel.class, "123")).thenReturn(auth);

        boolean result = userService.delete("123");

        assertTrue(result);
        assertEquals("Deleted User", user.getName());
        assertTrue(user.getEmail().startsWith("deleted-"));
        assertTrue(user.getEmail().endsWith("-anonymized@email.com"));
        assertFalse(user.getActive());
        assertTrue(user.getIsDeleted());
        assertNotNull(user.getDeletedAt());

        verify(userRepository).persist(user);
        verify(em).merge(auth);
        assertFalse(auth.getActive());
        verify(jwtService).deleteSessionVersion("123");
    }

    @Test
    void testDelete_UserExists_WithoutAuth() {
        UserModel user = new UserModel();
        user.setId("123");
        user.setName("John Doe");
        user.setEmail("john@email.com");
        user.setActive(true);

        when(userRepository.findById("123")).thenReturn(user);
        when(em.find(AuthModel.class, "123")).thenReturn(null);

        boolean result = userService.delete("123");

        assertTrue(result);
        assertEquals("Deleted User", user.getName());
        verify(userRepository).persist(user);
        verify(em, never()).merge(any(AuthModel.class));
        verify(jwtService).deleteSessionVersion("123");
    }

    @Test
    void testExportPdf() {
        UserModel user1 = new UserModel();
        user1.setId("1");
        user1.setName("User One");
        user1.setEmail("one@test.com");
        user1.setActive(true);
        user1.setPhone("123456");

        com.app.modules.role.RoleModel role = new com.app.modules.role.RoleModel();
        role.setName("Admin Role");
        user1.setRole(role);

        UserModel user2 = new UserModel();
        user2.setId("2");
        user2.setName("User Two");
        user2.setEmail("two@test.com");
        user2.setActive(false);
        user2.setPhone(null);
        user2.setRole(null);

        List<UserModel> users = List.of(user1, user2);
        when(userRepository.searchAll(eq(em), eq(UserModel.class), any())).thenReturn(users);

        byte[] pdfBytes = "Dummy PDF Content".getBytes();
        InputStream mockStream = new ByteArrayInputStream(pdfBytes);
        when(pdfProvider.generatePdf(any(PdfRequestDTO.class))).thenReturn(mockStream);

        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("name", "User");
        queryParams.put("page", "1");
        queryParams.put("size", "50");

        InputStream result = userService.exportPdf(queryParams);

        assertNotNull(result);
        assertSame(mockStream, result);

        verify(userRepository).searchAll(eq(em), eq(UserModel.class), argThat(filter -> {
            assertNotNull(filter);
            assertEquals("createdAt", filter.getOrderBy());
            assertEquals("desc", filter.getOrderDirection());
            return true;
        }));

        verify(pdfProvider).generatePdf(argThat(req -> {
            assertEquals("user-list", req.getTemplate());
            Map<String, Object> data = req.getData();
            assertEquals("Relatório de Usuários", data.get("title"));
            assertNotNull(data.get("generatedAt"));
            List<Map<String, Object>> usersData = (List<Map<String, Object>>) data.get("users");
            assertEquals(2, usersData.size());

            Map<String, Object> u1 = usersData.get(0);
            assertEquals("1", u1.get("id"));
            assertEquals("User One", u1.get("name"));
            assertEquals("one@test.com", u1.get("email"));
            assertEquals("123456", u1.get("phone"));
            assertEquals("Admin Role", u1.get("roleName"));
            assertEquals(true, u1.get("active"));

            Map<String, Object> u2 = usersData.get(1);
            assertEquals("2", u2.get("id"));
            assertEquals("User Two", u2.get("name"));
            assertEquals("two@test.com", u2.get("email"));
            assertNull(u2.get("phone"));
            assertNull(u2.get("roleName"));
            assertEquals(false, u2.get("active"));

            return true;
        }));
    }

    @Test
    void testExportPdf_Asc() {
        when(userRepository.searchAll(eq(em), eq(UserModel.class), any())).thenReturn(Collections.emptyList());

        byte[] pdfBytes = "Dummy PDF Content".getBytes();
        InputStream mockStream = new ByteArrayInputStream(pdfBytes);
        when(pdfProvider.generatePdf(any(PdfRequestDTO.class))).thenReturn(mockStream);

        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("orderDirection", "asc");

        InputStream result = userService.exportPdf(queryParams);

        assertNotNull(result);
        assertSame(mockStream, result);

        verify(userRepository).searchAll(eq(em), eq(UserModel.class), argThat(filter -> {
            assertNotNull(filter);
            assertEquals("createdAt", filter.getOrderBy());
            assertEquals("asc", filter.getOrderDirection());
            return true;
        }));
    }

    @Test
    void testUpdateWithPassword_AuthNoSessionVersion() {
        UserModel existing = new UserModel();
        existing.setId("123");
        when(userRepository.findById("123")).thenReturn(existing);

        UserModel incoming = new UserModel();
        incoming.setPassword("newpass");

        AuthModel auth = mock(AuthModel.class);
        when(auth.getSessionVersion()).thenReturn(null);
        when(em.find(AuthModel.class, "123")).thenReturn(auth);

        userService.update("123", incoming);

        verify(auth).setSessionVersion(1);
        verify(jwtService).deleteSessionVersion("123");
    }

    @Test
    void testDelete_WithoutAuthSessionVersion() {
        UserModel user = new UserModel();
        user.setId("123");
        user.setName("John");
        user.setEmail("john@test.com");
        when(userRepository.findById("123")).thenReturn(user);

        AuthModel auth = mock(AuthModel.class);
        when(auth.getSessionVersion()).thenReturn(null);
        when(em.find(AuthModel.class, "123")).thenReturn(auth);

        userService.delete("123");
        verify(auth).setSessionVersion(1);
        verify(jwtService).deleteSessionVersion("123");
    }
}
