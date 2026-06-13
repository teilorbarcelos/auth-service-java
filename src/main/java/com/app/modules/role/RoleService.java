package com.app.modules.role;

import com.app.core.BaseService;
import com.app.core.exception.ValidationException;
import com.app.infrastructure.auth.JwtService;
import com.app.modules.auth.AuthModel;
import com.app.modules.feature.FeatureModel;
import com.app.modules.feature.FeatureRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Transactional;

import java.util.*;

/**
 * Role service with permission sync and user token invalidation.
 * Equivalent to RoleService.php
 */
@ApplicationScoped
public class RoleService extends BaseService<RoleModel, RoleRepository> {
    private static final String PERMISSIONS_KEY = "permissions";


    @Inject
    JwtService jwtService;

    @Inject
    FeatureRepository featureRepository;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    public RoleService(RoleRepository roleRepository, EntityManager em) {
        this.repository = roleRepository;
        this.em = em;
        this.entityClass = RoleModel.class;
        this.filterableFields = List.of("name", "active", "createdAt");
        this.searchableFields = List.of("name");
    }

    public List<FeatureModel> listFeatures() {
        return featureRepository.listAll();
    }

    @Override
    @Transactional
    public RoleModel retrieveById(String id) {
        RoleModel role = super.retrieveById(id);
        if (role != null) {
            role.getRoleFeatures().size();
        }
        return role;
    }

    @Override
    @Transactional
    public RoleModel create(RoleModel role) {
        validateRole(role, false);
        RoleModel created = super.create(role);

        if (role.getPermissions() != null) {
            syncFeatures(created.getId(), role.getPermissions());
            em.flush();
            em.refresh(created);
        }

        return created;
    }

    @Override
    @Transactional
    public RoleModel update(String id, RoleModel incoming) {
        RoleModel existing = repository.findById(id);
        if (existing == null)
            return null;

        mergeFields(existing, incoming);
        repository.persist(existing);

        if (incoming.getPermissions() != null) {
            syncFeatures(id, incoming.getPermissions());
            invalidateUsersWithRole(id);
            em.flush();
            em.refresh(existing);
        }

        if (incoming.getActive() != null && incoming.getPermissions() == null) {
            invalidateUsersWithRole(id);
        }

        org.hibernate.Hibernate.initialize(existing.getRoleFeatures());

        return existing;
    }

    @Transactional
    public RoleModel createWithPermissions(Map<String, Object> data) {
        RoleModel role = new RoleModel();
        if (data.containsKey("id") && data.get("id") != null && !data.get("id").toString().isBlank())
            role.setId(data.get("id").toString());
        if (data.containsKey("name"))
            role.setName(data.get("name").toString());
        if (data.containsKey("description"))
            role.setDescription((String) data.get("description"));

        if (data.containsKey(PERMISSIONS_KEY) && data.get(PERMISSIONS_KEY) instanceof List<?> perms) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> permissions = (List<Map<String, Object>>) perms;
            role.setPermissions(permissions);
        }

        return create(role);
    }

    @Transactional
    public RoleModel updateWithPermissions(String id, Map<String, Object> data) {
        RoleModel incoming = new RoleModel();
        if (data.containsKey("name"))
            incoming.setName(data.get("name").toString());
        if (data.containsKey("description"))
            incoming.setDescription((String) data.get("description"));
        if (data.containsKey("active"))
            incoming.setActive((Boolean) data.get("active"));

        if (data.containsKey(PERMISSIONS_KEY) && data.get(PERMISSIONS_KEY) instanceof List<?> perms) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> permissions = (List<Map<String, Object>>) perms;
            incoming.setPermissions(permissions);
        }

        return update(id, incoming);
    }

    /**
     * Sync role-feature permissions (equivalent to syncFeatures in PHP).
     */
    private void syncFeatures(String roleId, List<?> permissions) {
        if (permissions == null)
            return;

        jakarta.persistence.Query deleteQuery = em
                .createQuery("DELETE FROM RoleFeatureModel rf WHERE rf.idRole = :roleId");
        if (deleteQuery != null) {
            deleteQuery.setParameter("roleId", roleId).executeUpdate();
        }

        for (Object p : permissions) {
            if (p instanceof Map<?, ?> perm) {
                String featureId = perm.get("id_feature") != null ? perm.get("id_feature").toString() : null;
                if (featureId == null)
                    continue;

                RoleFeatureModel rf = new RoleFeatureModel();
                rf.setIdRole(roleId);
                rf.setIdFeature(featureId);

                Map<String, Boolean> permMap = Map.of(
                        "create", Boolean.TRUE.equals(perm.get("create")),
                        "view", Boolean.TRUE.equals(perm.get("view")),
                        "delete", Boolean.TRUE.equals(perm.get("delete")),
                        "activate", Boolean.TRUE.equals(perm.get("activate")));

                try {
                    rf.setPermissions(objectMapper.writeValueAsString(permMap));
                } catch (JsonProcessingException e) {
                    rf.setPermissions("{}");
                }

                em.persist(rf);
            }
        }
    }

    private void invalidateUsersWithRole(String roleId) {
        TypedQuery<String> query = em.createQuery(
                "SELECT u.id FROM UserModel u WHERE u.idRole = :roleId", String.class);

        if (query != null) {
            List<String> userIds = query.setParameter("roleId", roleId).getResultList();
            for (String userId : userIds) {
                AuthModel auth = em.find(AuthModel.class, userId);
                if (auth != null) {
                    int newVersion = (auth.getSessionVersion() != null ? auth.getSessionVersion() : 0) + 1;
                    auth.setSessionVersion(newVersion);
                    em.merge(auth);
                }
                jwtService.deleteSessionVersion(userId);
            }
        }
    }

    @Override
    @Transactional
    public RoleModel setStatus(String id, boolean active) {
        RoleModel result = super.setStatus(id, active);
        if (result != null) {
            invalidateUsersWithRole(id);
            org.hibernate.Hibernate.initialize(result.getRoleFeatures());
        }
        return result;
    }

    @Override
    protected void mergeFields(RoleModel existing, RoleModel incoming) {
        if (incoming.getName() != null)
            existing.setName(incoming.getName());
        if (incoming.getDescription() != null)
            existing.setDescription(incoming.getDescription());
        if (incoming.getActive() != null)
            existing.setActive(incoming.getActive());
    }

    private void validateRole(RoleModel role, boolean isUpdate) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (!isUpdate) {
            if (role.getId() != null && (role.getId().length() < 2 || role.getId().length() > 50))
                errors.put("id", "Id deve ter entre 2 e 50 caracteres");
            if (role.getName() == null || role.getName().length() < 3)
                errors.put("name", "Name deve ter pelo menos 3 caracteres");
        }
        if (!errors.isEmpty())
            throw new ValidationException(errors);
    }
}
