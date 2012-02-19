/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PATH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RELATIVE_TO;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;

import java.util.EnumSet;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;


/**
 * @author Emanuel Muckenhuber
 */
class MessagingPathHandlers {

    static final AttributeDefinition[] ATTRIBUTES = new AttributeDefinition[] { CommonAttributes.PATH, CommonAttributes.RELATIVE_TO };
    static final OperationStepHandler PATH_ADD = new OperationStepHandler() {

        @Override
        public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
            final Resource resource = context.createResource(PathAddress.EMPTY_ADDRESS);
            final ModelNode model = resource.getModel();
            for(final AttributeDefinition def : ATTRIBUTES) {
                def.validateAndSet(operation, model);
            }
            reloadRequiredStep(context);
            context.completeStep();
        }
    };

    static final OperationStepHandler PATH_ATTR = new ReloadRequiredWriteAttributeHandler(CommonAttributes.RELATIVE_TO, CommonAttributes.PATH);

    static final OperationStepHandler PATH_REMOVE = new OperationStepHandler() {

        @Override
        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            final Resource resource = context.removeResource(PathAddress.EMPTY_ADDRESS);
            reloadRequiredStep(context);
            context.completeStep();
        }
    };

    static void register(final ManagementResourceRegistration registration) {
        registration.registerOperationHandler(ADD, PATH_ADD, MessagingSubsystemProviders.PATH_ADD);
        registration.registerOperationHandler(REMOVE, PATH_REMOVE, MessagingSubsystemProviders.PATH_REMOVE);
        for(final AttributeDefinition def : ATTRIBUTES) {
            registration.registerReadWriteAttribute(def.getName(), null, PATH_ATTR, EnumSet.of(AttributeAccess.Flag.RESTART_ALL_SERVICES));
        }
    }

    static ModelNode createAddOperation(final ModelNode address, final ModelNode subModel) {
        final ModelNode operation = new ModelNode();
        operation.get(OP).set(ADD);
        operation.get(OP_ADDR).set(address);
        operation.get(RELATIVE_TO).set(subModel.get(RELATIVE_TO));
        operation.get(PATH).set(subModel.require(PATH));
        return operation;
    }

    static void reloadRequiredStep(final OperationContext context) {
        if(context.isNormalServer()) {
            context.addStep(new OperationStepHandler() {
                @Override
                public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
                    final ServiceName hqServiceName = MessagingServices.getHornetQServiceName(PathAddress.pathAddress(operation.get(ModelDescriptionConstants.OP_ADDR)));
                    final ServiceController<?> controller = context.getServiceRegistry(false).getService(hqServiceName);
                    if(controller != null) {
                        context.reloadRequired();
                    }
                    context.completeStep();
                }
            }, OperationContext.Stage.RUNTIME);
        }
    }

}
