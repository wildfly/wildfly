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
import org.jboss.as.controller.access.Action.ActionEffect;
import org.jboss.as.controller.access.TargetAttribute;
import org.jboss.as.controller.access.TargetResource;
import org.jboss.as.controller.access.rbac.StandardRole;

/**
 * {@link Constraint} related to whether a resource, attribute or operation is
 * related to administrative audit logging.
 *
 * @author Brian Stansberry (c) 2013 Red Hat Inc.
 */
public class AuditConstraint extends AllowAllowNotConstraint {

    public static final ConstraintFactory FACTORY = new Factory();

    private static final AuditConstraint AUDIT = new AuditConstraint(true);
    private static final AuditConstraint NOT_AUDIT = new AuditConstraint(false);
    private static final AuditConstraint ALLOWS = new AuditConstraint(true, true);
    private static final AuditConstraint DISALLOWS = new AuditConstraint(false, true);

    private AuditConstraint(boolean isAudit) {
        super(isAudit);
    }

    private AuditConstraint(boolean allowsAudit, boolean allowsNonAudit) {
        super(allowsAudit, allowsNonAudit);
    }

    static class Factory extends AbstractConstraintFactory {

        @Override
        public Constraint getStandardUserConstraint(StandardRole role, Action.ActionEffect actionEffect) {
            if (actionEffect == ActionEffect.ADDRESS) {
                return ALLOWS;
            }
            return role == StandardRole.AUDITOR || role == StandardRole.SUPERUSER ? ALLOWS : DISALLOWS;
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
            // We prefer going ahead of anything except a ScopingConstraint
            return other instanceof ScopingConstraintFactory ? 1 : -1;
        }
    }
}
