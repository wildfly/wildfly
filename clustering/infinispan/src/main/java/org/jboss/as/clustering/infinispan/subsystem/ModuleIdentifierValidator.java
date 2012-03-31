package org.jboss.as.clustering.infinispan.subsystem;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.operations.validation.ModelTypeValidator;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.modules.ModuleIdentifier;

public class ModuleIdentifierValidator extends ModelTypeValidator {

    public ModuleIdentifierValidator(boolean nullable) {
        super(ModelType.STRING, nullable, false);
    }

    @Override
    public void validateParameter(String parameterName, ModelNode value) throws OperationFailedException {
        super.validateParameter(parameterName, value);
        if (value.isDefined()) {
            String module = value.asString();
            try {
                ModuleIdentifier.fromString(module);
            } catch (IllegalArgumentException e) {
                throw new OperationFailedException(e.getMessage() + ": " + module, e);
            }
        }
    }
}
