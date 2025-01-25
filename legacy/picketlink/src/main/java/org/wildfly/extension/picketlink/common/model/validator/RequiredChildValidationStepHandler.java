/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.picketlink.common.model.validator;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.wildfly.extension.picketlink.common.model.ModelElement;

import java.util.Set;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.registry.Resource.ResourceEntry;
import static org.wildfly.extension.picketlink.logging.PicketLinkLogger.ROOT_LOGGER;

/**
 * @author Pedro Igor
 */
public class RequiredChildValidationStepHandler implements ModelValidationStepHandler {

    private final ModelElement childElement;

    public RequiredChildValidationStepHandler(ModelElement childElement) {
        this.childElement = childElement;
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        validateRequiredChild(context, operation);
    }

    protected void validateRequiredChild(OperationContext context, ModelNode operation) throws OperationFailedException {
        Resource resource = context.readResource(PathAddress.EMPTY_ADDRESS);
        PathAddress pathAddress = PathAddress.pathAddress(operation.get(OP_ADDR));
        Set<ResourceEntry> children = resource.getChildren(this.childElement.getName());

        if (children.isEmpty()) {
            throw ROOT_LOGGER.requiredChild(pathAddress.getLastElement().toString(), this.childElement.getName());
        }
    }
}
