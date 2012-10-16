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
package org.jboss.as.host.controller.operations;


import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.host.controller.HostControllerMessages.MESSAGES;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelVersion;
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
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.host.controller.ServerInventory;
import org.jboss.as.host.controller.descriptions.HostResolver;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Starts a server.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class ServerStartHandler implements OperationStepHandler {

    public static final String OPERATION_NAME = "start";

    private static final AttributeDefinition SERVER = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.SERVER, ModelType.STRING, true)
        .setValidator(new StringLengthValidator(1, true))
        .setDeprecated(ModelVersion.create(1, 0, 4)) //TODO This has never been used since 7.0.0, not sure which version to set deprecated
        .build();

    private static final AttributeDefinition BLOCKING = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.BLOCKING, ModelType.BOOLEAN, true)
        .build();

    public static final OperationDefinition DEFINITION = getOperationDefinition(OPERATION_NAME);

    private final ServerInventory serverInventory;

    static final OperationDefinition getOperationDefinition(String name) {
        return new SimpleOperationDefinitionBuilder(name, HostResolver.getResolver("host.server"))
            .setParameters(SERVER, BLOCKING)
            .setReplyType(ModelType.STRING)
            .withFlag(OperationEntry.Flag.HOST_CONTROLLER_ONLY)
            .build();
    }

    /**
     * Create the ServerAddHandler
     */
    public ServerStartHandler(final ServerInventory serverInventory) {
        this.serverInventory = serverInventory;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {

        if (context.getRunningMode() == RunningMode.ADMIN_ONLY) {
            throw new OperationFailedException(new ModelNode(MESSAGES.cannotStartServersInvalidMode(context.getRunningMode())));
        }

        final PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
        final PathElement element = address.getLastElement();
        final String serverName = element.getValue();
        final boolean blocking = operation.get("blocking").asBoolean(false);

        final ModelNode model = Resource.Tools.readModel(context.readResourceFromRoot(PathAddress.EMPTY_ADDRESS, true));
        context.addStep(new OperationStepHandler() {
            @Override
            public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                final ServerStatus origStatus = serverInventory.determineServerStatus(serverName);
                if (origStatus != ServerStatus.STARTED && origStatus != ServerStatus.STARTING) {
                    final ServerStatus status = serverInventory.startServer(serverName, model, blocking);
                    context.getResult().set(status.toString());
                } else {
                    context.getResult().set(origStatus.toString());
                }

                context.completeStep(new OperationContext.RollbackHandler() {
                    @Override
                    public void handleRollback(OperationContext context, ModelNode operation) {
                        if (origStatus != ServerStatus.STARTED && origStatus != ServerStatus.STARTING) {
                            serverInventory.stopServer(serverName, -1);
                        }
                    }
                });
            }
        }, OperationContext.Stage.RUNTIME);

        context.stepCompleted();
    }
}
