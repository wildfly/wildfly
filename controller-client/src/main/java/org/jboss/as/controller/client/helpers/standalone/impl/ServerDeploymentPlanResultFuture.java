/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.jboss.as.controller.client.helpers.standalone.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.jboss.as.controller.client.helpers.ClientConstants;
import org.jboss.as.controller.client.helpers.standalone.ServerDeploymentActionResult;
import org.jboss.as.controller.client.helpers.standalone.ServerDeploymentPlanResult;
import org.jboss.as.controller.client.helpers.standalone.ServerUpdateActionResult.Result;
import org.jboss.as.controller.client.helpers.standalone.SimpleServerDeploymentActionResult;
import org.jboss.dmr.ModelNode;

/**
 * Adapts from Future<ModelNode> to Future<ServerDeploymentPlanResult>.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
class ServerDeploymentPlanResultFuture implements Future<ServerDeploymentPlanResult> {

    private final Future<ModelNode> nodeFuture;
    private final DeploymentPlanImpl plan;

    ServerDeploymentPlanResultFuture(final DeploymentPlanImpl plan, final Future<ModelNode> nodeFuture) {
        this.plan = plan;
        this.nodeFuture = nodeFuture;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return nodeFuture.cancel(mayInterruptIfRunning);
    }

    @Override
    public boolean isCancelled() {
        return nodeFuture.isCancelled();
    }

    @Override
    public boolean isDone() {
        return nodeFuture.isDone();
    }

    @Override
    public ServerDeploymentPlanResult get() throws InterruptedException, ExecutionException {
        ModelNode node = nodeFuture.get();
        return getResultFromNode(node.get(ClientConstants.RESULT));
    }

    @Override
    public ServerDeploymentPlanResult get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException,
            TimeoutException {
        ModelNode node = nodeFuture.get(timeout, unit);
        return getResultFromNode(node.get(ClientConstants.RESULT));
    }

    private ServerDeploymentPlanResult getResultFromNode(ModelNode planResultNode) {
        UUID planId = plan.getId();
        Map<UUID, ServerDeploymentActionResult> actionResults = new HashMap<UUID, ServerDeploymentActionResult>();
        List<DeploymentActionImpl> actions = plan.getDeploymentActionImpls();
        for (int i = 0; i < actions.size(); i++) {
            DeploymentActionImpl action = actions.get(i);
            UUID actionId = action.getId();
            ModelNode actionResultNode = planResultNode.get("step-" + (i + 1));
            actionResults.put(actionId, getActionResult(actionId, actionResultNode));
        }
        return new DeploymentPlanResultImpl(planId, actionResults);
    }

    private ServerDeploymentActionResult getActionResult(UUID actionId, ModelNode actionResultNode) {
        ServerDeploymentActionResult actionResult = null;
        String outcome = actionResultNode.get("outcome").asString();
        if ("cancelled".equals(outcome)) {
            actionResult = new SimpleServerDeploymentActionResult(actionId, Result.NOT_EXECUTED);
        } else if ("failed".equals(outcome)) {
            Exception e = actionResultNode.hasDefined("failure-description") ? new Exception(actionResultNode.get("failure-description").toString()) : null;
            if (actionResultNode.hasDefined("rolled-back") && actionResultNode.get("rolled-back").asBoolean()) {
                if (e == null) {
                    actionResult = new SimpleServerDeploymentActionResult(actionId, Result.ROLLED_BACK);
                } else {
                    actionResult = new SimpleServerDeploymentActionResult(actionId, Result.ROLLED_BACK, e);
                }
            } else {
                actionResult = new SimpleServerDeploymentActionResult(actionId, e);
            }
        } else {
            actionResult = new SimpleServerDeploymentActionResult(actionId, Result.EXECUTED);
        }
        // FIXME deal with shutdown possibilities
        return actionResult;
    }

}
