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

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;

import static org.jboss.as.controller.PathAddress.EMPTY_ADDRESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.wildfly.extension.picketlink.logging.PicketLinkLogger.ROOT_LOGGER;

/**
 * @author Pedro Igor
 */
public class AlternativeAttributeValidationStepHandler implements ModelValidationStepHandler {

    private final AttributeDefinition[] attributes;
    private final boolean required;

    public AlternativeAttributeValidationStepHandler(AttributeDefinition[] attributes) {
        this(attributes, true);
    }

    public AlternativeAttributeValidationStepHandler(AttributeDefinition[] attributes, boolean required) {
        this.attributes = attributes;
        this.required = required;
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        validateAlternatives(context, operation);
    }

    protected void validateAlternatives(OperationContext context, ModelNode operation) throws OperationFailedException {
        ModelNode elementNode = context.readResource(EMPTY_ADDRESS, false).getModel();
        PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
        ModelNode definedAttribute = null;

        for (AttributeDefinition attribute : this.attributes) {
            if (elementNode.hasDefined(attribute.getName())) {
                if (definedAttribute != null) {
                    throw ROOT_LOGGER.invalidAlternativeAttributeOccurrence(attribute.getName(), address.getLastElement().toString(), getAttributeNames());
                }

                definedAttribute = attribute.resolveModelAttribute(context, elementNode);
            }
        }

        if (this.required && definedAttribute == null) {
            throw ROOT_LOGGER.requiredAlternativeAttributes(address.getLastElement().toString(), getAttributeNames());
        }
    }

    private String getAttributeNames() {
        StringBuilder builder = new StringBuilder();

        for (AttributeDefinition alternativeAttribute : this.attributes) {
            if (builder.length() > 0) {
                builder.append(", ");
            }

            builder.append(alternativeAttribute.getName());
        }

        return builder.toString();
    }
}
