/*
 *
 *  * JBoss, Home of Professional Open Source.
 *  * Copyright 2013, Red Hat, Inc., and individual contributors
 *  * as indicated by the @author tags. See the copyright.txt file in the
 *  * distribution for a full listing of individual contributors.
 *  *
 *  * This is free software; you can redistribute it and/or modify it
 *  * under the terms of the GNU Lesser General Public License as
 *  * published by the Free Software Foundation; either version 2.1 of
 *  * the License, or (at your option) any later version.
 *  *
 *  * This software is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  * Lesser General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU Lesser General Public
 *  * License along with this software; if not, write to the Free
 *  * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 *  * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 *
 */

package org.jboss.as.server.operations;

import org.jboss.as.controller.ControlledProcessState;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.RunningModeControl;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.server.DomainServerCommunicationServices;
import org.jboss.as.server.controller.descriptions.ServerDescriptions;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceName;

/**
 * Custom reload handler updating the operation-id before reconnecting to the HC.
 *
 * @author Emanuel Muckenhuber
 */
public class ServerDomainProcessReloadHandler extends ServerProcessReloadHandler {

    public static final OperationDefinition DOMAIN_DEFINITION = new SimpleOperationDefinitionBuilder(OPERATION_NAME, ServerDescriptions.getResourceDescriptionResolver("server"))
            .setPrivateEntry() // For now
            .withFlags(OperationEntry.Flag.HOST_CONTROLLER_ONLY, OperationEntry.Flag.RUNTIME_ONLY)
            .build();

    private final DomainServerCommunicationServices.OperationIDUpdater operationIDUpdater;
    public ServerDomainProcessReloadHandler(ServiceName rootService, RunningModeControl runningModeControl, ControlledProcessState processState,
                                            final DomainServerCommunicationServices.OperationIDUpdater operationIDUpdater) {
        super(rootService, runningModeControl, processState);
        this.operationIDUpdater = operationIDUpdater;
    }

    @Override
    public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
        context.acquireControllerLock();
        // Update the operation permit
        final int permit = operation.require("operation-id").asInt();
        operationIDUpdater.updateOperationID(permit);

        super.execute(context, operation);
    }
}
