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

package org.jboss.as.controller.access.constraint;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.jboss.as.controller.access.Action;
import org.jboss.as.controller.access.ServerGroupEffect;
import org.jboss.as.controller.access.TargetAttribute;
import org.jboss.as.controller.access.TargetResource;
import org.jboss.as.controller.access.rbac.StandardRole;

/**
 * Constraint related to whether the target resource is associated with one or more managed domain server groups.
 *
 * @author Brian Stansberry (c) 2013 Red Hat Inc.
 */
public class ServerGroupEffectConstraint extends AbstractConstraint implements Constraint, ScopingConstraint {


    public static final ConstraintFactory FACTORY = new Factory();

    private static final ServerGroupEffectConstraint GLOBAL_USER = new ServerGroupEffectConstraint(true);
    private static final ServerGroupEffectConstraint GLOBAL_REQUIRED = new ServerGroupEffectConstraint(false);
    private static final ServerGroupEffectConstraint UNASSIGNED = new ServerGroupEffectConstraint();

    private final boolean user;
    private final boolean global;
    private final boolean unassigned;
    private volatile Set<String> specific = new LinkedHashSet<String>();
    private final boolean readOnly;
    private final ServerGroupEffectConstraint readOnlyConstraint;

    private ServerGroupEffectConstraint() {
        this.user = false;
        this.global = false;
        this.unassigned = true;
        this.readOnly = false;
        this.readOnlyConstraint = null;
    }

    private ServerGroupEffectConstraint(final boolean user) {
        this.user = user;
        this.global = true;
        this.unassigned = false;
        this.readOnly = false;
        this.readOnlyConstraint = null;
    }

    private ServerGroupEffectConstraint(Set<String> allowed) {
        this.user = false;
        this.global = false;
        this.unassigned = false;
        specific.addAll(allowed);
        this.readOnly = false;
        this.readOnlyConstraint = null;
    }

    public ServerGroupEffectConstraint(List<String> allowed) {
        this.user = true;
        this.global = false;
        this.unassigned = false;
        specific.addAll(allowed);
        this.readOnly = false;
        this.readOnlyConstraint = new ServerGroupEffectConstraint(allowed, true);
    }

    /**
     * Creates the constraint the standard constraint will return from {@link #getOutofScopeReadConstraint()}
     * Only call from {@link ServerGroupEffectConstraint#ServerGroupEffectConstraint(java.util.List)}
     */
    private ServerGroupEffectConstraint(List<String> allowed, boolean readOnly) {
        this.user = true;
        this.global = false;
        this.unassigned = false;
        specific.addAll(allowed);
        this.readOnly = readOnly;
        this.readOnlyConstraint = null;
    }

    public void setAllowedGroups(List<String> allowed) {
        assert !global : "constraint is global";
        assert readOnlyConstraint != null : "invalid cast";
        this.specific = new LinkedHashSet<String>(allowed);
        this.readOnlyConstraint.setAllowedGroups(allowed);
    }

    @Override
    public boolean violates(Constraint other, Action.ActionEffect actionEffect) {
        if (other instanceof ServerGroupEffectConstraint) {
            ServerGroupEffectConstraint sgec = (ServerGroupEffectConstraint) other;
            if (user) {
                assert !sgec.user : "illegal comparison";
                if (readOnly) {
                    // Allow global or any matching server group
                    if (!sgec.global) {
                        return !anyMatch(sgec);
                    }
                } else if (!global) {
                    if (sgec.global) {
                        // Only the readOnlyConstraint gets global
                        return true;
                    } else if (!sgec.unassigned) {
                        if (actionEffect == Action.ActionEffect.WRITE_RUNTIME || actionEffect == Action.ActionEffect.WRITE_CONFIG) {
                            //  Writes must not effect other groups
                            return !specific.containsAll(sgec.specific);
                        } else {
                            // Reads ok as long as one of our groups match
                            return !anyMatch(sgec);
                        }
                    } // else fall through
                }
            } else {
                assert sgec.user : "illegal comparison";
                return other.violates(this, actionEffect);
            }
        }
        return false;
    }

    private boolean anyMatch(ServerGroupEffectConstraint sgec) {

        boolean matched = false;
        for (String ourGroup : specific) {
            if (sgec.specific.contains(ourGroup)) {
                matched = true;
                break;
            }
        }
        return matched;
    }

    @Override
    public boolean replaces(Constraint other) {
        return other instanceof ServerGroupEffectConstraint && (readOnly || readOnlyConstraint != null);
    }

    // Scoping Constraint

    @Override
    public ConstraintFactory getFactory() {
        assert readOnlyConstraint != null : "invalid cast";
        return FACTORY;
    }

    @Override
    public Constraint getStandardConstraint() {
        assert readOnlyConstraint != null : "invalid cast";
        return this;
    }

    @Override
    public Constraint getOutofScopeReadConstraint() {
        assert readOnlyConstraint != null : "invalid cast";
        return readOnlyConstraint;
    }

    @Override
    protected int internalCompare(AbstractConstraint other) {
        // We prefer going first
        return this.equals(other) ? 0 : -1;
    }

    private static class Factory implements ConstraintFactory {

        @Override
        public Constraint getStandardUserConstraint(StandardRole role, Action.ActionEffect actionEffect) {
            return GLOBAL_USER;
        }

        @Override
        public Constraint getRequiredConstraint(Action.ActionEffect actionEffect, Action action, TargetAttribute target) {
            return getRequiredConstraint(target.getServerGroupEffect());
        }

        @Override
        public Constraint getRequiredConstraint(Action.ActionEffect actionEffect, Action action, TargetResource target) {
            return getRequiredConstraint(target.getServerGroupEffect());
        }

        private Constraint getRequiredConstraint(ServerGroupEffect serverGroupEffect) {
            if (serverGroupEffect == null || serverGroupEffect.isServerGroupEffectGlobal()) {
                return GLOBAL_REQUIRED;
            } else if (serverGroupEffect.isServerGroupEffectUnassigned()) {
                return UNASSIGNED;
            }
            return new ServerGroupEffectConstraint(serverGroupEffect.getAffectedServerGroups());
        }
    }
}
