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

package org.jboss.as.messaging;

import static org.jboss.as.controller.PathAddress.EMPTY_ADDRESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;

import java.util.HashMap;
import java.util.Map;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.messaging.logging.MessagingLogger;
import org.jboss.dmr.ModelNode;

public final class AlternativeAttributeCheckHandler implements OperationStepHandler {

    private final Map<String, AttributeDefinition> attributeDefinitions;

    public AlternativeAttributeCheckHandler(final AttributeDefinition... definitions) {
        attributeDefinitions = new HashMap<String, AttributeDefinition>();
        for (AttributeDefinition def : definitions) {
            attributeDefinitions.put(def.getName(), def);
        }
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        final String operationName = operation.get(OP).asString();
        final String attributeName = operation.get(NAME).asString();
        // when an attribute is undefined, check that an alternative is present
        // otherwise when an attribute is written, check that there is no alternative
        boolean alternativeMustBeSet = ModelDescriptionConstants.UNDEFINE_ATTRIBUTE_OPERATION.equals(operationName);
        checkAlternativeAttribute(context, attributeName, alternativeMustBeSet);
    }

    private void checkAlternativeAttribute(OperationContext context, String attributeName, boolean alternativeMustBeSet) throws OperationFailedException {
        if (attributeDefinitions.containsKey(attributeName)) {
            AttributeDefinition attr = attributeDefinitions.get(attributeName);
            final Resource resource = context.readResource(EMPTY_ADDRESS);
            if (alternativeMustBeSet) {
                if (!attr.hasAlternative(resource.getModel())) {
                    throw new OperationFailedException(MessagingLogger.ROOT_LOGGER.undefineAttributeWithoutAlternative(attributeName));
                }
            } else {
                if (attr.hasAlternative(resource.getModel())) {
                    throw new OperationFailedException(MessagingLogger.ROOT_LOGGER.altAttributeAlreadyDefined(attributeName));
                }
            }
        }
    }

    public static void checkAlternatives(ModelNode operation, String attr1, String attr2, boolean acceptNone) throws OperationFailedException {
        boolean hasAttr1 = operation.hasDefined(attr1);
        boolean hasAttr2 = operation.hasDefined(attr2);
        if (!hasAttr1 && !hasAttr2 && !acceptNone) {
            throw new OperationFailedException(MessagingLogger.ROOT_LOGGER.invalidOperationParameters(attr1, attr2));
        } else if (hasAttr1 && hasAttr2) {
            throw new OperationFailedException(MessagingLogger.ROOT_LOGGER.cannotIncludeOperationParameters(attr1, attr2));
        }
    }
}
