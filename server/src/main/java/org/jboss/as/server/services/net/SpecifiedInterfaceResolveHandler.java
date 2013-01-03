/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.jboss.as.server.services.net;

import static org.jboss.as.controller.ControllerMessages.MESSAGES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INTERFACE;

import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.EnumSet;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.SimpleOperationDefinition;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.descriptions.common.ControllerResolver;
import org.jboss.as.controller.interfaces.ParsedInterfaceCriteria;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.controller.resource.InterfaceDefinition;
import org.jboss.as.network.NetworkInterfaceBinding;
import org.jboss.as.server.ServerMessages;
import org.jboss.as.server.controller.descriptions.ServerDescriptions;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Handler that resolves an interface criteria to actual IP addresses in order to allow clients to check the validity
 * of the configuration.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class SpecifiedInterfaceResolveHandler implements OperationStepHandler {

    static final AttributeDefinition[] ATTRIBUTES = InterfaceDefinition.ROOT_ATTRIBUTES;
    private static final String OPERATION_NAME = "resolve-internet-address";

    public static final SimpleOperationDefinition DEFINITION = new SimpleOperationDefinitionBuilder(OPERATION_NAME,
            ServerDescriptions.getResourceDescriptionResolver(INTERFACE))
            .setParameters(ATTRIBUTES)
            .setReplyType(ModelType.STRING)
            .setAttributeResolver(ControllerResolver.getResolver(INTERFACE))
            .withFlags(EnumSet.of(OperationEntry.Flag.RUNTIME_ONLY))
            .build();

    public static final SpecifiedInterfaceResolveHandler INSTANCE = new SpecifiedInterfaceResolveHandler();

    private SpecifiedInterfaceResolveHandler() {
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {

        final ModelNode config = new ModelNode();

        for(final AttributeDefinition definition : ATTRIBUTES) {
            validateAndSet(definition, operation, config);
        }

        ParsedInterfaceCriteria parsed = ParsedInterfaceCriteria.parse(config, true, context);
        if (parsed.getFailureMessage() != null) {
            throw new OperationFailedException(new ModelNode().set(parsed.getFailureMessage()));
        }

        try {
            NetworkInterfaceBinding nib = NetworkInterfaceService.createBinding(parsed);
            context.getResult().set(nib.getAddress().getHostAddress());
        } catch (SocketException e) {
            throw ServerMessages.MESSAGES.cannotResolveInterface(e, e);
        } catch (UnknownHostException e) {
            throw ServerMessages.MESSAGES.cannotResolveInterface(e, e);
        }

        context.stepCompleted();
    }

    private void validateAndSet(final AttributeDefinition definition, final ModelNode operation, final ModelNode subModel) throws OperationFailedException {
        final String attributeName = definition.getName();
        final boolean has = operation.has(attributeName);
        if(! has && definition.isRequired(operation)) {
            throw new OperationFailedException(new ModelNode(MESSAGES.required(attributeName)));
        }
        if(has) {
            if(! definition.isAllowed(operation)) {
                throw new OperationFailedException(new ModelNode(MESSAGES.invalid(attributeName)));
            }
            definition.validateAndSet(operation, subModel);
        } else {
            // create the undefined node
            subModel.get(definition.getName());
        }
    }

}
