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

package org.jboss.as.domain.controller.operations;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.JVM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT_SUBSYSTEM_ENDPOINT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROFILE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING_PORT_OFFSET;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SYSTEM_PROPERTY;
import static org.jboss.as.domain.controller.DomainControllerMessages.MESSAGES;

import java.util.Locale;
import java.util.NoSuchElementException;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.operations.validation.ModelTypeValidator;
import org.jboss.as.controller.operations.validation.ParametersValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.domain.controller.descriptions.ServerGroupDescription;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author Emanuel Muckenhuber
 */
public class ServerGroupAddHandler implements OperationStepHandler, DescriptionProvider {

    public static final ServerGroupAddHandler INSTANCE = new ServerGroupAddHandler();

    private final ParametersValidator validator = new ParametersValidator();

    private ServerGroupAddHandler() {
        validator.registerValidator(PROFILE, new StringLengthValidator(1));
        validator.registerValidator(SOCKET_BINDING_GROUP, new StringLengthValidator(1, Integer.MAX_VALUE, true, false));
        validator.registerValidator(SOCKET_BINDING_PORT_OFFSET, new ModelTypeValidator(ModelType.INT, true, false));
        validator.registerValidator(JVM, new StringLengthValidator(1, Integer.MAX_VALUE, true, false));
        validator.registerValidator(MANAGEMENT_SUBSYSTEM_ENDPOINT, new ModelTypeValidator(true, false, true, ModelType.BOOLEAN));
    }

    /** {@inheritDoc */
    public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {

        validator.validate(operation);

        final Resource resource = context.createResource(PathAddress.EMPTY_ADDRESS);
        final ModelNode model = resource.getModel();

        String profile = operation.require(PROFILE).asString();

        try {
            context.readResourceFromRoot(PathAddress.pathAddress(PathElement.pathElement(PROFILE, profile)));
        } catch (NoSuchElementException e) {
            throw new OperationFailedException(new ModelNode().set(MESSAGES.unknown(PROFILE, profile)));
        }
        model.get(PROFILE).set(profile);

        if (operation.hasDefined(SOCKET_BINDING_GROUP)) {
            String socketBindingGroup =  operation.get(SOCKET_BINDING_GROUP).asString();

            try {
                context.readResourceFromRoot(PathAddress.pathAddress(PathElement.pathElement(SOCKET_BINDING_GROUP, socketBindingGroup)));
            } catch (NoSuchElementException e) {
                throw new OperationFailedException(new ModelNode().set(MESSAGES.unknown(SOCKET_BINDING_GROUP, socketBindingGroup)));
            }
            model.get(SOCKET_BINDING_GROUP).set(socketBindingGroup);
        }

        if (operation.hasDefined(SOCKET_BINDING_PORT_OFFSET)) {
            model.get(SOCKET_BINDING_PORT_OFFSET).set(operation.get(SOCKET_BINDING_PORT_OFFSET));
        }

        if (operation.hasDefined(JVM)) {
            model.get(JVM).set(operation.get(JVM).asString(), new ModelNode());
        } else {
            model.get(JVM);
        }
        if (operation.hasDefined(MANAGEMENT_SUBSYSTEM_ENDPOINT)) {
            model.get(MANAGEMENT_SUBSYSTEM_ENDPOINT).set(operation.get(MANAGEMENT_SUBSYSTEM_ENDPOINT));
        }

        model.get(SYSTEM_PROPERTY);
        model.get(DEPLOYMENT);

        context.completeStep();
    }

    protected boolean requiresRuntime(OperationContext context) {
        return false;
    }

    @Override
    public ModelNode getModelDescription(Locale locale) {
        return ServerGroupDescription.getServerGroupAdd(locale);
    }

}
