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

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.jboss.as.controller.ControllerLogger;
import org.jboss.as.controller.access.Action;
import org.jboss.as.controller.access.HostEffect;
import org.jboss.as.controller.access.TargetAttribute;
import org.jboss.as.controller.access.TargetResource;
import org.jboss.as.controller.access.rbac.StandardRole;

/**
 * Constraint related to whether the target resource is associated with one or more managed domain hosts.
 *
 * @author Brian Stansberry (c) 2013 Red Hat Inc.
 */
public class HostEffectConstraint extends AbstractConstraint implements Constraint, ScopingConstraint {


    public static final ScopingConstraintFactory FACTORY = new Factory();

    private static final HostEffectConstraint GLOBAL_USER = new HostEffectConstraint(true);
    private static final HostEffectConstraint GLOBAL_REQUIRED = new HostEffectConstraint(false);

    private final boolean user;
    private final boolean global;
    private final HostsHolder hostsHolder;
    private final boolean readOnly;
    private final HostEffectConstraint readOnlyConstraint;

    private HostEffectConstraint(final boolean user) {
        super();
        this.user = user;
        this.global = true;
        this.readOnly = false;
        this.readOnlyConstraint = null;
        this.hostsHolder = new HostsHolder();
    }

    private HostEffectConstraint(Set<String> allowed) {
        super();
        this.user = false;
        this.global = false;
        this.hostsHolder = new HostsHolder(allowed);
        this.readOnly = false;
        this.readOnlyConstraint = null;
    }

    public HostEffectConstraint(List<String> allowed) {
        super();
        this.user = true;
        this.global = false;
        this.hostsHolder = new HostsHolder(allowed);
        this.readOnly = false;
        this.readOnlyConstraint = new HostEffectConstraint(this.hostsHolder, true);
    }

    /**
     * Creates the constraint the standard constraint will return from {@link #getOutofScopeReadConstraint()}
     * Only call from {@link HostEffectConstraint#HostEffectConstraint(java.util.List)}
     */
    private HostEffectConstraint(HostsHolder hostsHolder, boolean readOnly) {
        super();
        this.user = true;
        this.global = false;
        this.hostsHolder = hostsHolder;
        this.readOnly = readOnly;
        this.readOnlyConstraint = null;
    }

    public void setAllowedHosts(List<String> allowed) {
        assert !global : "constraint is global";
        assert readOnlyConstraint != null : "invalid cast";
        this.hostsHolder.specific = new LinkedHashSet<String>(allowed);
    }

    @Override
    public boolean violates(Constraint other, Action.ActionEffect actionEffect) {
        if (other instanceof HostEffectConstraint) {
            HostEffectConstraint hec = (HostEffectConstraint) other;
            if (user) {
                assert !hec.user : "illegal comparison";
                if (readOnly) {
                    // Allow global or any matching server group
                    if (!hec.global) {
                        boolean anyMatch = anyMatch(hec);
                        if (!anyMatch) ControllerLogger.ACCESS_LOGGER.tracef("read-only host constraint violated " +
                                "for action %s due to no match between hosts %s and allowed hosts %s",
                                actionEffect, hec.hostsHolder.specific, hostsHolder.specific);
                        return !anyMatch;
                    }
                } else if (!global) {
                    if (hec.global) {
                        // Only the readOnlyConstraint gets global
                        ControllerLogger.ACCESS_LOGGER.tracef("host constraint violated for action %s due to " +
                                "requirement for access to global resources", actionEffect);
                        return true;
                    } else {
                        if (actionEffect == Action.ActionEffect.WRITE_RUNTIME || actionEffect == Action.ActionEffect.WRITE_CONFIG) {
                            //  Writes must not effect other groups
                            boolean containsAll = hostsHolder.specific.containsAll(hec.hostsHolder.specific);
                            if (!containsAll) {
                                ControllerLogger.ACCESS_LOGGER.tracef("host constraint violated for action %s due to " +
                                        "mismatch of hosts %s vs hosts %s", actionEffect, hec.hostsHolder.specific, hostsHolder.specific);
                            }
                            return !containsAll;
                        } else {
                            // Reads ok as long as one of our groups match
                            boolean anyMatch = anyMatch(hec);
                            if (!anyMatch) ControllerLogger.ACCESS_LOGGER.tracef("host constraint violated " +
                                    "for action %s due to no match between hosts %s and allowed hosts %s",
                                    actionEffect, hec.hostsHolder.specific, hostsHolder.specific);
                            return !anyMatch;
                        }
                    } // else fall through
                }
            } else {
                assert hec.user : "illegal comparison";
                return other.violates(this, actionEffect);
            }
        }
        return false;
    }

    private boolean anyMatch(HostEffectConstraint hec) {

        boolean matched = false;
        for (String ourGroup : hostsHolder.specific) {
            if (hec.hostsHolder.specific.contains(ourGroup)) {
                matched = true;
                break;
            }
        }
        return matched;
    }


    @Override
    public boolean replaces(Constraint other) {
        return other instanceof HostEffectConstraint && (readOnly || readOnlyConstraint != null);
    }

    // Scoping Constraint

    @Override
    public ScopingConstraintFactory getFactory() {
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

    private static class HostsHolder {
        private volatile Set<String> specific = new LinkedHashSet<String>();
        private HostsHolder() {
            // no-op
        }
        private HostsHolder(Collection<String> hosts) {
            this.specific.addAll(hosts);
        }
    }

    private static class Factory extends AbstractConstraintFactory implements ScopingConstraintFactory {

        @Override
        public Constraint getStandardUserConstraint(StandardRole role, Action.ActionEffect actionEffect) {
            return GLOBAL_USER;
        }

        @Override
        public Constraint getRequiredConstraint(Action.ActionEffect actionEffect, Action action, TargetAttribute target) {
            return getRequiredConstraint(target.getHostEffect());
        }

        @Override
        public Constraint getRequiredConstraint(Action.ActionEffect actionEffect, Action action, TargetResource target) {
            return getRequiredConstraint(target.getHostEffect());
        }

        private Constraint getRequiredConstraint(HostEffect hostEffect) {
            if (hostEffect == null || hostEffect.isHostEffectGlobal()) {
                return GLOBAL_REQUIRED;
            }
            return new HostEffectConstraint(hostEffect.getAffectedHosts());
        }

        @Override
        protected int internalCompare(AbstractConstraintFactory other) {
            // We prefer going first
            return this.equals(other) ? 0 : -1;
        }
    }
}
