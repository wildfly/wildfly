/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.controller.access.permission;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.security.Permission;

import org.jboss.as.controller.access.Action;
import org.jboss.as.controller.access.CombinationPolicy;
import org.jboss.as.controller.access.constraint.Constraint;
import org.junit.Test;

/**
 * @author Ladislav Thon <lthon@redhat.com>
 */
public class CombinationManagementPermissionTestCase {
    private static final ManagementPermission ACCESS_1 = new TestManagementPermission(Action.ActionEffect.ADDRESS, 1);
    private static final ManagementPermission ACCESS_2 = new TestManagementPermission(Action.ActionEffect.ADDRESS, 2);
    private static final ManagementPermission ACCESS_3 = new TestManagementPermission(Action.ActionEffect.ADDRESS, 3);
    private static final ManagementPermission READ_CONFIG_1 = new TestManagementPermission(Action.ActionEffect.READ_CONFIG, 1);

    @Test
    public void testPermissive() {
        CombinationManagementPermission cmp = new CombinationManagementPermission(CombinationPolicy.PERMISSIVE, Action.ActionEffect.ADDRESS);
        cmp.addUnderlyingPermission("access1", ACCESS_1);
        cmp.addUnderlyingPermission("access2", ACCESS_2);

        assertTrue(cmp.implies(ACCESS_1));
        assertTrue(cmp.implies(ACCESS_2));
        assertFalse(cmp.implies(ACCESS_3));
        assertFalse(cmp.implies(READ_CONFIG_1));
    }

    @Test
    public void testRejecting() {
        CombinationManagementPermission cmp = new CombinationManagementPermission(CombinationPolicy.REJECTING, Action.ActionEffect.ADDRESS);
        cmp.addUnderlyingPermission("access1", ACCESS_1);
        assertTrue(cmp.implies(ACCESS_1));
        assertFalse(cmp.implies(ACCESS_2));

        try {
            cmp.addUnderlyingPermission("access2", ACCESS_2);
            fail();
        } catch (Exception e) { /* expected */ }
    }

    @Test
    public void testRestrictive() {
        // CombinationPolicy.RESTRICTIVE has been removed
        CombinationPolicy[] policySet = CombinationPolicy.values();
        assertEquals(2, policySet.length);
    }

    // ---

    private static final class TestManagementPermission extends ManagementPermission {
        private final int id;

        private TestManagementPermission(Action.ActionEffect actionEffect, int id) {
            super("test", actionEffect);
            this.id = id;
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
