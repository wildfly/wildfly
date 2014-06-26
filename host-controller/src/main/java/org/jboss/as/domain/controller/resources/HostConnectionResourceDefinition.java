/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

package org.jboss.as.domain.controller.resources;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST_CONNECTION;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ObjectListAttributeDefinition;
import org.jboss.as.controller.ObjectTypeAttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.domain.controller.HostConnectionInfo;
import org.jboss.as.domain.controller.HostRegistrations;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author Emanuel Muckenhuber
 */
public class HostConnectionResourceDefinition extends SimpleResourceDefinition {

    public static final PathElement PATH = PathElement.pathElement(HOST_CONNECTION);

    private static final ResourceDescriptionResolver RESOLVER = DomainResolver.getResolver(HOST_CONNECTION, false);

    private static final AttributeDefinition CONNECTION_DEF = SimpleAttributeDefinitionBuilder.create(HostConnectionInfo.CONNECTED, ModelType.BOOLEAN, false)
            .setStorageRuntime()
            .build();

    private static final AttributeDefinition TIMESTAMP_DEF = SimpleAttributeDefinitionBuilder.create(HostConnectionInfo.TIMESTAMP, ModelType.LONG, false)
            .setStorageRuntime()
            .build();

    private static final AttributeDefinition EVENT_TYPE_DEF = SimpleAttributeDefinitionBuilder.create(HostConnectionInfo.TYPE, ModelType.STRING, false)
            .setStorageRuntime()
            .build();

    private static final AttributeDefinition ADDRESS_DEF = SimpleAttributeDefinitionBuilder.create(HostConnectionInfo.ADDRESS, ModelType.STRING, false)
            .setStorageRuntime()
            .build();

    private static final ObjectTypeAttributeDefinition EVENT = ObjectTypeAttributeDefinition.Builder.of("event", EVENT_TYPE_DEF, ADDRESS_DEF, TIMESTAMP_DEF)
            .setStorageRuntime()
            .build();

    private static final ObjectListAttributeDefinition EVENT_LIST = ObjectListAttributeDefinition.Builder.of(HostConnectionInfo.EVENTS, EVENT)
            .setStorageRuntime()
            .build();

    private static final OperationDefinition PRUNE_EXPIRED_DEF = new SimpleOperationDefinitionBuilder("prune-expired", RESOLVER)
            .withFlag(OperationEntry.Flag.MASTER_HOST_CONTROLLER_ONLY)
            .build();

    private static final OperationDefinition PRUNE_DISCONNECTED_DEF = new SimpleOperationDefinitionBuilder("prune-disconnected", RESOLVER)
            .withFlag(OperationEntry.Flag.MASTER_HOST_CONTROLLER_ONLY)
            .build();

    private final HostRegistrations slaveHosts;
    private final OperationStepHandler attributeReadHandler = new AttributeReadHandler();

    public HostConnectionResourceDefinition(final HostRegistrations slaveHosts) {
        super(PATH, RESOLVER, null, null);
        this.slaveHosts = slaveHosts;
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerReadOnlyAttribute(CONNECTION_DEF, attributeReadHandler);
        resourceRegistration.registerReadOnlyAttribute(EVENT_LIST, attributeReadHandler);
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {

        resourceRegistration.registerOperationHandler(PRUNE_EXPIRED_DEF, new OperationStepHandler() {
            @Override
            public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                slaveHosts.pruneExpired();
                context.stepCompleted();
            }
        });
        resourceRegistration.registerOperationHandler(PRUNE_DISCONNECTED_DEF, new OperationStepHandler() {
            @Override
            public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                slaveHosts.pruneDisconnected();
                context.stepCompleted();
            }
        });

    }

    private class AttributeReadHandler implements OperationStepHandler {

        @Override
        public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
            final PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
            final String operationName = operation.require(NAME).asString();
            final String hostName = address.getLastElement().getValue();
            final HostConnectionInfo info = slaveHosts.getHostInfo(hostName);
            if (info != null) {
                final ModelNode result = context.getResult();
                switch (operationName) {
                    case HostConnectionInfo.CONNECTED:
                        result.set(info.isConnected());
                        break;
                    case HostConnectionInfo.EVENTS:
                        processEvents(info, result.setEmptyList());
                        break;
                }
            }
            context.stepCompleted();
        }
    }

    static void processEvents(final HostConnectionInfo info, final ModelNode list) {
        for (final HostConnectionInfo.Event event : info.getEvents()) {
            event.toModelNode(list.add());
        }
    }

}
