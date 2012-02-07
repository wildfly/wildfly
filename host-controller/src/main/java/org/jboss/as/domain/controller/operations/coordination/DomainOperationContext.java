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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_OPERATIONS;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.jboss.as.domain.controller.LocalHostControllerInfo;
import org.jboss.as.domain.controller.ServerIdentity;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;

/**
 * Stores overall contextual information for an operation executing on the domain.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class DomainOperationContext {

    private final LocalHostControllerInfo localHostInfo;
    private final ModelNode coordinatorResult = new ModelNode();
    private final ConcurrentMap<String, ModelNode> hostControllerResults = new ConcurrentHashMap<String, ModelNode>();
    private final ConcurrentMap<ServerIdentity, ModelNode> serverResults = new ConcurrentHashMap<ServerIdentity, ModelNode>();

    private final Map<String, Boolean> serverGroupStatuses = new ConcurrentHashMap<String, Boolean>();
    private volatile boolean completeRollback = true;
    private volatile boolean failureReported;

    public DomainOperationContext(final LocalHostControllerInfo localHostInfo) {
        this.localHostInfo = localHostInfo;
    }

    public LocalHostControllerInfo getLocalHostInfo() {
        return localHostInfo;
    }

    public ModelNode getCoordinatorResult() {
        return coordinatorResult;
    }

    public Map<String, ModelNode> getHostControllerResults() {
        return new HashMap<String, ModelNode>(hostControllerResults);
    }

    public void addHostControllerResult(String hostId, ModelNode hostResult) {
        hostControllerResults.put(hostId, hostResult);
    }

    public Map<ServerIdentity, ModelNode> getServerResults() {
        return new HashMap<ServerIdentity, ModelNode>(serverResults);
    }

    public void addServerResult(ServerIdentity serverId, ModelNode serverResult) {
        serverResults.put(serverId, serverResult);
    }

    public boolean isCompleteRollback() {
        return completeRollback;
    }

    public void setCompleteRollback(boolean completeRollback) {
        this.completeRollback = completeRollback;
    }

    public boolean isServerGroupRollback(String serverGroup) {
        Boolean ok = serverGroupStatuses.get(serverGroup);
        return ok == null || ok.booleanValue();
    }

    public void setServerGroupRollback(String serverGroup, boolean rollback) {
        serverGroupStatuses.put(serverGroup, Boolean.valueOf(rollback));
    }

    public boolean hasHostLevelFailures() {
        boolean domainFailed = coordinatorResult.isDefined() && coordinatorResult.has(FAILURE_DESCRIPTION);
        if (domainFailed) {
            return true;
        }
        for (ModelNode hostResult : hostControllerResults.values()) {
            if (hostResult.has(FAILURE_DESCRIPTION)) {
                return true;
            }
        }
        return false;
    }

    public boolean isFailureReported() {
        return failureReported;
    }

    public void setFailureReported(boolean failureReported) {
        this.failureReported = failureReported;
    }

    public ModelNode getServerResult(String hostName, String serverName, String... stepLabels) {
        ModelNode result;
        ServerIdentity id = new ServerIdentity(hostName, null, serverName);
        ModelNode serverResult = getServerResults().get(id).clone();
        if (stepLabels.length == 0) {
            result = serverResult;
        } else {
            result = new ModelNode();
            ModelNode hostResults;
            if (hostName.equals(localHostInfo.getLocalHostName())) {
                hostResults = coordinatorResult;
            } else {
                hostResults = hostControllerResults.get(hostName);
            }
            String[] translatedSteps = getTranslatedSteps(serverName, hostResults, stepLabels);
            if (translatedSteps != null && serverResult.hasDefined(RESULT)) {
                result.set(serverResult.get(RESULT).get(translatedSteps));
            }
        }
        return result;
    }

    private String[] getTranslatedSteps(String serverName, ModelNode hostResults, String[] stepLabels) {
        String[] result = null;
        ModelNode domainMappedOp = getDomainMappedOperation(serverName, hostResults);
        if (domainMappedOp != null) {
            result = new String[stepLabels.length];
            ModelNode level = domainMappedOp;
            for (int i = 0; i < stepLabels.length; i++) {
                String translated = getTranslatedStepIndex(stepLabels[i], level);
                if (translated == null) {
                    return null;
                }
                result[i] = translated;
                level = level.get(stepLabels[i]);
            }
        }
        return result;
    }

    private String getTranslatedStepIndex(String stepLabel, ModelNode level) {
        int i = 1;
        for (String key : level.keys()) {
            if (stepLabel.equals(key)) {
                return "step-" + i;
            }
            i++;
        }
        return null;
    }

    private ModelNode getDomainMappedOperation(String serverName, ModelNode hostResults) {
        for (ModelNode set : hostResults.get(RESULT, SERVER_OPERATIONS).asList()) {
            for (Property prop : set.get(SERVERS).asPropertyList()) {
                if (prop.getName().equals(serverName)) {
                    return set.get(OP);
                }
            }
        }
        return null;
    }
}
