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

import org.jboss.as.controller.access.Action;
import org.jboss.as.controller.access.constraint.Constraint;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Ladislav Thon <lthon@redhat.com>
 */
public class SimpleManagementPermissionTestCase {
    private static final SimpleManagementPermission ACCESS_ALLOWED_REQUIRED = new SimpleManagementPermission(Action.ActionEffect.ACCESS, new TestConstraint(true));
    private static final SimpleManagementPermission ACCESS_DISALLOWED_REQUIRED = new SimpleManagementPermission(Action.ActionEffect.ACCESS, new TestConstraint(false));
    private static final SimpleManagementPermission READ_CONFIG_ALLOWED_REQUIRED = new SimpleManagementPermission(Action.ActionEffect.READ_CONFIG, new TestConstraint(true));
    private static final SimpleManagementPermission READ_RUNTIME_ALLOWED_REQUIRED = new SimpleManagementPermission(Action.ActionEffect.READ_RUNTIME, new TestConstraint(true));
    private static final SimpleManagementPermission READ_RUNTIME_DISALLOWED_REQUIRED = new SimpleManagementPermission(Action.ActionEffect.READ_RUNTIME, new TestConstraint(false));

    @Test
    public void testSameActionEffectAndSameConstraint() {
        assertTrue(ACCESS_ALLOWED_REQUIRED.implies(ACCESS_ALLOWED_REQUIRED));
    }

    @Test
    public void testSameActionEffectAndDifferentConstraint() {
        assertFalse(ACCESS_ALLOWED_REQUIRED.implies(ACCESS_DISALLOWED_REQUIRED));
    }

    @Test
    public void testDifferentActionEffectButSameConstraint() {
        assertFalse(READ_CONFIG_ALLOWED_REQUIRED.implies(READ_RUNTIME_ALLOWED_REQUIRED));
    }

    @Test
    public void testDifferentActionEffectAndDifferentConstraint() {
        assertFalse(READ_CONFIG_ALLOWED_REQUIRED.implies(READ_RUNTIME_ALLOWED_REQUIRED));
        assertFalse(READ_CONFIG_ALLOWED_REQUIRED.implies(READ_RUNTIME_DISALLOWED_REQUIRED));
    }

    // ---

    private static final class TestConstraint implements Constraint {
        private final boolean allowed;

        private TestConstraint(boolean allowed) {
            this.allowed = allowed;
        }

        @Override
        public boolean violates(Constraint other) {
            if (other instanceof TestConstraint) {
                return this.allowed != ((TestConstraint) other).allowed;
            }
            return false;
        }

        @Override
        public int compareTo(Constraint o) {
            return 0;
        }
    }
}
