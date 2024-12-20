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

import static org.jboss.as.controller.PathAddress.EMPTY_ADDRESS;
import static org.wildfly.extension.picketlink.logging.PicketLinkLogger.ROOT_LOGGER;

/**
 * @author Pedro Igor
 */
public abstract class UniqueTypeValidationStepHandler implements ModelValidationStepHandler {

    private final ModelElement element;

    public UniqueTypeValidationStepHandler(ModelElement element) {
        this.element = element;
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        validateType(context, operation);
    }

    protected void validateType(OperationContext context, ModelNode operation) throws OperationFailedException {
        PathAddress pathAddress = context.getCurrentAddress();
        String elementName = context.getCurrentAddressValue();
        ModelNode typeNode = context.readResource(EMPTY_ADDRESS, false).getModel();
        String type = getType(context, typeNode);
        PathAddress parentAddress = pathAddress.getParent();
        Set<Resource.ResourceEntry> children = context.readResourceFromRoot(parentAddress, true).getChildren(this.element.getName());

        for (Resource.ResourceEntry child : children) {
            String existingResourceName = child.getName();
            String existingType = getType(context, child.getModel());

            if (!elementName.equals(existingResourceName) && (type.equals(existingType))) {
                throw ROOT_LOGGER.typeAlreadyDefined(type);
            }
        }
    }

    protected abstract String getType(OperationContext context, ModelNode model) throws OperationFailedException;
}
