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

package org.jboss.as.domain.controller.operations.coordination;


import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DOMAIN_RESULTS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.IGNORED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_OPERATIONS;
import static org.jboss.as.domain.controller.DomainControllerLogger.HOST_CONTROLLER_LOGGER;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.domain.controller.ServerIdentity;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Adds to the localResponse the server-level operations needed to effect the given domain/host operation on the
 * servers controlled by this host.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class ServerOperationsResolverHandler implements OperationStepHandler {

    public static final String OPERATION_NAME = "server-operation-resolver";

    private final String localHostName;
    private final ServerOperationResolver resolver;
    private final ParsedOp parsedOp;
    private final PathAddress originalAddress;
    private final ImmutableManagementResourceRegistration originalRegistration;
    private final ModelNode localResponse;
    private final boolean recordResponse;

    ServerOperationsResolverHandler(final String localHostName, final ServerOperationResolver resolver, final ParsedOp parsedOp,
                                    final PathAddress originalAddress, final ImmutableManagementResourceRegistration originalRegistration,
                                    final ModelNode response, final boolean recordResponse) {
        this.localHostName = localHostName;
        this.resolver = resolver;
        this.parsedOp = parsedOp;
        this.originalAddress = originalAddress;
        this.originalRegistration = originalRegistration;
        this.localResponse = response;
        this.recordResponse = recordResponse;
    }

    @Override
    public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {

        // Read out what was added by any MODEL/RUNTIME/VERIFY handlers and publish it to the overall context
        if (context.hasResult()) {
            localResponse.get(RESULT).set(context.getResult());
        }
        if (context.hasFailureDescription()) {
            localResponse.get(FAILURE_DESCRIPTION).set(context.getFailureDescription());
            // We do not allow failures on the host controllers
            context.setRollbackOnly();
        } else {

            final ModelNode domainModel = Resource.Tools.readModel(context.getRootResource());
            ParsedOp.ServerOperationProvider provider = new ParsedOp.ServerOperationProvider() {

                @Override
                public Map<Set<ServerIdentity>, ModelNode> getServerOperations(ModelNode domainOp, PathAddress address) {
                    return ServerOperationsResolverHandler.this.getServerOperations(context, domainOp, address, domainModel, domainModel.get(HOST).get(localHostName));
                }
            };

            ModelNode localResult = localResponse.get(RESULT);
            localResponse.remove(RESULT);  // We're going to replace it
            ModelNode responseNode = recordResponse ? context.getResult() : localResponse.get(RESULT);
            if (ModelType.STRING == localResult.getType() && IGNORED.equals(localResult.asString())) {
                // Just pass the IGNORED along
                responseNode.set(localResult);
            } else {
                Map<Set<ServerIdentity>, ModelNode> serverOps = parsedOp.getServerOps(provider);
                createOverallResult(serverOps, localResult, responseNode);
            }

            if (HOST_CONTROLLER_LOGGER.isTraceEnabled()) {
                HOST_CONTROLLER_LOGGER.tracef("%s responseNode is %s",getClass().getSimpleName(), responseNode);
            }
        }

        context.completeStep();
    }

    private Map<Set<ServerIdentity>, ModelNode> getServerOperations(OperationContext context, ModelNode domainOp, PathAddress domainOpAddress, ModelNode domainModel, ModelNode hostModel) {
        Map<Set<ServerIdentity>, ModelNode> result = null;
        final PathAddress relativeAddress = domainOpAddress.subAddress(originalAddress.size());
        Set<OperationEntry.Flag> flags = originalRegistration.getOperationFlags(relativeAddress, domainOp.require(OP).asString());
        if (flags.contains(OperationEntry.Flag.READ_ONLY) && !flags.contains(OperationEntry.Flag.DOMAIN_PUSH_TO_SERVERS)) {
            result = Collections.emptyMap();
        }
        if (result == null) {
            result = resolver.getServerOperations(context, domainOp, domainOpAddress, domainModel, hostModel);
        }
        return result;
    }

    private void createOverallResult(Map<Set<ServerIdentity>, ModelNode> serverOps, final ModelNode localResult, final ModelNode overallResult) {
        ModelNode domainResult = parsedOp.getFormattedDomainResult(localResult);
        overallResult.get(DOMAIN_RESULTS).set(domainResult);
        ModelNode serverOpsNode = overallResult.get(SERVER_OPERATIONS);
        for (Map.Entry<Set<ServerIdentity>, ModelNode> entry : serverOps.entrySet()) {
            ModelNode setNode = serverOpsNode.add();
            ModelNode serverNode = setNode.get("servers");
            serverNode.setEmptyList();
            for (ServerIdentity server : entry.getKey()) {
                serverNode.add(server.getServerName(), server.getServerGroupName());
            }
            setNode.get(OP).set(entry.getValue());
        }
    }
}
