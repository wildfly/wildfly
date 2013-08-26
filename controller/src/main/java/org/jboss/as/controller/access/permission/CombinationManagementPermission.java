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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.jboss.as.controller.ControllerMessages;
import org.jboss.as.controller.access.Action;
import org.jboss.as.controller.access.constraint.Constraint;

/**
 * A {@link ManagementPermission} that combines multiple underlying permissions according
 * to a {@link CombinationPolicy}.
 *
 * @author Brian Stansberry (c) 2013 Red Hat Inc.
 */
public class CombinationManagementPermission extends ManagementPermission {

    private final CombinationPolicy combinationPolicy;
    private final List<ManagementPermission> underlyingPermissions = new ArrayList<ManagementPermission>();

    public CombinationManagementPermission(CombinationPolicy combinationPolicy, Action.ActionEffect actionEffect) {
        super("CombinationManagementPermission", actionEffect);
        this.combinationPolicy = combinationPolicy;
    }

    public void addUnderlyingPermission(ManagementPermission underlyingPermission) {
        assert underlyingPermission.getActionEffect() == getActionEffect() : "incompatible ActionEffect";
        if (combinationPolicy == CombinationPolicy.REJECTING && underlyingPermissions.size() > 0) {
            throw ControllerMessages.MESSAGES.illegalMultipleRoles();
        }
        synchronized (underlyingPermissions) {
            underlyingPermissions.add(underlyingPermission);
        }
    }

    @Override
    public String getActions() {
        Set<Action.ActionEffect> effects = new TreeSet<Action.ActionEffect>();
        for (ManagementPermission permission : underlyingPermissions) {
            effects.add(permission.getActionEffect());
        }
        boolean first = true;
        StringBuilder sb = new StringBuilder();
        for (Action.ActionEffect effect : effects) {
            if (!first) {
                sb.append(',');
            } else {
                first = false;
            }
            sb.append(effect.toString());
        }
        return sb.toString();
    }

    @Override
    public boolean implies(Permission permission) {
        for (ManagementPermission underlying : underlyingPermissions) {
            if (combinationPolicy == CombinationPolicy.PERMISSIVE) {
                if (underlying.implies(permission)) {
                    return true;
                }
            } else if (!underlying.implies(permission)) {
                return false;
            }
        }

        return combinationPolicy != CombinationPolicy.PERMISSIVE;
    }

    @Override
    public PermissionCollection newPermissionCollection() {
        return new ManagementPermissionCollection(getClass());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CombinationManagementPermission that = (CombinationManagementPermission) o;

        // TODO I believe that actionEffect should be taken into account here (and in hashCode)

        return combinationPolicy == that.combinationPolicy && underlyingPermissions.equals(that.underlyingPermissions);

    }

    @Override
    public int hashCode() {
        int result = combinationPolicy.hashCode();
        result = 31 * result + underlyingPermissions.hashCode();
        return result;
    }

    @Override
    public ManagementPermission createScopedPermission(Constraint constraint, int constraintIndex) {
        CombinationManagementPermission result = new CombinationManagementPermission(combinationPolicy, getActionEffect());
        synchronized (underlyingPermissions) {
            for (ManagementPermission underlying : underlyingPermissions) {
                result.addUnderlyingPermission(underlying.createScopedPermission(constraint, constraintIndex));
            }
        }
        return result;
    }
}
