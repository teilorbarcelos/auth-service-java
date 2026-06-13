package com.app.infrastructure.seed;

import com.app.modules.auth.AuthModel;
import com.app.modules.feature.FeatureModel;
import com.app.modules.feature.FeatureRepository;
import com.app.modules.role.RoleModel;
import com.app.modules.role.RoleFeatureModel;
import com.app.modules.role.RoleRepository;
import com.app.modules.user.UserModel;
import com.app.modules.user.UserRepository;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import org.mindrot.jbcrypt.BCrypt;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Seeds initial data on application startup.
 * Equivalent to DatabaseBootstrap.php
 */
@ApplicationScoped
public class DatabaseBootstrap {

    private static final Logger LOG = Logger.getLogger(DatabaseBootstrap.class);

    @Inject
    UserRepository userRepository;

    @Inject
    RoleRepository roleRepository;

    @Inject
    FeatureRepository featureRepository;

    @Inject
    EntityManager em;

    @ConfigProperty(name = "app.seed.first-user", defaultValue = "admin@email.com")
    String adminEmail;

    @ConfigProperty(name = "app.seed.first-password", defaultValue = "admin@123")
    String adminPassword;

    @Transactional
    void onStartup(@Observes StartupEvent event) {
        try {
            LOG.info("Seeding initial data (roles, features and permissions)...");

            RoleModel adminRole = findOrCreateRole("administrator", "Administrador", "Acesso total ao sistema");
            findOrCreateRole("user", "Usuário", "Acesso básico");

            List<Map<String, String>> features = List.of(
                    Map.of("id", "user", "name", "Usuários", "description", "Gestão de usuários"),
                    Map.of("id", "role", "name", "Papéis", "description", "Gestão de papéis e permissões"),
                    Map.of("id", "product", "name", "Produtos", "description", "Gestão de produtos"),
                    Map.of("id", "feature", "name", "Features", "description", "Gestão de features"),
                    Map.of("id", "dashboard", "name", "Dashboard", "description", "Visualização de gráficos e estatísticas"));

            for (Map<String, String> f : features) {
                findOrCreateFeature(f.get("id"), f.get("name"), f.get("description"));

                RoleFeatureModel rf = new RoleFeatureModel();
                rf.setIdRole("administrator");
                rf.setIdFeature(f.get("id"));
                rf.setPermissions("{\"create\":true,\"view\":true,\"delete\":true,\"activate\":true}");
                em.merge(rf);
            }

            UserModel existingAdmin = userRepository.findByEmail(adminEmail);
            if (existingAdmin != null) {
                LOG.info("Admin user already exists. Skipping user seed.");
                return;
            }

            LOG.info("Seeding initial administrator user...");
            String userId = UUID.randomUUID().toString();

            UserModel adminUser = new UserModel();
            adminUser.setId(userId);
            adminUser.setName("Administrator");
            adminUser.setEmail(adminEmail);
            adminUser.setIdRole(adminRole.getId());
            adminUser.setActive(true);
            userRepository.persist(adminUser);

            AuthModel auth = new AuthModel();
            auth.setId(userId);
            auth.setPassword(BCrypt.hashpw(adminPassword, BCrypt.gensalt()));
            auth.setFirstAccess(false);
            em.persist(auth);

            LOG.infov("Seed completed. Admin user: {0}", adminEmail);
        } catch (Exception e) {
            LOG.errorv("Database seed failed: {0}", e.getMessage());
        }
    }

    private RoleModel findOrCreateRole(String id, String name, String description) {
        RoleModel existing = roleRepository.findById(id);
        if (existing != null)
            return existing;

        RoleModel role = new RoleModel();
        role.setId(id);
        role.setName(name);
        role.setDescription(description);
        roleRepository.persist(role);
        return role;
    }

    private void findOrCreateFeature(String id, String name, String description) {
        FeatureModel existing = featureRepository.findById(id);
        if (existing != null)
            return;

        FeatureModel feature = new FeatureModel();
        feature.setId(id);
        feature.setName(name);
        feature.setDescription(description);
        featureRepository.persist(feature);
    }
}
