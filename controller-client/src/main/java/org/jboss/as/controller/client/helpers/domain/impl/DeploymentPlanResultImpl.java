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

import static org.jboss.as.controller.client.ControllerClientMessages.MESSAGES;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.jboss.as.controller.client.helpers.domain.DeploymentActionResult;
import org.jboss.as.controller.client.helpers.domain.DeploymentPlan;
import org.jboss.as.controller.client.helpers.domain.DeploymentPlanResult;
import org.jboss.as.controller.client.helpers.domain.InvalidDeploymentPlanException;
import org.jboss.as.controller.client.helpers.domain.ServerGroupDeploymentActionResult;
import org.jboss.as.controller.client.helpers.domain.ServerGroupDeploymentPlanResult;
import org.jboss.as.controller.client.helpers.domain.ServerUpdateResult;


/**
 * Default implementation of {@link DeploymentPlanResult}.
 *
 * @author Brian Stansberry
 */
public class DeploymentPlanResultImpl implements DeploymentPlanResult {

    private final DeploymentPlan plan;
    private final InvalidDeploymentPlanException idpe;
    private final Map<UUID, DeploymentActionResult> results;
    private Map<String, ServerGroupDeploymentPlanResult> resultsByServerGroup;

    public DeploymentPlanResultImpl(final DeploymentPlan plan, final Map<UUID, DeploymentActionResult> results) {
        assert plan != null : "plan is null";
        assert results != null : "results is null";
        this.plan = plan;
        this.idpe = null;
        this.results = results;
    }

    public DeploymentPlanResultImpl(final DeploymentPlan plan, final InvalidDeploymentPlanException invalidPlanException) {
        if (plan == null)
            throw MESSAGES.nullVar("plan");
        if (invalidPlanException == null)
            throw MESSAGES.nullVar("invalidPlanException");
        this.plan = plan;
        this.results = null;
        this.idpe = invalidPlanException;
    }

    @Override
    public Map<UUID, DeploymentActionResult> getDeploymentActionResults() {
        return Collections.unmodifiableMap(results);
    }

    @Override
    public UUID getId() {
        return plan.getId();
    }

    @Override
    public DeploymentPlan getDeploymentPlan() {
        return plan;
    }

    @Override
    public InvalidDeploymentPlanException getInvalidDeploymentPlanException() {
        return idpe;
    }

    @Override
    public boolean isValid() {
        return idpe == null;
    }

    @Override
    public synchronized Map<String, ServerGroupDeploymentPlanResult> getServerGroupResults() {
        if (resultsByServerGroup == null) {
            this.resultsByServerGroup = buildServerGroupResults(results);
        }
        return Collections.unmodifiableMap(resultsByServerGroup);
    }

    // Builds the data structures that show the effects of the plan by server group
    private static Map<String, ServerGroupDeploymentPlanResult> buildServerGroupResults(Map<UUID, DeploymentActionResult> deploymentActionResults) {
        Map<String, ServerGroupDeploymentPlanResult> serverGroupResults = new HashMap<String, ServerGroupDeploymentPlanResult>();

        for (Map.Entry<UUID, DeploymentActionResult> entry : deploymentActionResults.entrySet()) {

            UUID actionId = entry.getKey();
            DeploymentActionResult actionResult = entry.getValue();

            Map<String, ServerGroupDeploymentActionResult> actionResultsByServerGroup = actionResult.getResultsByServerGroup();
            for (ServerGroupDeploymentActionResult serverGroupActionResult : actionResultsByServerGroup.values()) {
                String serverGroupName = serverGroupActionResult.getServerGroupName();

                ServerGroupDeploymentPlanResultImpl sgdpr = (ServerGroupDeploymentPlanResultImpl) serverGroupResults.get(serverGroupName);
                if (sgdpr == null) {
                    sgdpr = new ServerGroupDeploymentPlanResultImpl(serverGroupName);
                    serverGroupResults.put(serverGroupName, sgdpr);
                }

                for (Map.Entry<String, ServerUpdateResult> serverEntry : serverGroupActionResult.getResultByServer().entrySet()) {
                    String serverName = serverEntry.getKey();
                    ServerUpdateResult sud = serverEntry.getValue();
                    ServerDeploymentPlanResultImpl sdpr = (ServerDeploymentPlanResultImpl) sgdpr.getServerResult(serverName);
                    if (sdpr == null) {
                        sdpr = new ServerDeploymentPlanResultImpl(serverName);
                        sgdpr.storeServerResult(serverName, sdpr);
                    }
                    sdpr.storeServerUpdateResult(actionId, sud);
                }
            }
        }
        return serverGroupResults;
    }


}
