package com.app.modules.role;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class RoleFeatureUnitTest {

    @Test
    void testRoleFeatureModel_GetPermissions() {
        RoleFeatureModel rf = new RoleFeatureModel();
        
        // Null permissions
        rf.setPermissions(null);
        assertFalse(rf.isCreate());
        assertFalse(rf.isView());
        assertFalse(rf.isDelete());
        assertFalse(rf.isActivate());

        // Blank permissions
        rf.setPermissions("   ");
        assertFalse(rf.isCreate());

        // Invalid JSON
        rf.setPermissions("{invalid}");
        assertFalse(rf.isCreate());

        // Valid JSON but missing keys
        rf.setPermissions("{}");
        assertFalse(rf.isCreate());

        // Valid JSON with true values
        rf.setPermissions("{\"create\":true, \"view\":true, \"delete\":true, \"activate\":true}");
        assertTrue(rf.isCreate());
        assertTrue(rf.isView());
        assertTrue(rf.isDelete());
        assertTrue(rf.isActivate());

        // Valid JSON with false values
        rf.setPermissions("{\"create\":false, \"view\":false}");
        assertFalse(rf.isCreate());
        assertFalse(rf.isView());
    }

    @Test
    void testRoleFeatureModel_Relationship() {
        RoleFeatureModel rf = new RoleFeatureModel();
        RoleModel role = new RoleModel();
        role.setId("admin");
        
        rf.setRole(role);
        assertEquals(role, rf.getRole());
    }

    @Test
    void testRoleFeatureId_EqualsHashCode() {
        RoleFeatureId id1 = new RoleFeatureId("r1", "f1");
        RoleFeatureId id2 = new RoleFeatureId("r1", "f1");
        RoleFeatureId id3 = new RoleFeatureId("r1", "f2");
        RoleFeatureId id4 = new RoleFeatureId(null, null);
        RoleFeatureId id5 = new RoleFeatureId(null, null);

        // Standard equals
        assertEquals(id1, id1);
        assertEquals(id1, id2);
        assertNotEquals(id1, id3);
        assertNotEquals(id1, null);
        assertNotEquals(id1, "string");

        // HashCode
        assertEquals(id1.hashCode(), id2.hashCode());
        assertNotEquals(id1.hashCode(), id3.hashCode());

        // Null fields
        assertEquals(id4, id5);
        assertNotEquals(id1, id4);
        assertEquals(id4.hashCode(), id5.hashCode());
        
        // Constructor test
        RoleFeatureId defaultId = new RoleFeatureId();
        assertNull(defaultId.idRole);
    }
}
