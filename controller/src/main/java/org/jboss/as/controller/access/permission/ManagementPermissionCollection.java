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

import java.security.Permission;
import java.security.PermissionCollection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.jboss.as.controller.ControllerMessages;
import org.jboss.as.controller.access.Action;

/**
* {@link PermissionCollection} for use with {@link ManagementPermission}. It's homogeneous.
*
* @author Brian Stansberry (c) 2013 Red Hat Inc.
*/
public class ManagementPermissionCollection extends PermissionCollection {

    private final Class<? extends ManagementPermission> type;

    private final Map<Action.ActionEffect, ManagementPermission> permissions = new HashMap<Action.ActionEffect, ManagementPermission>();

    public ManagementPermissionCollection(Class<? extends ManagementPermission> type) {
        this.type = type;
    }

    @Override
    public void add(Permission permission) {
        if (isReadOnly()) {
            throw ControllerMessages.MESSAGES.permissionCollectionIsReadOnly();
        }

        if (type.equals(permission.getClass())) {
            ManagementPermission mperm = (ManagementPermission) permission;
            synchronized (permissions) {
                permissions.put(mperm.getActionEffect(), mperm);
            }
        } else {
            throw ControllerMessages.MESSAGES.incompatiblePermissionType(permission.getClass());
        }
    }

    @Override
    public boolean implies(Permission permission) {
        if (permission instanceof ManagementPermission) {
            ManagementPermission mperm = (ManagementPermission) permission;
            ManagementPermission provided;
            synchronized (permissions) {
                provided = permissions.get(mperm.getActionEffect());
            }
            return provided != null && provided.implies(mperm);
        }
        return false;
    }

    @Override
    public Enumeration<Permission> elements() {
        final Iterator<ManagementPermission> iterator = iterator();
        return new Enumeration<Permission>() {
            @Override
            public boolean hasMoreElements() {
                return iterator.hasNext();
            }

            @Override
            public Permission nextElement() {
                return iterator.next();
            }
        };
    }

    private Iterator<ManagementPermission> iterator() {
        synchronized (permissions) {
            return permissions.values().iterator();
        }
    }
}
