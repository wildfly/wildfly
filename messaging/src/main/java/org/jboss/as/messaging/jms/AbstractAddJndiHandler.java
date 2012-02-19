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

package org.jboss.as.messaging.jms;

import org.hornetq.core.server.HornetQServer;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.validation.ParametersValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.messaging.CommonAttributes;
import org.jboss.as.messaging.MessagingServices;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;

import static org.jboss.as.messaging.MessagingMessages.MESSAGES;

/**
 * Base class for handlers that handle an "add-jndi" operation.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public abstract class AbstractAddJndiHandler implements OperationStepHandler, DescriptionProvider {

    public static final String ADD_JNDI = "add-jndi";

    private final ParametersValidator validator = new ParametersValidator();

    protected AbstractAddJndiHandler() {
        validator.registerValidator(CommonAttributes.JNDI_BINDING, new StringLengthValidator(1));
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {

        validator.validate(operation);
        String jndiName = operation.require(CommonAttributes.JNDI_BINDING).asString();
        final ModelNode entries = context.readResourceForUpdate(PathAddress.EMPTY_ADDRESS).getModel().get(CommonAttributes.ENTRIES.getName());
        for (ModelNode entry : entries.asList()) {
            if (jndiName.equals(entry.asString())) {
                throw new OperationFailedException(new ModelNode().set(MESSAGES.jndiNameAlreadyRegistered(jndiName)));
            }
        }
        entries.add(jndiName);


        if (context.isNormalServer()) {
            context.addStep(new OperationStepHandler() {
                @Override
                public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {


                    final ServiceName hqServiceName = MessagingServices.getHornetQServiceName(PathAddress.pathAddress(operation.get(ModelDescriptionConstants.OP_ADDR)));
                    ServiceController<?> hqService = context.getServiceRegistry(false).getService(hqServiceName);
                    if (hqService != null) {
                        HornetQServer hqServer = HornetQServer.class.cast(hqService.getValue());
                        String resourceName = PathAddress.pathAddress(operation.require(ModelDescriptionConstants.OP_ADDR)).getLastElement().getValue();
                        String jndiName = operation.require(CommonAttributes.JNDI_BINDING).asString();
                        addJndiNameToControl(jndiName, resourceName, hqServer, context);
                    } // else the subsystem isn't started yet

                    if (!context.hasFailureDescription()) {
                        context.getResult();
                    }

                    if (context.completeStep() != OperationContext.ResultAction.KEEP) {
                        // TODO is it possible to revert?
                    }
                }
            }, OperationContext.Stage.RUNTIME);
        }
        context.completeStep();
    }

    public void registerOperation(final ManagementResourceRegistration registration) {
        registration.registerOperationHandler(ADD_JNDI, this, this);
    }

    protected abstract void addJndiNameToControl(String toAdd, String resourceName, HornetQServer server, OperationContext context);
}
