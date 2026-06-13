package com.app.infrastructure;

import io.quarkus.test.junit.QuarkusTestProfile;
import java.util.Map;

public class MockRedisProfile implements QuarkusTestProfile {
    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of("quarkus.redis.devservices.enabled", "false");
    }
}
