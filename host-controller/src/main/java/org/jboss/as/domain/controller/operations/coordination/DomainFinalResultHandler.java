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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.COMPOSITE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DOMAIN_FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DOMAIN_RESULTS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST_FAILURE_DESCRIPTIONS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESPONSE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_GROUPS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STEPS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import static org.jboss.as.domain.controller.DomainControllerMessages.MESSAGES;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.domain.controller.ServerIdentity;
import org.jboss.dmr.ModelNode;

/**
 * Assembles the overall result for a domain operation from individual host and server results.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class DomainFinalResultHandler implements OperationStepHandler {

    private final DomainOperationContext domainOperationContext;

    public DomainFinalResultHandler(DomainOperationContext domainOperationContext) {
        this.domainOperationContext = domainOperationContext;
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {

        context.completeStep();

        // On the way out, fix up the response
        final boolean isDomain = isDomainOperation(operation);
        boolean shouldContinue = collectDomainFailure(context, isDomain);
        shouldContinue = shouldContinue && collectContextFailure(context, isDomain);
        shouldContinue = shouldContinue && collectHostFailures(context, isDomain);
        if(shouldContinue){
            ModelNode contextResult = context.getResult();
            contextResult.setEmptyObject(); // clear out any old data
            contextResult.set(getDomainResults(operation));
            if (domainOperationContext.getServerResults().size() > 0) {
                populateServerGroupResults(context, context.getResult());
            } else {
                // Just make sure there's an 'undefined' server-groups node
                context.getServerResults();
            }
        }
    }

    private boolean collectDomainFailure(OperationContext context, final boolean isDomain) {
        final ModelNode coordinator = domainOperationContext.getCoordinatorResult();
        ModelNode domainFailure = null;
        if (isDomain &&  coordinator != null && coordinator.has(FAILURE_DESCRIPTION)) {
            domainFailure = coordinator.hasDefined(FAILURE_DESCRIPTION) ? coordinator.get(FAILURE_DESCRIPTION) : new ModelNode(MESSAGES.unexplainedFailure());
        }
        if (domainFailure != null) {
            context.getFailureDescription().get(DOMAIN_FAILURE_DESCRIPTION).set(domainFailure);
            return false;
        }
        return true;
    }

    private boolean collectContextFailure(OperationContext context, final boolean isDomain) {
        // We ignore a context failure description if the request failed on all servers, as the
        // DomainRolloutStepHandler would have had to set that to trigger model rollback
        // but we still want to record the server results so the user can see the problem
        if (!domainOperationContext.isFailureReported() && context.hasFailureDescription()) {
            ModelNode formattedFailure = new ModelNode();
            if (isDomain) {
                ModelNode failure = context.getFailureDescription();
                if (failure.isDefined())
                    formattedFailure.get(DOMAIN_FAILURE_DESCRIPTION).set(failure);
                else
                    formattedFailure.get(DOMAIN_FAILURE_DESCRIPTION).set(MESSAGES.unexplainedFailure());
            } else {
                ModelNode hostFailureProperty = new ModelNode();
                ModelNode contextFailure = context.getFailureDescription();
                ModelNode hostFailure = contextFailure.isDefined() ? contextFailure : new ModelNode().set(MESSAGES.unexplainedFailure());
                hostFailureProperty.add(domainOperationContext.getLocalHostInfo().getLocalHostName(), hostFailure);

                formattedFailure.get(HOST_FAILURE_DESCRIPTIONS).set(hostFailureProperty);
            }

            context.getFailureDescription().set(formattedFailure);

            return false;
        }
        return true;
    }

    private boolean collectHostFailures(final OperationContext context, final boolean isDomain) {
        ModelNode hostFailureResults = null;
        for (Map.Entry<String, ModelNode> entry : domainOperationContext.getHostControllerResults().entrySet()) {
            ModelNode hostResult = entry.getValue();
            if (hostResult.has(FAILURE_DESCRIPTION)) {
                if (hostFailureResults == null) {
                    hostFailureResults = new ModelNode();
                }
                final ModelNode desc = hostResult.hasDefined(FAILURE_DESCRIPTION) ? hostResult.get(FAILURE_DESCRIPTION) : new ModelNode().set(MESSAGES.unexplainedFailure());
                hostFailureResults.add(entry.getKey(), desc);
            }
        }

        final ModelNode coordinator = domainOperationContext.getCoordinatorResult();
        if (!isDomain && coordinator != null && coordinator.has(FAILURE_DESCRIPTION)) {
            if (hostFailureResults == null) {
                hostFailureResults = new ModelNode();
            }
            final ModelNode desc = coordinator.hasDefined(FAILURE_DESCRIPTION) ? coordinator.get(FAILURE_DESCRIPTION) : new ModelNode().set(MESSAGES.unexplainedFailure());
            hostFailureResults.add(domainOperationContext.getLocalHostInfo().getLocalHostName(), desc);
        }

        if (hostFailureResults != null) {
            context.getFailureDescription().get(HOST_FAILURE_DESCRIPTIONS).set(hostFailureResults);
            return false;
        }
        return true;
    }

    private ModelNode getDomainResults(final ModelNode operation, final String... stepLabels) {
        ResponseProvider provider = new ResponseProvider(operation, domainOperationContext.getLocalHostInfo().getLocalHostName());
        ModelNode result;
        if (!provider.isLeaf()) {
            result = new ModelNode();
            String[] nextStepLabels = new String[stepLabels.length + 1];
            System.arraycopy(stepLabels, 0, nextStepLabels, 0, stepLabels.length);
            int i = 1;
            for (ModelNode step : provider.getChildren()) {
                String childStepLabel = "step-" + i++;
                nextStepLabels[stepLabels.length] = childStepLabel;
                result.get(childStepLabel).set(getDomainResults(step, nextStepLabels));
            }
        } else if (provider.getServer() == null) {
            String hostName = provider.getHost();
            boolean forMaster = hostName.equals(domainOperationContext.getLocalHostInfo().getLocalHostName());
            ModelNode hostResponse = forMaster ? domainOperationContext.getCoordinatorResult()
                    : domainOperationContext.getHostControllerResults().get(hostName);
                result = getHostControllerResult(hostResponse, stepLabels);
        } else {
            result = domainOperationContext.getServerResult(provider.getHost(), provider.getServer(), stepLabels);
        }

        return result == null ? new ModelNode() : result;
    }

    private ModelNode getHostControllerResult(final ModelNode fullResult, final String... stepLabels) {
        ModelNode result = null;
        if (fullResult != null && fullResult.hasDefined(RESULT) && fullResult.get(RESULT).hasDefined(DOMAIN_RESULTS)) {
            ModelNode domainResults = fullResult.get(RESULT, DOMAIN_RESULTS);
            result = domainResults.get(stepLabels);
            if (result.has(OUTCOME) && !result.hasDefined(OUTCOME)) {
                if (result.hasDefined(FAILURE_DESCRIPTION)) {
                    result.get(OUTCOME).set(FAILED);
                } else {
                    result.get(OUTCOME).set(SUCCESS);
                }
            }
        }
        return result;
    }

    private void populateServerGroupResults(final OperationContext context, final ModelNode result) {
        final Set<String> groupNames = new TreeSet<String>();
        final Map<String, Set<HostServer>> groupToServerMap = new HashMap<String, Set<HostServer>>();
        for (Map.Entry<ServerIdentity, ModelNode> entry : domainOperationContext.getServerResults().entrySet()) {
            final String serverGroup = entry.getKey().getServerGroupName();
            groupNames.add(serverGroup);
            final String hostName = entry.getKey().getHostName();
            final String serverName = entry.getKey().getServerName();
            if (!groupToServerMap.containsKey(serverGroup)) {
                groupToServerMap.put(serverGroup, new TreeSet<HostServer>());
            }
            groupToServerMap.get(serverGroup).add(new HostServer(hostName, serverName, entry.getValue()));
        }

        boolean serverGroupSuccess = false;
        for (String groupName : groupNames) {
            final ModelNode groupNode = new ModelNode();
            if (domainOperationContext.isServerGroupRollback(groupName)) {
                // TODO revisit if we should report this for the whole group, since the result might not be accurate
                // groupNode.get(ROLLED_BACK).set(true);
            } else {
                serverGroupSuccess = true;
            }
            for (HostServer hostServer : groupToServerMap.get(groupName)) {
                final ModelNode serverNode = new ModelNode();
                serverNode.get(HOST).set(hostServer.hostName);
                serverNode.get(RESPONSE).set(hostServer.result);
                groupNode.get(hostServer.serverName).set(serverNode);
            }
            context.getServerResults().get(groupName).set(groupNode);
            // TODO AS7-3677
            result.get(SERVER_GROUPS, groupName).set(groupNode);
        }
        if(!serverGroupSuccess) {
            // TODO see if we can extract more information from the server details
            context.getFailureDescription().set(MESSAGES.operationFailedOrRolledBack());
        }
    }

    private boolean isDomainOperation(final ModelNode operation) {
        final PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
        return address.size() == 0 || !address.getElement(0).getKey().equals(HOST);
    }

    private static class HostServer implements Comparable<HostServer> {
        private final String hostName;
        private final String serverName;
        private final ModelNode result;

        private HostServer(String hostName, String serverName, ModelNode result) {
            this.hostName = hostName;
            this.serverName = serverName;
            this.result = result;
        }

        public int compareTo(HostServer hostServer) {
            int hostCompare = hostName.compareTo(hostServer.hostName);
            if (hostCompare != 0) {
                return hostCompare;
            }
            return serverName.compareTo(hostServer.serverName);
        }
    }

    private static class ResponseProvider {
        private final String host;
        private final String server;
        private final List<ModelNode> children;

        private ResponseProvider(final ModelNode operation, final String localHostName) {

            boolean composite = COMPOSITE.equals(operation.require(OP).asString());
            PathAddress opAddr = PathAddress.pathAddress(operation.get(OP_ADDR));
            int addrSize = opAddr.size();
            if (addrSize == 0) {
                host = localHostName;
                server = null;
            } else if (HOST.equals(opAddr.getElement(0).getKey())) {
                host = opAddr.getElement(0).getValue();
                if (addrSize > 1 && SERVER.equals(opAddr.getElement(1).getKey())) {
                    server =  opAddr.getElement(1).getValue();
                    composite = composite && addrSize == 2;
                } else {
                    server = null;
                }
            } else {
                // A domain op
                host = localHostName;
                server = null;
                composite = false;
            }

            if (composite) {
                if (operation.hasDefined(STEPS)) {
                    children = new ArrayList<ModelNode>(operation.require(STEPS).asList());
                } else {
                    // This shouldn't be possible
                    children = Collections.emptyList();
                }
            } else {
                children = null;
            }
        }

        private String getHost() {
            return host;
        }

        private String getServer() {
            return server;
        }

        private List<ModelNode> getChildren() {
            return children;
        }

        private boolean isLeaf() {
            return children == null;
        }
    }
}
