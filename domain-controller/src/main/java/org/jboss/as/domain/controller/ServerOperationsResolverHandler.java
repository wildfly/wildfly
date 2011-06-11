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

package org.jboss.as.domain.controller;


import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.*;

import java.util.Map;
import java.util.Set;

import org.jboss.as.controller.NewOperationContext;
import org.jboss.as.controller.NewStepHandler;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;

/**
 * Adds to the response the server-level operations needed to effect the given domain/host operation on the
 * servers controlled by this host.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class ServerOperationsResolverHandler implements NewStepHandler {

    final String TO_RESOLVE = "to-resolve";

    private final String localHostName;
    private final ServerOperationResolver resolver;

    ServerOperationsResolverHandler(final String localHostName, final ServerOperationResolver resolver) {
        this.localHostName = localHostName;
        this.resolver = resolver;
    }

    @Override
    public void execute(NewOperationContext context, ModelNode operation) throws OperationFailedException {

        final ModelNode domainModel = context.readModel(PathAddress.EMPTY_ADDRESS);
        ModelNode baseOp = operation.get(TO_RESOLVE);
        Map<Set<ServerIdentity>, ModelNode> serverOps = resolver.getServerOperations(baseOp, PathAddress.pathAddress(baseOp.get(OP_ADDR)), domainModel, domainModel.get(HOST).get(localHostName));
        final ModelNode overallResult = context.getResult();
        ModelNode serverOpsNode = overallResult.get(RESULT, SERVER_OPERATIONS);
        for (Map.Entry<Set<ServerIdentity>, ModelNode> entry : serverOps.entrySet()) {
            ModelNode setNode = serverOpsNode.add();
            ModelNode serverNode = setNode.get("servers");
            serverNode.setEmptyList();
            for (ServerIdentity server : entry.getKey()) {
                serverNode.add(server.getServerName(), server.getServerGroupName());
            }
            setNode.get(OP).set(entry.getValue());
        }
        context.completeStep();
    }
}
