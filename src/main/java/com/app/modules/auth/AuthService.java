package com.app.modules.auth;

import com.app.core.exception.BadRequestException;
import com.app.infrastructure.auth.JwtService;
import com.app.modules.auth.dto.AuthResponseDTO;
import com.app.infrastructure.email.EmailProvider;
import com.app.infrastructure.email.EmailTemplates;
import com.app.modules.role.RoleFeatureModel;
import com.app.modules.user.UserModel;
import com.app.modules.user.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.mindrot.jbcrypt.BCrypt;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Authentication service — login, me, refresh, password reset.
 * Equivalent to AuthService.php
 */
@ApplicationScoped
public class AuthService {
    private static final String USER_NOT_FOUND = "User not found";


    @Inject
    JwtService jwtService;

    @Inject
    UserRepository userRepository;

    @Inject
    EmailProvider emailProvider;

    @Inject
    EntityManager em;

    @Inject
    ObjectMapper objectMapper;

    @Transactional
    public AuthResponseDTO login(String email, String password) {
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            throw new BadRequestException("Email e password são obrigatórios");
        }

        UserModel user = userRepository.findByEmail(email);
        if (user == null) {
            throw new WebApplicationException("Invalid credentials", Response.Status.UNAUTHORIZED);
        }

        AuthModel auth = em.find(AuthModel.class, user.getId());
        if (auth == null || !BCrypt.checkpw(password, auth.getPassword())) {
            throw new WebApplicationException("Invalid credentials", Response.Status.UNAUTHORIZED);
        }

        if (!user.getActive()) {
            throw new WebApplicationException("Account is disabled", Response.Status.FORBIDDEN);
        }

        if (user.getRole() != null && !user.getRole().getActive()) {
            throw new WebApplicationException("Role is disabled", Response.Status.FORBIDDEN);
        }

        return buildAuthResponse(user, "Login successful");
    }

    @Transactional
    public AuthResponseDTO getMe(String userId) {
        UserModel user = userRepository.findById(userId);
        if (user == null) {
            throw new WebApplicationException(USER_NOT_FOUND, Response.Status.NOT_FOUND);
        }
        return buildAuthResponse(user, "User found");
    }

    @Transactional
    public AuthResponseDTO refreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BadRequestException("Refresh token é obrigatório");
        }

        Map<String, Object> claims = jwtService.validateToken(refreshToken);
        if (claims == null || claims.get("uid") == null) {
            throw new WebApplicationException("Invalid or expired refresh token", Response.Status.UNAUTHORIZED);
        }

        String uid = claims.get("uid").toString();
        long sv = claims.get("sv") instanceof Number n ? n.longValue() : -1;
        if (sv < 0 || jwtService.getSessionVersion(uid) != sv) {
            throw new WebApplicationException("Invalid or expired refresh token", Response.Status.UNAUTHORIZED);
        }

        return getMe(uid);
    }

    @Transactional
    public void requestPasswordReset(String email) {
        UserModel user = userRepository.findByEmail(email);
        if (user == null)
            return;

        AuthModel auth = em.find(AuthModel.class, user.getId());
        if (auth == null)
            return;

        String token = String.valueOf(100000 + new Random().nextInt(900000));
        auth.setRequestPasswordToken(token);
        auth.setRequestPasswordExpiration(LocalDateTime.now().plusMinutes(15));
        em.merge(auth);

        String html = EmailTemplates.render(EmailTemplates.FORGOT_PASSWORD_TEMPLATE, Map.of(
                "name", user.getName(),
                "token", token));

        emailProvider.sendEmail(email, "Recuperação de Senha", html);
    }

    @Transactional
    public boolean validateResetToken(String email, String token) {
        UserModel user = userRepository.findByEmail(email);
        if (user == null) {
            throw new WebApplicationException(USER_NOT_FOUND, Response.Status.NOT_FOUND);
        }

        AuthModel auth = em.find(AuthModel.class, user.getId());
        if (auth == null) {
            throw new WebApplicationException(USER_NOT_FOUND, Response.Status.NOT_FOUND);
        }

        if (token == null || !token.equals(auth.getRequestPasswordToken())) {
            throw new WebApplicationException("Invalid reset token", Response.Status.UNAUTHORIZED);
        }

        if (auth.getRequestPasswordExpiration() != null
                && auth.getRequestPasswordExpiration().isBefore(LocalDateTime.now())) {
            throw new WebApplicationException("Reset token has expired", Response.Status.UNAUTHORIZED);
        }

        return true;
    }

    @Transactional
    public void resetPassword(String email, String token, String newPassword) {
        validateResetToken(email, token);

        UserModel user = userRepository.findByEmail(email);
        if (user == null) {
            throw new WebApplicationException(USER_NOT_FOUND, Response.Status.NOT_FOUND);
        }

        AuthModel auth = em.find(AuthModel.class, user.getId());
        if (auth == null) {
            throw new WebApplicationException(USER_NOT_FOUND, Response.Status.NOT_FOUND);
        }

        auth.setPassword(BCrypt.hashpw(newPassword, BCrypt.gensalt()));
        auth.setRequestPasswordToken(null);
        auth.setRequestPasswordExpiration(null);
        auth.setRetries(0);
        em.merge(auth);
    }

    // --- Private helpers ---

    private AuthResponseDTO buildAuthResponse(UserModel user, String message) {
        List<Map<String, Object>> permissions = getFormattedPermissions(user);

        AuthModel auth = em.find(AuthModel.class, user.getId());
        long sessionVersion = auth != null ? auth.getSessionVersion() : 1L;

        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("email", user.getEmail());
        claims.put("roleId", user.getIdRole() != null ? user.getIdRole() : "user");
        claims.put("permissions", permissions);
        claims.put("sv", sessionVersion);

        Map<String, String> tokens = jwtService.createTokenPair(user.getId(), claims);
        jwtService.saveSessionVersion(user.getId(), sessionVersion);

        String roleName = "User";
        String roleDescription = "";
        if (user.getRole() != null) {
            roleName = user.getRole().getName();
            roleDescription = user.getRole().getDescription() != null ? user.getRole().getDescription() : "";
        }

        AuthResponseDTO response = new AuthResponseDTO();
        response.setMessage(message);
        response.setValid(true);
        response.setToken(tokens.get("token"));
        response.setRefreshToken(tokens.get("refreshToken"));

        AuthResponseDTO.UserData userData = new AuthResponseDTO.UserData();
        userData.setId(user.getId());
        userData.setName(user.getName());
        userData.setEmail(user.getEmail());

        AuthResponseDTO.RoleData roleData = new AuthResponseDTO.RoleData();
        roleData.setId(user.getIdRole() != null ? user.getIdRole() : "user");
        roleData.setName(roleName);
        roleData.setDescription(roleDescription);
        roleData.setPermissions(permissions);

        userData.setRole(roleData);
        response.setUser(userData);

        return response;
    }

    private Map<String, Object> parsePermissions(String json) {
        if (json == null)
            return Map.of();
        try {
            Map<String, Object> perms = objectMapper.readValue(json, Map.class);
            return perms != null ? perms : Map.of();
        } catch (Exception e) {
            return Map.of();
        }
    }

    private List<Map<String, Object>> getFormattedPermissions(UserModel user) {
        List<RoleFeatureModel> roleFeatures = em.createQuery(
                "SELECT rf FROM RoleFeatureModel rf WHERE rf.idRole = :roleId", RoleFeatureModel.class)
                .setParameter("roleId", user.getIdRole())
                .getResultList();

        List<Map<String, Object>> result = new ArrayList<>();
        for (RoleFeatureModel rf : roleFeatures) {
            Map<String, Object> perms = parsePermissions(rf.getPermissions());

            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("feature", rf.getIdFeature());
            entry.put("create", Boolean.TRUE.equals(perms.get("create")));
            entry.put("view", Boolean.TRUE.equals(perms.get("view")));
            entry.put("delete", Boolean.TRUE.equals(perms.get("delete")));
            entry.put("activate", Boolean.TRUE.equals(perms.get("activate")));
            result.add(entry);
        }

        return result;
    }
}
