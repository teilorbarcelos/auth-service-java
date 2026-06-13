package com.app.modules.user;

import com.app.core.BaseService;
import com.app.infrastructure.auth.JwtService;
import com.app.modules.auth.AuthModel;
import com.app.infrastructure.pdf.PdfProvider;
import com.app.infrastructure.pdf.PdfRequestDTO;
import com.app.core.QueryFilter;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.mindrot.jbcrypt.BCrypt;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

/**
 * User service with validation and token invalidation on role/status change.
 * Equivalent to UserService.php
 */
@ApplicationScoped
public class UserService extends BaseService<UserModel, UserRepository> {

    @Inject
    JwtService jwtService;

    @Inject
    PdfProvider pdfProvider;

    @Inject
    public UserService(UserRepository userRepository, EntityManager em) {
        this.repository = userRepository;
        this.em = em;
        this.entityClass = UserModel.class;
        this.filterableFields = List.of("name", "email", "active", "id_role", "role.name", "createdAt");
        this.searchableFields = List.of("name", "email", "role.name");
    }

    @Override
    @Transactional
    public UserModel create(UserModel user) {
        String rawPassword = user.getPassword();
        if (rawPassword == null || rawPassword.isBlank()) {
            rawPassword = "User@123";
        }

        UserModel created = super.create(user);

        AuthModel auth = new AuthModel();
        auth.setId(created.getId());
        auth.setPassword(BCrypt.hashpw(rawPassword, BCrypt.gensalt()));
        auth.setFirstAccess(true);
        auth.setActive(true);
        em.persist(auth);

        return created;
    }

    @Override
    @Transactional
    public UserModel update(String id, UserModel incoming) {
        UserModel existing = repository.findById(id);
        if (existing == null)
            return null;

        boolean needsInvalidation = false;

        if (incoming.getName() != null)
            existing.setName(incoming.getName());
        if (incoming.getEmail() != null)
            existing.setEmail(incoming.getEmail());
        if (incoming.getIdRole() != null) {
            existing.setIdRole(incoming.getIdRole());
            needsInvalidation = true;
        }
        if (incoming.getActive() != null) {
            existing.setActive(incoming.getActive());
            needsInvalidation = true;
        }

        if (incoming.getPassword() != null && !incoming.getPassword().isBlank()) {
            AuthModel auth = em.find(AuthModel.class, id);
            if (auth == null) {
                auth = new AuthModel();
                auth.setId(id);
                em.persist(auth);
            }
            auth.setPassword(BCrypt.hashpw(incoming.getPassword(), BCrypt.gensalt()));
            em.merge(auth);
            needsInvalidation = true;
        }

        repository.persist(existing);

        if (needsInvalidation) {
            invalidateSession(id);
        }

        return existing;
    }

    @Override
    @Transactional
    public UserModel setStatus(String id, boolean active) {
        UserModel user = super.setStatus(id, active);
        if (user != null) {
            invalidateSession(id);
        }
        return user;
    }

    @Override
    @Transactional
    public boolean delete(String id) {
        UserModel user = repository.findById(id);
        if (user == null) {
            return false;
        }

        // LGPD Anonymization
        user.setName("Deleted User");
        user.setEmail("deleted-" + java.util.UUID.randomUUID().toString() + "-anonymized@email.com");
        user.setActive(false);
        user.setIsDeleted(true);
        user.setDeletedAt(java.time.LocalDateTime.now());

        repository.persist(user);

        // Deactivate associated Auth record and invalidate sessions
        AuthModel auth = em.find(AuthModel.class, id);
        if (auth != null) {
            auth.setActive(false);
            int newVersion = (auth.getSessionVersion() != null ? auth.getSessionVersion() : 0) + 1;
            auth.setSessionVersion(newVersion);
            em.merge(auth);
        }
        jwtService.deleteSessionVersion(id);

        return true;
    }

    private void invalidateSession(String userId) {
        AuthModel auth = em.find(AuthModel.class, userId);
        if (auth != null) {
            int newVersion = (auth.getSessionVersion() != null ? auth.getSessionVersion() : 0) + 1;
            auth.setSessionVersion(newVersion);
            em.merge(auth);
        }
        jwtService.deleteSessionVersion(userId);
    }

    @Override
    protected void mergeFields(UserModel existing, UserModel incoming) {
    }

    public InputStream exportPdf(Map<String, String> queryParams) {
        Map<String, String> params = new HashMap<>(queryParams);
        params.remove("page");
        params.remove("size");

        QueryFilter filter = QueryFilter.fromQueryParams(params, filterableFields, searchableFields, false);
        List<UserModel> users = repository.searchAll(em, UserModel.class, filter);

        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        String localTime = now.format(formatter);

        List<Map<String, Object>> usersData = new ArrayList<>();
        for (UserModel u : users) {
            Map<String, Object> userData = new HashMap<>();
            userData.put("id", u.getId());
            userData.put("name", u.getName());
            userData.put("email", u.getEmail());
            userData.put("phone", u.getPhone());
            userData.put("roleName", u.getRole() != null ? u.getRole().getName() : null);
            userData.put("active", u.getActive());
            usersData.add(userData);
        }

        Map<String, Object> pdfData = new HashMap<>();
        pdfData.put("title", "Relatório de Usuários");
        pdfData.put("generatedAt", localTime);
        pdfData.put("users", usersData);

        PdfRequestDTO pdfRequest = new PdfRequestDTO();
        pdfRequest.setTemplate("user-list");
        pdfRequest.setData(pdfData);

        return pdfProvider.generatePdf(pdfRequest);
    }
}
