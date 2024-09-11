/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.controller.validation;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.operations.validation.ModelTypeValidator;
import org.jboss.as.controller.operations.validation.ParameterValidator;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author Paul Ferraro
 */
public class ModuleIdentifierValidator extends ModelTypeValidator {
    public static final ParameterValidator INSTANCE = new ModuleIdentifierValidator();

    ModuleIdentifierValidator() {
        super(ModelType.STRING);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void validateParameter(String parameterName, ModelNode value) throws OperationFailedException {
        super.validateParameter(parameterName, value);
        if (value.isDefined()) {
            String module = value.asString();
            try {
                org.jboss.modules.ModuleIdentifier.fromString(module);
            } catch (IllegalArgumentException e) {
                throw new OperationFailedException(e.getMessage() + ": " + module, e);
            }
        }
    }
}
