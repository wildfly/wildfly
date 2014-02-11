/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.as.host.controller.operations;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.host.controller.HostControllerMessages.MESSAGES;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.RunningMode;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.client.helpers.domain.ServerStatus;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.host.controller.ServerInventory;
import org.jboss.as.host.controller.descriptions.HostResolver;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Reloads a managed server.
 *
 * @author Emanuel Muckenhuber
 */
public class ServerReloadHandler implements OperationStepHandler {

    public static final String OPERATION_NAME = "reload";

    private static final AttributeDefinition BLOCKING = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.BLOCKING, ModelType.BOOLEAN, true)
            .setDefaultValue(new ModelNode(false))
            .build();

    private static final AttributeDefinition IF_REQUIRED = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.IF_REQUIRED, ModelType.BOOLEAN, true)
            .setDefaultValue(new ModelNode(false))
            .build();

    public static final OperationDefinition DEFINITION = getOperationDefinition(OPERATION_NAME);

    static OperationDefinition getOperationDefinition(String name) {
        return new SimpleOperationDefinitionBuilder(name, HostResolver.getResolver("host.server"))
                .setParameters(BLOCKING, IF_REQUIRED)
                .setReplyType(ModelType.STRING)
                .setRuntimeOnly()
                .withFlag(OperationEntry.Flag.HOST_CONTROLLER_ONLY)
                .build();
    }

    private final ServerInventory serverInventory;
    public ServerReloadHandler(ServerInventory serverInventory) {
        this.serverInventory = serverInventory;
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {

        if (context.getRunningMode() == RunningMode.ADMIN_ONLY) {
            throw new OperationFailedException(new ModelNode(MESSAGES.cannotStartServersInvalidMode(context.getRunningMode())));
        }

        final PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
        final PathElement element = address.getLastElement();
        final String serverName = element.getValue();
        final boolean blocking = BLOCKING.resolveModelAttribute(context, operation).asBoolean();
        final boolean ifRequired = IF_REQUIRED.resolveModelAttribute(context, operation).asBoolean();

        context.addStep(new OperationStepHandler() {
            @Override
            public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
                // WFLY-2189 trigger a write-runtime authz check
                context.getServiceRegistry(true);

                final ServerStatus status = serverInventory.reloadServer(serverName, blocking, ifRequired);
                context.getResult().set(status.toString());
                context.stepCompleted();
            }
        }, OperationContext.Stage.RUNTIME);

        context.stepCompleted();
    }
}
