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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.access.Action;
import org.jboss.as.controller.access.TargetAttribute;
import org.jboss.as.controller.access.TargetResource;
import org.jboss.as.controller.access.rbac.StandardRole;

/**
 * {@link Constraint} related to whether a resource, attribute or operation is NOT
 * related to administrative audit logging.
 *
 * @author Brian Stansberry (c) 2013 Red Hat Inc.
 */
public class NonAuditConstraint extends AllowAllowNotConstraint {

    public static final ConstraintFactory FACTORY = new Factory();

    private static final NonAuditConstraint AUDIT = new NonAuditConstraint(true);
    private static final NonAuditConstraint NOT_AUDIT = new NonAuditConstraint(false);
    private static final NonAuditConstraint ALLOWS = new NonAuditConstraint(true, true);
    private static final NonAuditConstraint DISALLOWS = new NonAuditConstraint(true, false);

    private NonAuditConstraint(boolean isAudit) {
        super(isAudit);
    }

    private NonAuditConstraint(boolean allowsAudit, boolean allowsNonAudit) {
        super(allowsAudit, allowsNonAudit);
    }

    private static class Factory extends AbstractConstraintFactory {

        @Override
        public Constraint getStandardUserConstraint(StandardRole role, Action.ActionEffect actionEffect) {
            return role == StandardRole.AUDITOR
                    && (actionEffect == Action.ActionEffect.WRITE_CONFIG
                         || actionEffect == Action.ActionEffect.WRITE_RUNTIME)
                    ? DISALLOWS : ALLOWS;
        }

        @Override
        public Constraint getRequiredConstraint(Action.ActionEffect actionEffect, Action action, TargetAttribute target) {
            return getRequiredConstraint(actionEffect, action, target.getTargetResource());
        }

        @Override
        public Constraint getRequiredConstraint(Action.ActionEffect actionEffect, Action action, TargetResource target) {
            return (isAuditOperation(action) || isAuditResource(target)) ? AUDIT : NOT_AUDIT;
        }

        private boolean isAuditOperation(Action action) {
            return AuditLogAddressUtil.isAuditLogAddress(PathAddress.pathAddress(action.getOperation().get(OP_ADDR)));
        }

        private boolean isAuditResource(TargetResource target) {
            return AuditLogAddressUtil.isAuditLogAddress(target.getResourceAddress());
        }

        @Override
        protected int internalCompare(AbstractConstraintFactory other) {
            // We prefer going ahead of anything except a ScopingConstraint or AuditConstraint
            return (other instanceof ScopingConstraintFactory || other instanceof AuditConstraint.Factory)  ? 1 : -1;

        }
    }
}
