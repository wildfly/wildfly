/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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
