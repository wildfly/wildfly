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

import org.jboss.as.controller.access.Action;
import org.jboss.as.controller.access.constraint.Constraint;

/**
 * Simple implementation of {@link ManagementPermission}.
 *
 * @author Brian Stansberry (c) 2013 Red Hat Inc.
 */
public class SimpleManagementPermission extends ManagementPermission {

    private final Constraint[] constraints;
    /**
     * Constructs a permission with the specified name.
     */
    public SimpleManagementPermission(Action.ActionEffect actionEffect, List<Constraint> constraints) {
        this(actionEffect, constraints.toArray(new Constraint[constraints.size()]));
    }

    public SimpleManagementPermission(Action.ActionEffect actionEffect, Constraint... constraints) {
        super("SimpleManagementPermission", actionEffect);
        this.constraints = constraints;
    }

    @Override
    public PermissionCollection newPermissionCollection() {
        return new ManagementPermissionCollection(getClass());
    }

    @Override
    public boolean implies(Permission permission) {
        if (equals(permission)) {
            SimpleManagementPermission other = (SimpleManagementPermission) permission;
            // Validate constraints
            assert constraints.length == other.constraints.length : String.format("incompatible ManagementPermission; " +
                    "differing constraint counts %d vs %d", constraints.length, other.constraints.length);
            Action.ActionEffect actionEffect = getActionEffect();
            for (int i = 0; i < constraints.length; i++) {
                Constraint ours = constraints[i];
                Constraint theirs = other.constraints[i];
                assert ours.getClass() == theirs.getClass() : "incompatible constraints: ours = " + ours.getClass() + " -- theirs = " + theirs.getClass();
                if (ours.violates(theirs, actionEffect)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ManagementPermission that = (ManagementPermission) o;

        return getActionEffect() == that.getActionEffect();

    }

    @Override
    public int hashCode() {
        return getActionEffect().hashCode();
    }

    @Override
    public String getActions() {
        return getActionEffect().toString();
    }

    @Override
    public ManagementPermission createScopedPermission(Constraint constraint) {
        boolean added = false;
        List<Constraint> newList = new ArrayList<Constraint>();
        for (Constraint existing : constraints) {
            if (constraint.replaces(existing)) {
                // replace existing
                newList.add(constraint);
                added = true;
            } else {
                newList.add(existing);
            }
        }
        if (!added) {
            for (Constraint existing : constraints) {
                int compare = existing.compareTo(constraint);
                if (compare > 0) {
                    if (!added) {
                        newList.add(constraint);
                        added = true;
                    }
                } else if (compare == 0) {
                    assert existing.equals(constraint) : "inconsistent equals and compareTo in " + constraint + " and " + existing;
                }
            }
        }
        if (!added) {
            newList.add(constraint);
        }
        return new SimpleManagementPermission(getActionEffect(), newList);
    }
}
