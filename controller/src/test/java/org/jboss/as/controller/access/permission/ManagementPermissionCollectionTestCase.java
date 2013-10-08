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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.security.Permission;
import java.util.Enumeration;

import org.jboss.as.controller.access.Action;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Ladislav Thon <lthon@redhat.com>
 */
public class ManagementPermissionCollectionTestCase {
    private ManagementPermissionCollection permissionCollection;

    @Before
    public void setUp() {
        permissionCollection = new ManagementPermissionCollection(getClass().getSimpleName(), TestManagementPermission.class);
    }

    @Test
    public void testAdd() {
        try {
            permissionCollection.add(new TestManagementPermission(Action.ActionEffect.ADDRESS));
        } catch (Exception e) {
            fail();
        }

        try {
            permissionCollection.add(new AnotherTestManagementPermission(Action.ActionEffect.ADDRESS));
            fail();
        } catch (IllegalArgumentException ignored) { /* expected */ }

        try {
            permissionCollection.add(new RuntimePermission("bad"));
            fail();
        } catch (IllegalArgumentException ignored) { /* expected */ }
    }

    @Test
    public void testElements() {
        permissionCollection.add(new TestManagementPermission(Action.ActionEffect.ADDRESS));
        permissionCollection.add(new TestManagementPermission(Action.ActionEffect.READ_CONFIG));
        permissionCollection.add(new TestManagementPermission(Action.ActionEffect.READ_RUNTIME));

        Enumeration<Permission> elements = permissionCollection.elements();
        while (elements.hasMoreElements()) {
            ManagementPermission permission = (ManagementPermission) elements.nextElement();
            Action.ActionEffect actionEffect = permission.getActionEffect();
            assertTrue(actionEffect == Action.ActionEffect.ADDRESS
                    || actionEffect == Action.ActionEffect.READ_CONFIG
                    || actionEffect == Action.ActionEffect.READ_RUNTIME);
        }
    }

    @Test
    public void testImplies() {
        permissionCollection.add(new TestManagementPermission(Action.ActionEffect.ADDRESS));
        assertTrue(permissionCollection.implies(new TestManagementPermission(Action.ActionEffect.ADDRESS)));
        assertFalse(permissionCollection.implies(new TestManagementPermission(Action.ActionEffect.READ_CONFIG)));

        permissionCollection.add(new TestManagementPermission(Action.ActionEffect.READ_CONFIG));
        assertTrue(permissionCollection.implies(new TestManagementPermission(Action.ActionEffect.ADDRESS)));
        assertTrue(permissionCollection.implies(new TestManagementPermission(Action.ActionEffect.READ_CONFIG)));
        assertFalse(permissionCollection.implies(new TestManagementPermission(Action.ActionEffect.READ_RUNTIME)));
    }

    @Test
    public void testReadOnly() {
        assertFalse(permissionCollection.isReadOnly());

        try {
            permissionCollection.add(new TestManagementPermission(Action.ActionEffect.ADDRESS));
        } catch (Exception e) {
            fail();
        }

        permissionCollection.setReadOnly();
        assertTrue(permissionCollection.isReadOnly());

        try {
            permissionCollection.add(new TestManagementPermission(Action.ActionEffect.READ_CONFIG));
            fail();
        } catch (SecurityException ignored) { /* expected */ }
    }

    // ---

    private static final class TestManagementPermission extends ManagementPermission {
        private TestManagementPermission(Action.ActionEffect actionEffect) {
            super("test", actionEffect);
        }

        @Override
        public boolean implies(Permission permission) {
            return equals(permission);
        }
    }

    private static final class AnotherTestManagementPermission extends ManagementPermission {
        private AnotherTestManagementPermission(Action.ActionEffect actionEffect) {
            super("test2", actionEffect);
        }

        @Override
        public boolean implies(Permission permission) {
            return equals(permission);
        }
    }
}
