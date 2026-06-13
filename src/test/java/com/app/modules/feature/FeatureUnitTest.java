package com.app.modules.feature;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FeatureUnitTest {

    @Test
    void testFeatureModel() {
        FeatureModel feature = new FeatureModel();
        feature.setName("Test Feature");
        feature.setDescription("A test feature");

        assertEquals("Test Feature", feature.getName());
        assertEquals("A test feature", feature.getDescription());
    }
}
