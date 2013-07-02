package org.jboss.as.controller.access.permission;

import org.jboss.as.controller.access.Action;
import org.jboss.as.controller.access.constraint.ScopingConstraint;
import org.junit.Test;

import java.security.Permission;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Ladislav Thon <lthon@redhat.com>
 */
public class CombinationManagementPermissionTestCase {
    private static final ManagementPermission ACCESS_1 = new TestManagementPermission(Action.ActionEffect.ACCESS, 1);
    private static final ManagementPermission ACCESS_2 = new TestManagementPermission(Action.ActionEffect.ACCESS, 2);
    private static final ManagementPermission ACCESS_3 = new TestManagementPermission(Action.ActionEffect.ACCESS, 3);
    private static final ManagementPermission READ_CONFIG_1 = new TestManagementPermission(Action.ActionEffect.READ_CONFIG, 1);

    @Test
    public void testPermissive() {
        CombinationManagementPermission cmp = new CombinationManagementPermission(CombinationPolicy.PERMISSIVE, Action.ActionEffect.ACCESS);
        cmp.addUnderlyingPermission(ACCESS_1);
        cmp.addUnderlyingPermission(ACCESS_2);

        assertTrue(cmp.implies(ACCESS_1));
        assertTrue(cmp.implies(ACCESS_2));
        assertFalse(cmp.implies(ACCESS_3));
        assertFalse(cmp.implies(READ_CONFIG_1));
    }

    @Test
    public void testRejecting() {
        CombinationManagementPermission cmp = new CombinationManagementPermission(CombinationPolicy.REJECTING, Action.ActionEffect.ACCESS);
        cmp.addUnderlyingPermission(ACCESS_1);
        assertTrue(cmp.implies(ACCESS_1));
        assertFalse(cmp.implies(ACCESS_2));

        // TODO I believe that this should work, see also the TODO in CombinationManagementPermission
//        try {
//            cmp.addUnderlyingPermission(ACCESS_2);
//            fail();
//        } catch (Exception e) { /* expected */ }
    }

    // TODO CombinationPolicy.RESTRICTIVE is going to be removed, so no test for it...

    // ---

    private static final class TestManagementPermission extends ManagementPermission {
        private final int id;

        private TestManagementPermission(Action.ActionEffect actionEffect, int id) {
            super("test", actionEffect);
            this.id = id;
        }

        @Override
        public ManagementPermission createScopedPermission(ScopingConstraint constraint) {
            return null;
        }

        @Override
        public boolean implies(Permission permission) {
            return equals(permission);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            if (!super.equals(o)) return false;

            TestManagementPermission that = (TestManagementPermission) o;

            if (id != that.id) return false;

            return true;
        }
    }
}
