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

/**
 * {@link PermissionCollection} that implies all permissions.
 *
 * @author Brian Stansberry (c) 2013 Red Hat Inc.
 */
public final class AllPermissionsCollection extends PermissionCollection {

    public static final AllPermissionsCollection INSTANCE = new AllPermissionsCollection();

    private AllPermissionsCollection() {
    }

    @Override
    public void add(Permission permission) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean implies(Permission permission) {
        return true;
    }

    @Override
    public Enumeration<Permission> elements() {
        throw new UnsupportedOperationException();
    }
}
