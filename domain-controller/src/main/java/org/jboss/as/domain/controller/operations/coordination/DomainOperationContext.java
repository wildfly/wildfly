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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.jboss.as.domain.controller.LocalHostControllerInfo;
import org.jboss.as.domain.controller.ServerIdentity;
import org.jboss.dmr.ModelNode;

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
    private boolean completeRollback = true;
    private boolean failureReported;

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
        return ok == null ? true : ok.booleanValue();
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
}
