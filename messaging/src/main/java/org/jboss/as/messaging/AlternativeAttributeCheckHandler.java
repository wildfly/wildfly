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

import static org.jboss.as.messaging.MessagingMessages.MESSAGES;

import java.util.HashMap;
import java.util.Map;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.Resource;
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
        final String attributeName = operation.get(ModelDescriptionConstants.NAME).asString();
        if (attributeDefinitions.containsKey(attributeName)) {
            AttributeDefinition attr = attributeDefinitions.get(attributeName);
            final Resource resource = context.readResource(PathAddress.EMPTY_ADDRESS);
            if(attr.hasAlternative(resource.getModel())) {
                context.setRollbackOnly();
                throw new OperationFailedException(new ModelNode().set(MESSAGES.altAttributeAlreadyDefined(attributeName)));
            }
        }
        context.stepCompleted();
    }

    public static void checkAlternatives(ModelNode operation, String attr1, String attr2, boolean acceptNone) throws OperationFailedException {
        boolean hasAttr1 = operation.hasDefined(attr1);
        boolean hasAttr2 = operation.hasDefined(attr2);
        if (!hasAttr1 && !hasAttr2 && !acceptNone) {
            throw new OperationFailedException(new ModelNode().set(MESSAGES.invalidOperationParameters(attr1, attr2)));
        } else if (hasAttr1 && hasAttr2) {
            throw new OperationFailedException(new ModelNode().set(MESSAGES.cannotIncludeOperationParameters(attr1, attr2)));
        }
    }
}
