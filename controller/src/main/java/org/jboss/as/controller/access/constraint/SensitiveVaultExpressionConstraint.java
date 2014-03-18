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

import java.util.regex.Pattern;

import org.jboss.as.controller.ExpressionResolver;
import org.jboss.as.controller.access.Action;
import org.jboss.as.controller.access.TargetAttribute;
import org.jboss.as.controller.access.TargetResource;
import org.jboss.as.controller.access.rbac.StandardRole;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;

/**
 * {@link Constraint} related to whether an attribute is considered security sensitive
 * because it contains a vault expression.
 *
 * @author Brian Stansberry (c) 2013 Red Hat Inc.
 */
public class SensitiveVaultExpressionConstraint extends AllowAllowNotConstraint {

    public static final ConstraintFactory FACTORY = new Factory();

    private static final SensitiveVaultExpressionConstraint SENSITIVE = new SensitiveVaultExpressionConstraint(true);
    private static final SensitiveVaultExpressionConstraint NOT_SENSITIVE = new SensitiveVaultExpressionConstraint(false);
    private static final SensitiveVaultExpressionConstraint ALLOWS = new SensitiveVaultExpressionConstraint(true, true);
    private static final SensitiveVaultExpressionConstraint DISALLOWS = new SensitiveVaultExpressionConstraint(false, true);

    private SensitiveVaultExpressionConstraint(boolean sensitive) {
        super(sensitive);
    }

    private SensitiveVaultExpressionConstraint(boolean allowsSensitive, boolean allowsNonSensitive) {
        super(allowsSensitive, allowsNonSensitive);
    }

    private static class Factory extends AbstractConstraintFactory {

        private static final Pattern VAULT_PATTERN = Pattern.compile("VAULT::.*::.*::.*");

        @Override
        public Constraint getStandardUserConstraint(StandardRole role, Action.ActionEffect actionEffect) {
            if (role == StandardRole.ADMINISTRATOR
                    || role == StandardRole.SUPERUSER
                    || role == StandardRole.AUDITOR) {
                return ALLOWS;
            }
            return DISALLOWS;
        }

        @Override
        public Constraint getRequiredConstraint(Action.ActionEffect actionEffect, Action action, TargetAttribute target) {
            return isSensitiveAction(action, actionEffect, target) ? SENSITIVE : NOT_SENSITIVE;
        }

        @Override
        public Constraint getRequiredConstraint(Action.ActionEffect actionEffect, Action action, TargetResource target) {
            return isSensitiveAction(action, actionEffect) ? SENSITIVE : NOT_SENSITIVE;
        }

        private boolean isSensitiveAction(Action action, Action.ActionEffect actionEffect) {
            if (VaultExpressionSensitivityConfig.INSTANCE.isSensitive(actionEffect)) {
                if (actionEffect == Action.ActionEffect.WRITE_RUNTIME || actionEffect == Action.ActionEffect.WRITE_CONFIG) {
                    ModelNode operation = action.getOperation();
                    for (Property property : operation.asPropertyList()) {
                        if (isSensitiveValue(property.getValue())) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }

        private boolean isSensitiveAction(Action action, Action.ActionEffect actionEffect, TargetAttribute targetAttribute) {
            if (VaultExpressionSensitivityConfig.INSTANCE.isSensitive(actionEffect)) {
                if (actionEffect == Action.ActionEffect.WRITE_RUNTIME || actionEffect == Action.ActionEffect.WRITE_CONFIG) {
                    ModelNode operation = action.getOperation();
                    if (operation.hasDefined(targetAttribute.getAttributeName())) {
                        if (isSensitiveValue(operation.get(targetAttribute.getAttributeName()))) {
                            return true;
                        }
                    }
                    if (ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION.equals(operation.get(ModelDescriptionConstants.OP).asString())
                            && operation.hasDefined(ModelDescriptionConstants.VALUE)) {
                        if (isSensitiveValue(operation.get(ModelDescriptionConstants.VALUE))) {
                            return true;
                        }
                    }
                }
                if (actionEffect != Action.ActionEffect.ADDRESS) {
                    if (isSensitiveValue(targetAttribute.getCurrentValue())) {
                        return true;
                    }
                }
            }
            return false;
        }

        private boolean isSensitiveValue(ModelNode value) {
            if (value.getType() == ModelType.EXPRESSION
                    || value.getType() == ModelType.STRING) {
                String valueString = value.asString();
                if (ExpressionResolver.EXPRESSION_PATTERN.matcher(valueString).matches()) {
                    int start = valueString.indexOf("${") + 2;
                    int end = valueString.indexOf("}", start);
                    valueString = valueString.substring(start, end);
                    return VAULT_PATTERN.matcher(valueString).matches();
                }
            }
            return false;
        }

        @Override
        protected int internalCompare(AbstractConstraintFactory other) {
            // We have no preference
            return 0;
        }
    }
}

