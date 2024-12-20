/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.picketlink.common.model.validator;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.wildfly.extension.picketlink.logging.PicketLinkLogger.ROOT_LOGGER;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;

/**
 * @author Pedro Igor
 */
public class NotEmptyResourceValidationStepHandler implements ModelValidationStepHandler {

    public static final NotEmptyResourceValidationStepHandler INSTANCE = new NotEmptyResourceValidationStepHandler();

    private NotEmptyResourceValidationStepHandler() {
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        validateChildren(context, operation);
    }

    protected void validateChildren(OperationContext context, ModelNode operation) throws OperationFailedException {
        Resource resource = context.readResource(PathAddress.EMPTY_ADDRESS);
        PathAddress pathAddress = PathAddress.pathAddress(operation.get(OP_ADDR));

        if (resource.getChildTypes().isEmpty()) {
            throw ROOT_LOGGER.emptyResource(pathAddress.getLastElement().toString());
        }
    }
}
