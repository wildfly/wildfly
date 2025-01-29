/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller.validation;

import org.jboss.as.controller.ModuleIdentifierUtil;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.operations.validation.ModelTypeValidator;
import org.jboss.as.controller.operations.validation.ParameterValidator;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * {@link ParameterValidatorBuilder} that builds a validator that validates that a given value is a valid module
 * name + optional slot that can be {@link ModuleIdentifierUtil#canonicalModuleIdentifier(String) canonicalized}.
 *
 * @author Paul Ferraro
 */
public class ModuleIdentifierValidatorBuilder extends AbstractParameterValidatorBuilder {

    @Override
    public ParameterValidator build() {
        return new ModuleIdentifierValidator(this.allowsUndefined, this.allowsExpressions);
    }

    private static class ModuleIdentifierValidator extends ModelTypeValidator {

        ModuleIdentifierValidator(boolean allowsUndefined, boolean allowsExpression) {
            super(ModelType.STRING, allowsUndefined, allowsExpression);
        }

        @SuppressWarnings("deprecation")
        @Override
        public void validateParameter(String parameterName, ModelNode value) throws OperationFailedException {
            super.validateParameter(parameterName, value);
            if (value.isDefined()) {
                String module = value.asString();
                try {
                    ModuleIdentifierUtil.canonicalModuleIdentifier(module);
                } catch (IllegalArgumentException e) {
                    throw new OperationFailedException(e.getMessage() + ": " + module, e);
                }
            }
        }
    }
}
