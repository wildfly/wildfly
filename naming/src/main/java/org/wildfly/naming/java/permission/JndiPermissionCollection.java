/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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

package org.wildfly.naming.java.permission;

import java.security.Permission;
import java.security.PermissionCollection;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.concurrent.atomic.AtomicReference;

import org.jboss.as.naming.logging.NamingLogger;
import org.wildfly.common.Assert;

final class JndiPermissionCollection extends PermissionCollection {

    private static final long serialVersionUID = - 769684900128311150L;
    private static final JndiPermission[] NO_PERMISSIONS = new JndiPermission[0];

    private final AtomicReference<JndiPermission[]> permissions;

    JndiPermissionCollection() {
        permissions = new AtomicReference<>(NO_PERMISSIONS);
    }

    JndiPermissionCollection(JndiPermission[] permissions) {
        Assert.checkNotNullParam("permissions", permissions);
        this.permissions = new AtomicReference<>(permissions);
    }

    public void add(final Permission permission) {
        if (isReadOnly()) {
            throw NamingLogger.ROOT_LOGGER.cannotAddToReadOnlyPermissionCollection();
        }
        if (! (permission instanceof JndiPermission)) {
            throw NamingLogger.ROOT_LOGGER.invalidPermission(permission);
        }
        final AtomicReference<JndiPermission[]> permissions = this.permissions;
        JndiPermission jndiPermission = (JndiPermission) permission;
        if (jndiPermission.getActionBits() == 0) {
            // no operation
            return;
        }
        JndiPermission[] oldVal;
        ArrayList<JndiPermission> newVal;
        boolean added = false;
        do {
            oldVal = permissions.get();
            newVal = new ArrayList<>(oldVal.length + 1);
            // first, test if it's in the set, or combine with any other permission with the same actions
            for (final JndiPermission testPerm : oldVal) {
                if (testPerm.implies(jndiPermission)) {
                    // already in the set
                    return;
                } else if (jndiPermission.implies(testPerm)) {
                    // otherwise skip all other matches
                } else if (jndiPermission.getName().equals(testPerm.getName())) {
                    // the two .implies() would have caught this condition
                    assert jndiPermission.getActionBits() != testPerm.getActionBits();
                    jndiPermission = jndiPermission.withActions(testPerm.getActionBits());
                    // and skip it
                }
            }
            for (final JndiPermission testPerm : oldVal) {
                if (! jndiPermission.implies(testPerm)) {
                    newVal.add(testPerm);
                }
            }
            newVal.add(jndiPermission);
        } while (! permissions.compareAndSet(oldVal, newVal.toArray(NO_PERMISSIONS)));
    }

    public boolean implies(final Permission permission) {
        final JndiPermission[] jndiPermissions = permissions.get();
        for (JndiPermission jndiPermission : jndiPermissions) {
            if (jndiPermission.implies(permission)) {
                return true;
            }
        }
        return false;
    }

    public Enumeration<Permission> elements() {
        final JndiPermission[] jndiPermissions = permissions.get();
        return new Enumeration<Permission>() {
            int i;
            public boolean hasMoreElements() {
                return i < jndiPermissions.length;
            }

            public Permission nextElement() {
                return jndiPermissions[i++];
            }
        };
    }

    Object writeReplace() {
        return new SerializedJndiPermissionCollection(isReadOnly(), permissions.get());
    }
}
