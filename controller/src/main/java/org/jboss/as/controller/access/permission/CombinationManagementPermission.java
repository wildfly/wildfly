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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.jboss.as.controller.ControllerLogger;
import org.jboss.as.controller.ControllerMessages;
import org.jboss.as.controller.access.Action;
import org.jboss.as.controller.access.CombinationPolicy;

/**
 * A {@link ManagementPermission} that combines multiple underlying permissions according
 * to a {@link org.jboss.as.controller.access.CombinationPolicy}.
 *
 * @author Brian Stansberry (c) 2013 Red Hat Inc.
 */
public class CombinationManagementPermission extends ManagementPermission {

    private final CombinationPolicy combinationPolicy;
    private final Map<String, ManagementPermission> underlyingPermissions = new HashMap<String, ManagementPermission>();

    public CombinationManagementPermission(CombinationPolicy combinationPolicy, Action.ActionEffect actionEffect) {
        super("CombinationManagementPermission", actionEffect);
        this.combinationPolicy = combinationPolicy;
    }

    /**
     * Adds a permission.
     * <p>
     * This method should not be called after the instance has been made visible to another thread
     * than the one that constructed it.
     * </p>
     * @param permissionName name of the permission to add. Cannot be {@code null}
     * @param underlyingPermission the permission. Cannot be {@code null}
     */
    public void addUnderlyingPermission(String permissionName, ManagementPermission underlyingPermission) {
        assert underlyingPermission.getActionEffect() == getActionEffect() : "incompatible ActionEffect";
        if (combinationPolicy == CombinationPolicy.REJECTING && underlyingPermissions.size() > 0) {
            throw ControllerMessages.MESSAGES.illegalMultipleRoles();
        }
        underlyingPermissions.put(permissionName, underlyingPermission);
    }

    @Override
    public String getActions() {
        Set<Action.ActionEffect> effects = new TreeSet<Action.ActionEffect>();
        for (ManagementPermission permission : underlyingPermissions.values()) {
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
        if (combinationPolicy == CombinationPolicy.PERMISSIVE) {
            for (ManagementPermission underlying : underlyingPermissions.values()) {
                if (underlying.implies(permission)) {
                    return true;
                }
            }
            if (ControllerLogger.ACCESS_LOGGER.isTraceEnabled()) {
                ControllerLogger.ACCESS_LOGGER.tracef("None of the underlying permissions %s imply %s", underlyingPermissions.keySet(), permission);
            }
            return false;
        } else {
            for (Map.Entry<String, ManagementPermission> underlying : underlyingPermissions.entrySet()) {
                if (!underlying.getValue().implies(permission)) {
                    ControllerLogger.ACCESS_LOGGER.tracef("Underlying permission %s does not imply %s", underlying.getKey(), permission);
                    return false;
                }
            }
            return true;

        }
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
}
