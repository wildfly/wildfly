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

package org.jboss.as.server.operations;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.server.controller.descriptions.ServerDescriptions;
import org.jboss.as.server.controller.resources.ServerRootResourceDefinition;
import org.jboss.dmr.ModelNode;

/**
 * Handler for operation run by Host Controller during boot to pass in the server group and host name.
 *
 * @author Brian Stansberry (c) 2013 Red Hat Inc.
 */
public class SetServerGroupHostHandler implements OperationStepHandler {

    public static final String OPERATION_NAME = "set-server-group-host";

    public static final OperationDefinition DEFINITION =
            new SimpleOperationDefinitionBuilder(OPERATION_NAME, ServerDescriptions.getResourceDescriptionResolver("server"))
                    .setParameters(ServerRootResourceDefinition.SERVER_GROUP, ServerRootResourceDefinition.HOST)
                    .setPrivateEntry()
                    .build();

    public static final SetServerGroupHostHandler INSTANCE = new SetServerGroupHostHandler();

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        if (!context.isBooting()) {
            throw new IllegalStateException();
        }

        ModelNode model = context.readResourceForUpdate(PathAddress.EMPTY_ADDRESS).getModel();

        ServerRootResourceDefinition.SERVER_GROUP.validateAndSet(operation, model);
        ServerRootResourceDefinition.HOST.validateAndSet(operation, model);
        context.stepCompleted();
    }
}
