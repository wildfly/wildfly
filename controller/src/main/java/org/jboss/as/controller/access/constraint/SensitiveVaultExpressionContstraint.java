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
import org.jboss.as.controller.access.TargetAttribute;
import org.jboss.as.controller.access.TargetResource;
import org.jboss.as.controller.access.rbac.StandardRole;
import org.jboss.dmr.ModelNode;

/**
 * TODO class javadoc.
 *
 * @author Brian Stansberry (c) 2013 Red Hat Inc.
 */
public class SensitiveVaultExpressionContstraint extends AllowAllowNotConstraint {

    public static final ConstraintFactory FACTORY = new Factory();

    private static final SensitiveVaultExpressionContstraint SENSITIVE = new SensitiveVaultExpressionContstraint(true);
    private static final SensitiveVaultExpressionContstraint NOT_SENSITIVE = new SensitiveVaultExpressionContstraint(false);
    private static final SensitiveVaultExpressionContstraint ALLOWS = new SensitiveVaultExpressionContstraint(true, true);
    private static final SensitiveVaultExpressionContstraint DISALLOWS = new SensitiveVaultExpressionContstraint(false, true);

    private SensitiveVaultExpressionContstraint(boolean sensitive) {
        super(ControlFlag.REQUIRED, sensitive);
    }

    private SensitiveVaultExpressionContstraint(boolean allowsSensitive, boolean allowsNonSensitive) {
        super(ControlFlag.REQUIRED, allowsSensitive, allowsNonSensitive);
    }

    @Override
    protected int internalCompare(AbstractConstraint other) {
        // We have no preference
        return 0;
    }

    private static class Factory implements ConstraintFactory {

        @Override
        public Constraint getStandardUserConstraint(StandardRole role, Action.ActionEffect actionEffect) {
            if (role == StandardRole.ADMINISTRATOR
                    || role == StandardRole.SUPERUSER
                    || (role == StandardRole.AUDITOR
                    && actionEffect != Action.ActionEffect.WRITE_CONFIG
                    && actionEffect != Action.ActionEffect.WRITE_RUNTIME)) {
                return ALLOWS;
            }
            return DISALLOWS;
        }

        @Override
        public Constraint getRequiredConstraint(Action.ActionEffect actionEffect, Action action, TargetAttribute target) {
            return (VaultExpressionSensitivityConfig.INSTANCE.isSensitive(actionEffect) && isSensitiveAttribute(target)) ? SENSITIVE : NOT_SENSITIVE;
        }

        @Override
        public Constraint getRequiredConstraint(Action.ActionEffect actionEffect, Action action, TargetResource target) {
            return (VaultExpressionSensitivityConfig.INSTANCE.isSensitive(actionEffect) &&
                    (isSensitiveAction(action, actionEffect) || isSensitiveResource(target))) ? SENSITIVE : NOT_SENSITIVE;
        }

        private boolean isSensitiveAction(Action action, Action.ActionEffect actionEffect) {
            if (actionEffect == Action.ActionEffect.WRITE_RUNTIME || actionEffect == Action.ActionEffect.WRITE_CONFIG) {
                ModelNode operation = action.getOperation();
                // TODO check for vault expressions
            }
            return false;
        }

        private boolean isSensitiveAttribute(TargetAttribute target) {
            ModelNode currentValue = target.getCurrentValue();
            // TODO check for vault expressions
            throw new UnsupportedOperationException("implement me");
        }

        private boolean isSensitiveResource(TargetResource target) {
            ModelNode model = target.getResource().getModel();
            // TODO check for vault expressions
            throw new UnsupportedOperationException("implement me");
        }
    }
}
