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

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.notification.Notification;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.host.controller.ServerInventory;
import org.jboss.as.host.controller.descriptions.HostResolver;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.host.controller.HostControllerMessages.MESSAGES;
import static org.jboss.as.host.controller.resources.ServerConfigResourceDefinition.SERVER_DESTROYED_NOTIFICATION;
import static org.jboss.as.host.controller.resources.ServerConfigResourceDefinition.SERVER_KILLED_NOTIFICATION;

/**
 * @author Emanuel Muckenhuber
 */
public abstract class ServerProcessHandlers implements OperationStepHandler {

    public static final OperationDefinition DESTROY_OPERATION = new SimpleOperationDefinitionBuilder("destroy", HostResolver.getResolver("host.server"))
            .setReplyType(ModelType.UNDEFINED)
            .setRuntimeOnly()
            .withFlag(OperationEntry.Flag.HOST_CONTROLLER_ONLY)
            .build();

    public static final OperationDefinition KILL_OPERATION = new SimpleOperationDefinitionBuilder("kill", HostResolver.getResolver("host.server"))
            .setReplyType(ModelType.UNDEFINED)
            .setRuntimeOnly()
            .withFlag(OperationEntry.Flag.HOST_CONTROLLER_ONLY)
            .build();

    protected final ServerInventory serverInventory;
    public ServerProcessHandlers(ServerInventory serverInventory) {
        this.serverInventory = serverInventory;
    }

    @Override
    public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {

        final PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
        final PathElement element = address.getLastElement();
        final String serverName = element.getValue();
        context.addStep(new OperationStepHandler() {
            @Override
            public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                // WFLY-2189 trigger a write-runtime authz check
                context.getServiceRegistry(true);

                doExecute(context, address, serverName);
                context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
            }
        }, OperationContext.Stage.RUNTIME);

        context.stepCompleted();
    }

    abstract void doExecute(OperationContext context, PathAddress address, String serverName);

    public static class ServerDestroyHandler extends ServerProcessHandlers {

        public ServerDestroyHandler(ServerInventory serverInventory) {
            super(serverInventory);
        }

        @Override
        void doExecute(OperationContext context, PathAddress address, String serverName) {
            serverInventory.destroyServer(serverName);
            context.emit(new Notification(SERVER_DESTROYED_NOTIFICATION, address.toModelNode(), MESSAGES.serverHasBeenDestroyed()));
        }

    }

    public static class ServerKillHandler extends ServerProcessHandlers {

        public ServerKillHandler(ServerInventory serverInventory) {
            super(serverInventory);
        }

        @Override
        void doExecute(OperationContext context, PathAddress address, String serverName) {
            serverInventory.killServer(serverName);
            context.emit(new Notification(SERVER_KILLED_NOTIFICATION, address.toModelNode(), MESSAGES.serverHasBeenKilled()));
        }

    }

}
