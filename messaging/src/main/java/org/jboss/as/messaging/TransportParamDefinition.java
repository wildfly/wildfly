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

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;

/**
 * Transport param resource definition.
 *
 * @author <a href="http://jmesnil.net">Jeff Mesnil</a> (c) 2012 Red Hat Inc.
 */
public class TransportParamDefinition extends SimpleResourceDefinition {

    static final SimpleAttributeDefinition VALUE = create("value", STRING)
            // FIXME AS7-5121 transport parameter parsing and configuration must be refactored
            // prior to allow expressions for the param's value
            .setAllowExpression(false)
            .setRestartAllServices()
            .build();

    static final OperationStepHandler PARAM_ADD = new OperationStepHandler() {
        @Override
        public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
            final Resource resource = context.createResource(PathAddress.EMPTY_ADDRESS);
            VALUE.validateAndSet(operation, resource.getModel());
            TransportConfigOperationHandlers.reloadRequiredStep(context);
            context.stepCompleted();
        }
    };

    static final OperationStepHandler PARAM_ATTR = new OperationStepHandler() {
        @Override
        public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
            final Resource resource = context.readResourceForUpdate(PathAddress.EMPTY_ADDRESS);
            VALUE.validateAndSet(operation, resource.getModel());
            TransportConfigOperationHandlers.reloadRequiredStep(context);
            context.stepCompleted();
        }
    };

    public TransportParamDefinition() {
        super(PathElement.pathElement(CommonAttributes.PARAM),
                MessagingExtension.getResourceDescriptionResolver("transport-config." + CommonAttributes.PARAM),
                PARAM_ADD,
                TransportConfigOperationHandlers.REMOVE);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration registry) {
        super.registerAttributes(registry);

        registry.registerReadWriteAttribute(VALUE, null, PARAM_ATTR);
    }

}
