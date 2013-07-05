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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.jboss.as.controller.access.Action;
import org.jboss.as.controller.access.TargetAttribute;
import org.jboss.as.controller.access.TargetResource;
import org.jboss.as.controller.access.rbac.StandardRole;

/**
 * Constraint related to whether the target resource is associated with one or more managed domain hosts.
 *
 * @author Brian Stansberry (c) 2013 Red Hat Inc.
 */
public class HostEffectConstraint extends AbstractConstraint implements ScopingConstraint {


    public static final ConstraintFactory FACTORY = new Factory();

    public static final HostEffectConstraint GLOBAL = new HostEffectConstraint();

    private final boolean global;
    private final Set<String> specific = new HashSet<String>();

    private HostEffectConstraint() {
        super();
        this.global = true;
    }

    private HostEffectConstraint(Collection<String> allowed) {
        super();
        this.global = false;
        specific.addAll(allowed);
    }

    public HostEffectConstraint(String... allowed) {
        this(Arrays.asList(allowed));
    }

    @Override
    public boolean violates(Constraint other) {
        if (other instanceof HostEffectConstraint) {
            HostEffectConstraint hec = (HostEffectConstraint) other;
            return (global && !hec.global) || !hec.specific.containsAll(specific);
        }
        return false;
    }

    @Override
    protected int internalCompare(AbstractConstraint other) {
        // We prefer going first
        return this.equals(other) ? 0 : -1;
    }

    private static class Factory implements ConstraintFactory {

        @Override
        public Constraint getStandardUserConstraint(StandardRole role, Action.ActionEffect actionEffect) {
            return GLOBAL;
        }

        @Override
        public Constraint getRequiredConstraint(Action.ActionEffect actionEffect, Action action, TargetAttribute target) {
            return getRequiredConstraint(target.getHosts());
        }

        @Override
        public Constraint getRequiredConstraint(Action.ActionEffect actionEffect, Action action, TargetResource target) {
            return getRequiredConstraint(target.getHosts());
        }

        private Constraint getRequiredConstraint(Set<String> hosts) {
            if (hosts == null || hosts.isEmpty()) {
                return GLOBAL;
            }
            return new HostEffectConstraint(hosts);
        }
    }
}
