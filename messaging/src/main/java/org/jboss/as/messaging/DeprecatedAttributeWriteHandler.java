package org.jboss.as.messaging;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;

public final class DeprecatedAttributeWriteHandler implements OperationStepHandler {

    private String attributeName;

    public DeprecatedAttributeWriteHandler(String attributeName) {
       this.attributeName = attributeName;
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        PathAddress pa = PathAddress.pathAddress(operation.require(ModelDescriptionConstants.OP_ADDR));
        MessagingLogger.MESSAGING_LOGGER.deprecatedAttribute(attributeName, pa);
        context.stepCompleted();
    }
}