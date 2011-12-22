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

package org.jboss.as.domain.controller.plan;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.domain.controller.DomainControllerMessages.MESSAGES;

import java.util.Set;

import org.jboss.as.domain.controller.ServerIdentity;
import org.jboss.dmr.ModelNode;

/**
 * Policy used to determine whether a server can be updated, based on the result
 * of updates made to other servers.
 *
 * @author Brian Stansberry
 */
class ServerUpdatePolicy {
    private final ConcurrentGroupServerUpdatePolicy parent;
    private final String serverGroupName;
    private final Set<ServerIdentity> servers;
    private int successCount;
    private int failureCount;
    private final int maxFailed;

    /**
     * Constructor for normal case where the max number of failures before
     * plan is considered failed comes from the plan.
     *
     * @param parent parent policy
     * @param serverGroupName the name of the server group being updated
     * @param servers servers that are being updated
     * @param maxFailures maximum number of failed servers before the server group should be rolled back
     */
    ServerUpdatePolicy(final ConcurrentGroupServerUpdatePolicy parent,
                            final String serverGroupName,
                            final Set<ServerIdentity> servers,
                            final int maxFailures) {
        assert parent != null : "parent is null";
        assert serverGroupName != null : "serverGroupName is null";
        assert servers != null : "servers is null";

        this.parent = parent;
        this.serverGroupName = serverGroupName;
        this.servers = servers;
        this.maxFailed = maxFailures;
    }

    /**
     * Constructor for the rollback case where failure on one server should
     * not prevent execution on the others.
     *
     * @param parent parent policy
     * @param serverGroupName the name of the server group being updated
     * @param servers servers that are being updated
     */
    ServerUpdatePolicy(final ConcurrentGroupServerUpdatePolicy parent,
                       final String serverGroupName,
                       final Set<ServerIdentity> servers) {
        assert parent != null : "parent is null";
        assert serverGroupName != null : "serverGroupName is null";
        assert servers != null : "servers is null";

        this.parent = parent;
        this.serverGroupName = serverGroupName;
        this.servers = servers;
        this.maxFailed = servers.size();
    }

    /**
     * Gets the name of the server group to which this policy is scoped.
     *
     * @return the name of the server group. Will not be <code>null</code>
     */
    public String getServerGroupName() {
        return serverGroupName;
    }

    /**
     * Gets whether the given server can be updated.
     *
     * @param server the id of the server. Cannot be <code>null</code>
     *
     * @return <code>true</code> if the server can be updated; <code>false</code>
     *          if the update should be cancelled
     *
     * @throws IllegalStateException if this policy is not expecting a request
     *           to update the given server
     */
    public boolean canUpdateServer(ServerIdentity server) {
        if (!serverGroupName.equals(server.getServerGroupName()) || !servers.contains(server)) {
            throw MESSAGES.unknownServer(server);
        }

        if (!parent.canChildProceed())
            return false;

        synchronized (this) {
            return failureCount <= maxFailed;
        }
    }

    /**
     * Records the result of updating a server.
     *
     * @param server  the id of the server. Cannot be <code>null</code>
     * @param response the result of the updates
     */
    public void recordServerResult(ServerIdentity server, ModelNode response) {

        if (!serverGroupName.equals(server.getServerGroupName()) || !servers.contains(server)) {
            throw MESSAGES.unknownServer(server);
        }

        boolean serverFailed = response.has(FAILURE_DESCRIPTION);

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

    /**
     * Gets whether the
     * {@link #recordServerResult(org.jboss.as.domain.controller.ServerIdentity, org.jboss.dmr.ModelNode)} recorded results}
     * constitute a failed server group update per this policy.
     *
     * @return <code>true</code> if the server group update is considered to be a failure;
     *         <code>false</code> otherwise
     */
    public synchronized boolean isFailed() {
        return failureCount > maxFailed;
    }
}
