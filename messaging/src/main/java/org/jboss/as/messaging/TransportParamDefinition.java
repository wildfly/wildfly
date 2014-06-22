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

import static org.jboss.as.controller.SimpleAttributeDefinitionBuilder.create;
import static org.jboss.dmr.ModelType.STRING;

import java.util.Set;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.messaging.logging.MessagingLogger;
import org.jboss.dmr.ModelNode;

/**
 * Transport param resource definition.
 *
 * @author <a href="http://jmesnil.net">Jeff Mesnil</a> (c) 2012 Red Hat Inc.
 */
public class TransportParamDefinition extends SimpleResourceDefinition {

    public static final PathElement PATH = PathElement.pathElement(CommonAttributes.PARAM);

    public static final SimpleAttributeDefinition VALUE = create("value", STRING)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    public TransportParamDefinition(final Set<String> allowedKeys) {
        super(PATH,
                MessagingExtension.getResourceDescriptionResolver("transport-config." + CommonAttributes.PARAM),
                new HornetQReloadRequiredHandlers.AddStepHandler(VALUE) {
                    @Override
                    protected void populateModel(OperationContext context, ModelNode operation, Resource resource) throws OperationFailedException {
                        super.populateModel(context, operation, resource);
                        context.addStep(new CheckParameterStep(allowedKeys), OperationContext.Stage.MODEL);
                    }
                },
                new HornetQReloadRequiredHandlers.RemoveStepHandler());
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration registry) {
        super.registerAttributes(registry);

        registry.registerReadWriteAttribute(VALUE, null, new HornetQReloadRequiredHandlers.WriteAttributeHandler(VALUE));
    }

    private static class CheckParameterStep implements OperationStepHandler {
        private final Set<String> allowedKeys;

        public CheckParameterStep(Set<String> allowedKeys) {
            this.allowedKeys = allowedKeys;
        }

        @Override
        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            String parameterName = PathAddress.pathAddress(operation.require(ModelDescriptionConstants.OP_ADDR)).getLastElement().getValue();

            // empty allowedKeys is possible for generic transport resources where the keys are not known.
            if (!allowedKeys.isEmpty() && !allowedKeys.contains(parameterName)) {
                throw MessagingLogger.ROOT_LOGGER.invalidParameterName(parameterName, allowedKeys);
            }

            context.stepCompleted();
        }
    }
}
