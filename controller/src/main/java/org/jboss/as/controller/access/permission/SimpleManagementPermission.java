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
import org.jboss.as.controller.access.constraint.ScopingConstraint;

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
            Constraint violatedRequisiteConstraint = null;
            Constraint violatedRequiredConstraint = null;
            Constraint violatedOptionalConstraint = null;
            for (int i = 0; i < constraints.length && violatedRequisiteConstraint == null; i++) {
                Constraint ours = constraints[i];
                Constraint theirs = other.constraints[i];
                assert ours.getClass() == theirs.getClass() : "incompatible constraints: ours = " + ours.getClass() + " -- theirs = " + theirs.getClass();
                if (ours.violates(theirs)) {
                    switch (ours.getControlFlag()) {
                        case REQUISITE:
                            violatedRequisiteConstraint = constraints[i];
                            break;
                        case REQUIRED:
                        case SUFFICIENT:
                            if (violatedRequiredConstraint == null) {
                                violatedRequiredConstraint = constraints[i];
                            }
                            break;
                        case OPTIONAL:
                            if (violatedOptionalConstraint == null) {
                                violatedOptionalConstraint = constraints[i];
                            }
                            break;
                        default:
                            throw new IllegalStateException();
                    }
                } else if (constraints[i].getControlFlag() == Constraint.ControlFlag.SUFFICIENT) {
                    // passes
                    violatedOptionalConstraint = null;
                    break;
                }
            }
            return violatedRequisiteConstraint == null && violatedRequiredConstraint == null && violatedOptionalConstraint == null;
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
    public ManagementPermission createScopedPermission(ScopingConstraint constraint) {
        boolean added = false;
        List<Constraint> newList = new ArrayList<Constraint>();
        for (Constraint existing : constraints) {
            int compare = existing.compareTo(constraint);
            if (compare > 0) {
                if (!added) {
                    newList.add(constraint);
                    added = true;
                }
            } else if (compare == 0) {
                assert existing.equals(constraint) : "inconsistent equals and compareTo in " + constraint + " and " + existing;
                continue;
            } else if (compare < 0) {
                assert !added : "inconsistent ordering of constraints";
            }
            newList.add(existing);
        }
        if (!added) {
            newList.add(constraint);
        }
        return new SimpleManagementPermission(getActionEffect(), newList);
    }
}
