package com.app.modules.role;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RoleUnitTest {

    @Test
    void testRoleFeatureId() {
        RoleFeatureId id1 = new RoleFeatureId("r1", "f1");
        RoleFeatureId id2 = new RoleFeatureId("r1", "f1");
        RoleFeatureId id3 = new RoleFeatureId("r2", "f1");
        RoleFeatureId id4 = new RoleFeatureId("r1", "f2");

        assertEquals(id1, id2);
        assertEquals(id1.hashCode(), id2.hashCode());
        assertNotEquals(id1, id3);
        assertNotEquals(id1, id4);
        assertNotEquals(null, id1);
        assertNotEquals(id1, "string");
        assertEquals(id1, id1);

        RoleFeatureId empty = new RoleFeatureId();
        assertNull(empty.idRole);
        assertNull(empty.idFeature);
    }

    @Test
    void testRoleFeatureModel() {
        RoleFeatureModel rf = new RoleFeatureModel();

        rf.setIdRole("role-1");
        rf.setIdFeature("feat-1");
        rf.setPermissions("{\"create\":true,\"view\":false,\"delete\":true,\"activate\":false}");

        assertEquals("role-1", rf.getIdRole());
        assertEquals("feat-1", rf.getIdFeature());
        assertTrue(rf.isCreate());
        assertFalse(rf.isView());
        assertTrue(rf.isDelete());
        assertFalse(rf.isActivate());

        RoleModel role = new RoleModel();
        rf.setRole(role);
        assertSame(role, rf.getRole());
    }

    @Test
    void testRoleFeatureModelNullPermissions() {
        RoleFeatureModel rf = new RoleFeatureModel();
        rf.setPermissions(null);
        assertFalse(rf.isCreate());
        assertFalse(rf.isView());
        assertFalse(rf.isDelete());
        assertFalse(rf.isActivate());
    }

    @Test
    void testRoleFeatureModelBlankPermissions() {
        RoleFeatureModel rf = new RoleFeatureModel();
        rf.setPermissions("   ");
        assertFalse(rf.isCreate());
        assertFalse(rf.isView());
        assertFalse(rf.isDelete());
        assertFalse(rf.isActivate());
    }

    @Test
    void testRoleFeatureModelInvalidJsonPermissions() {
        RoleFeatureModel rf = new RoleFeatureModel();
        rf.setPermissions("not-json");
        assertFalse(rf.isCreate());
        assertFalse(rf.isView());
        assertFalse(rf.isDelete());
        assertFalse(rf.isActivate());
    }

    @Test
    void testRoleModel() {
        RoleModel role = new RoleModel();

        role.setName("Admin");
        role.setDescription("Admin role");

        assertEquals("Admin", role.getName());
        assertEquals("Admin role", role.getDescription());

        List<RoleFeatureModel> features = new ArrayList<>();
        RoleFeatureModel rf = new RoleFeatureModel();
        rf.setIdFeature("f1");
        features.add(rf);
        role.setRoleFeatures(features);
        assertEquals(1, role.getRoleFeatures().size());

        role.setPermissions(List.of(Map.of("feature", "test")));
        assertNotNull(role.getPermissions());
        assertEquals("test", role.getPermissions().get(0).get("feature"));
    }
}
