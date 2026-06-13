package com.app.infrastructure.seed;

import com.app.modules.feature.FeatureModel;
import com.app.modules.feature.FeatureRepository;
import com.app.modules.role.RoleModel;
import com.app.modules.role.RoleRepository;
import com.app.modules.user.UserModel;
import com.app.modules.user.UserRepository;
import io.quarkus.runtime.StartupEvent;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class DatabaseBootstrapUnitTest {

    @InjectMocks
    DatabaseBootstrap databaseBootstrap;

    @Mock
    UserRepository userRepository;

    @Mock
    RoleRepository roleRepository;

    @Mock
    FeatureRepository featureRepository;

    @Mock
    EntityManager em;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        databaseBootstrap.adminEmail = "admin@email.com";
        databaseBootstrap.adminPassword = "admin";
    }

    @Test
    void testSeedAlreadyExists() {
        when(userRepository.findByEmail(anyString())).thenReturn(new UserModel());

        databaseBootstrap.onStartup(new StartupEvent());

        verify(userRepository, never()).persist(any(UserModel.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void testSeedNewInstallation() {
        when(userRepository.findByEmail(anyString())).thenReturn(null);
        when(roleRepository.findById(anyString())).thenReturn(null);
        when(featureRepository.findById(anyString())).thenReturn(null);

        databaseBootstrap.onStartup(new StartupEvent());

        verify(roleRepository, atLeastOnce()).persist(any(RoleModel.class));
        verify(featureRepository, atLeastOnce()).persist(any(FeatureModel.class));
        verify(userRepository, times(1)).persist(any(UserModel.class));
        verify(em, atLeastOnce()).persist(any(Object.class));
        verify(em, atLeastOnce()).merge(any(Object.class));
    }

    @Test
    void testSeedPartialInstallation() {
        when(userRepository.findByEmail(anyString())).thenReturn(null);
        
        // Return existing role and feature for some calls
        when(roleRepository.findById("administrator")).thenReturn(new RoleModel());
        when(featureRepository.findById("user")).thenReturn(new FeatureModel());

        databaseBootstrap.onStartup(new StartupEvent());

        // Verify it didn't persist administrator role again
        verify(roleRepository, never()).persist(argThat((RoleModel r) -> r != null && "administrator".equals(r.getId())));
        // But did persist the user role
        verify(roleRepository, atLeastOnce()).persist(argThat((RoleModel r) -> r != null && "user".equals(r.getId())));
    }

    @Test
    void testSeedFailure() {
        when(userRepository.findByEmail(anyString())).thenThrow(new RuntimeException("DB Error"));

        databaseBootstrap.onStartup(new StartupEvent());
        // Should catch and log
    }
}
