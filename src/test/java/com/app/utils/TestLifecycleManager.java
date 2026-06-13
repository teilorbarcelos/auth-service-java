package com.app.utils;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import java.util.Map;

public class TestLifecycleManager implements QuarkusTestResourceLifecycleManager {

    @Override
    public Map<String, String> start() {
        // We can't easily get the EntityManager here because CDI is not yet started.
        // However, Quarkus allows us to run SQL during bootstrap via
        // application.properties or other means.
        // For H2, we can also use an init script.
        return Map.of();
    }

    @Override
    public void stop() {
    }
}
