/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

import java.util.List;
import java.util.Set;

import org.jboss.as.domain.client.api.ServerIdentity;
import org.jboss.as.domain.client.api.deployment.ServerGroupDeploymentPlan;
import org.jboss.as.model.UpdateResultHandlerResponse;

class ServerUpdatePolicy {
    private final ConcurrentGroupServerUpdatePolicy parent;
    private final String serverGroupName;
    private final Set<ServerIdentity> servers;
    private int successCount;
    private int failureCount;
    private final int maxFailed;

    ServerUpdatePolicy(final ConcurrentGroupServerUpdatePolicy parent,
                            final String serverGroupName,
                            final Set<ServerIdentity> servers,
                            final ServerGroupDeploymentPlan groupPlan) {
        assert parent != null : "parent is null";
        assert serverGroupName != null : "serverGroupName is null";
        assert servers != null : "servers is null";
        assert groupPlan != null : "groupPlan is null";

        this.parent = parent;
        this.serverGroupName = serverGroupName;
        this.servers = servers;
        if (groupPlan.getMaxServerFailurePercentage() > 0) {
            this.maxFailed = ((servers.size() * groupPlan.getMaxServerFailurePercentage()) / 100);
        }
        else {
            this.maxFailed = groupPlan.getMaxServerFailures();
        }
    }

    public String getServerGroupName() {
        return serverGroupName;
    }

    public boolean canUpdateServer(ServerIdentity server) {
        if (!serverGroupName.equals(server.getServerGroupName()) || !servers.contains(server)) {
            throw new IllegalStateException("Unknown server " + server);
        }

        if (!parent.canChildProceed())
            return false;

        synchronized (this) {
            return failureCount <= maxFailed;
        }
    }

    public void recordServerResult(ServerIdentity server, List<UpdateResultHandlerResponse<?>> responses) {

        if (!serverGroupName.equals(server.getServerGroupName()) || !servers.contains(server)) {
            throw new IllegalStateException("Unknown server " + server);
        }

        UpdateResultHandlerResponse<?> last = responses.size() == 0 ? null : responses.get(responses.size() - 1);

        boolean serverFailed = last != null && (last.isCancelled()
                        || last.isRolledBack() || last.isTimedOut()
                        || last.getFailureResult() != null);

        synchronized (this) {
            int previousFailed = failureCount;
            if (serverFailed) {
                failureCount++;
            }
            else {
                successCount++;
            }
            if (previousFailed <= maxFailed) {
                if (!serverFailed && (successCount + failureCount) == servers.size()) {
                    // All results are in; notify parent of success
                    parent.recordServerGroupResult(serverGroupName, false);
                }
                else if (serverFailed && failureCount > maxFailed) {
                    parent.recordServerGroupResult(serverGroupName, true);
                }
            }
        }
    }

    public synchronized boolean isFailed() {
        return failureCount > maxFailed;
    }
}
