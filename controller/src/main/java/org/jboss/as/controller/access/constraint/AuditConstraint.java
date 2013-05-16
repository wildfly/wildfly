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

import org.jboss.as.controller.access.Action;
import org.jboss.as.controller.access.rbac.StandardRole;
import org.jboss.as.controller.access.TargetAttribute;
import org.jboss.as.controller.access.TargetResource;

/**
 * TODO class javadoc.
 *
 * @author Brian Stansberry (c) 2013 Red Hat Inc.
 */
public class AuditConstraint extends AllowAllowNotConstraint {

    public static final ConstraintFactory FACTORY = new Factory();

    private AuditConstraint(boolean required, boolean isAudit) {
        super(required ? ControlFlag.SUFFICIENT : ControlFlag.REQUIRED, isAudit);
    }

    private AuditConstraint(boolean required, boolean allowsAudit, boolean allowsNonAudit) {
        super(required ? ControlFlag.SUFFICIENT : ControlFlag.REQUIRED, allowsAudit, allowsNonAudit);
    }

    @Override
    public int compareTo(Constraint o) {
        // We prefer going ahead of anything except a ScopingConstraint
        if (o instanceof ScopingConstraint) {
            return 1;
        }
        return this.equals(o) ? 0 : -1;
    }

    @Override
    protected int internalCompare(AbstractConstraint other) {
        // We prefer going ahead of anything except a ScopingConstraint
        return other instanceof ScopingConstraint ? 1 : -1;
    }

    private static class Factory implements ConstraintFactory {

        @Override
        public Constraint getStandardUserConstraint(StandardRole role, Action.ActionEffect actionEffect) {
            //TODO implement getStandardUserConstraint
            throw new UnsupportedOperationException();
        }

        @Override
        public Constraint getRequiredConstraint(Action.ActionEffect actionEffect, Action action, TargetAttribute target) {
            //TODO implement getRequiredConstraint
            throw new UnsupportedOperationException();
        }

        @Override
        public Constraint getRequiredConstraint(Action.ActionEffect actionEffect, Action action, TargetResource target) {
            //TODO implement getRequiredConstraint
            throw new UnsupportedOperationException();
        }
    }
}
