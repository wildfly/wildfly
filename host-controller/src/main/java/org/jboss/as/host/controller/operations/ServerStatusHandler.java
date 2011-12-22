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

package org.jboss.as.host.controller.operations;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.AUTO_START;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.host.controller.HostControllerMessages.MESSAGES;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.client.helpers.domain.ServerStatus;
import org.jboss.as.host.controller.ServerInventory;
import org.jboss.dmr.ModelNode;

/**
 * {@code OperationHandler} determining the status of a server.
 *
 * @author Emanuel Muckenhuber
 */
public class ServerStatusHandler implements OperationStepHandler {

    public static final String ATTRIBUTE_NAME = "status";

    private final ServerInventory serverInventory;

    /**
     * Create the ServerAddHandler
     */
    public ServerStatusHandler(final ServerInventory serverInventory) {
        this.serverInventory = serverInventory;
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {

        final PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
        final PathElement element = address.getLastElement();
        final String serverName = element.getValue();

        final ModelNode subModel = context.readModel(PathAddress.EMPTY_ADDRESS);
        final boolean isStart;
        if(subModel.hasDefined(AUTO_START)) {
            isStart = subModel.get(AUTO_START).asBoolean();
        } else {
            isStart = true;
        }

        ServerStatus status = serverInventory.determineServerStatus(serverName);

        if (status == ServerStatus.STOPPED) {
            status = isStart ? status : ServerStatus.DISABLED;
        }

        if(status != null) {
            context.getResult().set(status.toString());
            context.completeStep();
        } else {
            throw new OperationFailedException(new ModelNode().set(MESSAGES.failedToGetServerStatus()));
        }
    }

}
