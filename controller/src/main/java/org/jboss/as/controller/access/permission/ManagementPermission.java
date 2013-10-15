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

import org.jboss.as.controller.access.Action;

/**
 * Base class for {@link Permission} implementations related to WildFly access control.
 *
 * @author Brian Stansberry (c) 2013 Red Hat Inc.
 */
public abstract class ManagementPermission extends Permission {

    private final Action.ActionEffect actionEffect;

    /**
     * Constructs a permission with the specified name and action effect.
     */
    ManagementPermission(String name, Action.ActionEffect actionEffect) {
        super(name);
        this.actionEffect = actionEffect;
    }

    @Override
    public PermissionCollection newPermissionCollection() {
        return new ManagementPermissionCollection(getClass());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ManagementPermission that = (ManagementPermission) o;

        return actionEffect == that.actionEffect;

    }

    @Override
    public int hashCode() {
        return actionEffect.hashCode();
    }

    @Override
    public String getActions() {
        return actionEffect.toString();
    }

    public Action.ActionEffect getActionEffect() {
        return actionEffect;
    }
}
