package com.app.modules.user;

import com.app.modules.auth.AuthModel;
import com.app.modules.role.RoleModel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserModelUnitTest {

    @Test
    void testUserModel() {
        UserModel user = new UserModel();

        user.setName("John Doe");
        user.setEmail("john@test.com");
        user.setPhone("123456789");
        user.setIdRole("role-1");
        user.setCognitoId("cognito-1");
        user.setDocument("doc-1");
        user.setAvatar("avatar-url");
        user.setPassword("secret");

        assertEquals("John Doe", user.getName());
        assertEquals("john@test.com", user.getEmail());
        assertEquals("123456789", user.getPhone());
        assertEquals("role-1", user.getIdRole());
        assertEquals("cognito-1", user.getCognitoId());
        assertEquals("doc-1", user.getDocument());
        assertEquals("avatar-url", user.getAvatar());
        assertEquals("secret", user.getPassword());

        RoleModel role = new RoleModel();
        role.setName("Admin");
        user.setRole(role);
        assertSame(role, user.getRole());

        AuthModel auth = new AuthModel();
        auth.setId("auth-1");
        user.setAuth(auth);
        assertSame(auth, user.getAuth());
    }
}
