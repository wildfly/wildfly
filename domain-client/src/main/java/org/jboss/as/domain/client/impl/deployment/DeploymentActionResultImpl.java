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

package org.jboss.as.domain.client.impl.deployment;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.jboss.as.domain.client.api.ServerIdentity;
import org.jboss.as.domain.client.api.ServerUpdateResult;
import org.jboss.as.domain.client.api.deployment.DeploymentAction;
import org.jboss.as.domain.client.api.deployment.DeploymentActionResult;
import org.jboss.as.domain.client.api.deployment.ServerGroupDeploymentActionResult;
import org.jboss.as.domain.client.impl.DomainUpdateApplierResponse;
import org.jboss.as.model.UpdateFailedException;

/**
 * Default implementation of {@link DeploymentActionResult}.
 *
 * @author Brian Stansberry
 */
class DeploymentActionResultImpl implements DeploymentActionResult {

    private final DeploymentAction deploymentAction;
    private final DomainUpdateApplierResponse applierResponse;
    private final Map<String, ServerGroupDeploymentActionResult> serverResults = new HashMap<String, ServerGroupDeploymentActionResult>();
    private boolean rolledBack;

    DeploymentActionResultImpl(final DeploymentAction deploymentAction,
                               final DomainUpdateApplierResponse applierResponse) {
        assert deploymentAction != null : "deploymentAction is null";
        assert applierResponse != null : "applierResponse is null";
        this.deploymentAction = deploymentAction;
        this.applierResponse = applierResponse;
    }

    @Override
    public DeploymentAction getDeploymentAction() {
        return deploymentAction;
    }

    @Override
    public UUID getDeploymentActionId() {
        return deploymentAction.getId();
    }

    @Override
    public UpdateFailedException getDomainControllerFailure() {
        return applierResponse.getDomainFailure();
    }

    @Override
    public Map<String, ServerGroupDeploymentActionResult> getResultsByServerGroup() {
        return Collections.unmodifiableMap(serverResults);
    }

    @Override
    public Map<String, UpdateFailedException> getServerManagerFailures() {
        return applierResponse.getHostFailures();
    }

    @Override
    public boolean isCancelledByDomain() {
        return applierResponse.isCancelled();
    }

    @Override
    public boolean isRolledBackOnDomain() {
        return rolledBack ? true : applierResponse.isRolledBack();
    }

    void markRolledBack() {
        rolledBack = true;
    }

    void storeServerUpdateResult(ServerIdentity server, ServerUpdateResult<Void> result) {
        ServerGroupDeploymentActionResultImpl sgdar = (ServerGroupDeploymentActionResultImpl) serverResults.get(server.getServerGroupName());
        if (sgdar == null) {
            sgdar = new ServerGroupDeploymentActionResultImpl(server.getServerGroupName());
            serverResults.put(server.getServerGroupName(), sgdar);
        }
        sgdar.storeServerResult(server.getServerName(), result);
    }
}
