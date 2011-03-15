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

package org.jboss.as.controller.client.helpers.domain.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.jboss.as.controller.client.helpers.domain.DeploymentAction;
import org.jboss.as.controller.client.helpers.domain.DeploymentActionResult;
import org.jboss.as.controller.client.helpers.domain.ServerGroupDeploymentActionResult;
import org.jboss.as.controller.client.helpers.domain.ServerIdentity;
import org.jboss.as.controller.client.helpers.domain.ServerUpdateResult;
import org.jboss.as.controller.client.helpers.domain.UpdateFailedException;

/**
 * Default implementation of {@link DeploymentActionResult}.
 *
 * @author Brian Stansberry
 */
class DeploymentActionResultImpl implements DeploymentActionResult {

    private final DeploymentAction deploymentAction;
    private final BasicDomainUpdateResult applierResponse;
    private final Map<String, ServerGroupDeploymentActionResult> serverResults = new HashMap<String, ServerGroupDeploymentActionResult>();
    private BasicDomainUpdateResult rollbackResponse;

    DeploymentActionResultImpl(final DeploymentAction deploymentAction,
                               final BasicDomainUpdateResult applierResponse) {
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
    public Map<String, UpdateFailedException> getHostControllerFailures() {
        return applierResponse.getHostFailures();
    }

    @Override
    public boolean isCancelledByDomain() {
        return applierResponse.isCancelled();
    }

    @Override
    public boolean isRolledBackOnDomain() {
        return rollbackResponse != null ? true : applierResponse.isRolledBack();
    }

    @Override
    public UpdateFailedException getDomainControllerRollbackFailure() {
        return rollbackResponse == null ? null : rollbackResponse.getDomainFailure();
    }

    @Override
    public Map<String, UpdateFailedException> getHostControllerRollbackFailures() {
        return rollbackResponse == null ? Collections.<String, UpdateFailedException>emptyMap(): rollbackResponse.getHostFailures();
    }

    void markRolledBack(BasicDomainUpdateResult rollbackResponse) {
        this.rollbackResponse = rollbackResponse;
    }

    void storeServerUpdateResult(ServerIdentity server, ServerUpdateResult result) {
        ServerGroupDeploymentActionResultImpl sgdar = (ServerGroupDeploymentActionResultImpl) serverResults.get(server.getServerGroupName());
        if (sgdar == null) {
            sgdar = new ServerGroupDeploymentActionResultImpl(server.getServerGroupName());
            serverResults.put(server.getServerGroupName(), sgdar);
        }
        sgdar.storeServerResult(server.getServerName(), result);
    }
}
