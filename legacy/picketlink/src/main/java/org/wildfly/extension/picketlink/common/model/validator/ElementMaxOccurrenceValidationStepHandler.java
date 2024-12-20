/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.picketlink.common.model.validator;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.wildfly.extension.picketlink.logging.PicketLinkLogger.ROOT_LOGGER;

import java.util.Set;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.dmr.ModelNode;
import org.wildfly.extension.picketlink.common.model.ModelElement;

/**
 * @author Pedro Igor
 */
public class ElementMaxOccurrenceValidationStepHandler implements ModelValidationStepHandler {

    private final int maxOccurs;
    private final ModelElement parentElement;
    private final ModelElement element;

    public ElementMaxOccurrenceValidationStepHandler(ModelElement element, ModelElement parentElement, int maxOccurs) {
        this.element = element;
        this.parentElement = parentElement;
        this.maxOccurs = maxOccurs;
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        validateOccurrence(context, operation);
    }

    protected void validateOccurrence(OperationContext context, ModelNode operation) throws OperationFailedException {
        PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
        PathAddress parentAddress = Util.getParentAddressByKey(address, this.parentElement.getName());
        Set<String> elements = context.readResourceFromRoot(parentAddress, false).getChildrenNames(this.element.getName());

        if (elements.size() > this.maxOccurs) {
            throw ROOT_LOGGER.invalidChildTypeOccurrence(parentAddress.getLastElement().toString(), this.maxOccurs, this.element
                .getName());
        }
    }
}
