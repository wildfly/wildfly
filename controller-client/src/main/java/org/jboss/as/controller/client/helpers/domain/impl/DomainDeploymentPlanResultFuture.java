package org.jboss.as.controller.client.helpers.domain.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.jboss.as.controller.client.helpers.domain.DeploymentAction;
import org.jboss.as.controller.client.helpers.domain.DeploymentActionResult;
import org.jboss.as.controller.client.helpers.domain.DeploymentPlanResult;
import org.jboss.dmr.ModelNode;

/**
 * Adapts from Future<ModelNode> to Future<ServerDeploymentPlanResult>.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
class DomainDeploymentPlanResultFuture implements Future<DeploymentPlanResult> {

    private final Future<ModelNode> nodeFuture;
    private final DeploymentPlanImpl plan;

    DomainDeploymentPlanResultFuture(final DeploymentPlanImpl plan, final Future<ModelNode> nodeFuture) {
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
    public DeploymentPlanResult get() throws InterruptedException, ExecutionException {
        ModelNode node = nodeFuture.get();
        return getResultFromNode(node);
    }

    @Override
    public DeploymentPlanResult get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException,
            TimeoutException {
        ModelNode node = nodeFuture.get(timeout, unit);
        return getResultFromNode(node);
    }

    private DeploymentPlanResult getResultFromNode(ModelNode planResultNode) {
        Map<UUID, DeploymentActionResult> actionResults = new HashMap<UUID, DeploymentActionResult>();
        List<DeploymentAction> actions = plan.getDeploymentActions();
        for (int i = 0; i < actions.size(); i++) {
            DeploymentAction action = actions.get(i);
            UUID actionId = action.getId();
            ModelNode actionResultNode = planResultNode.get("step-" + i);
            actionResults.put(actionId, getActionResult(action, actionResultNode));
        }
        return new DeploymentPlanResultImpl(plan, actionResults);
    }

    private DeploymentActionResult getActionResult(DeploymentAction action, ModelNode actionResultNode) {
        DeploymentActionResultImpl actionResult = null;
        String outcome = actionResultNode.get("outcome").asString();
        if ("cancelled".equals(outcome)) {
            actionResult = new DeploymentActionResultImpl(action, new DomainUpdateApplierResponse(true));
        } else if ("failed".equals(outcome)) {
            throw new UnsupportedOperationException("implement me");
//            Exception e = actionResultNode.hasDefined("failure-description") ? new Exception(actionResultNode.get("failure-description").toString()) : null;
//            if (actionResultNode.hasDefined("rolled-back") && actionResultNode.get("rolled-back").asBoolean()) {
//                if (e == null) {
//                    actionResult = new SimpleServerDeploymentActionResult(actionId, Result.ROLLED_BACK);
//                } else {
//                    actionResult = new SimpleServerDeploymentActionResult(actionId, Result.ROLLED_BACK, e);
//                }
//            } else {
//                actionResult = new SimpleServerDeploymentActionResult(actionId, e);
//            }
        } else {
            throw new UnsupportedOperationException("implement me");
//            actionResult = new SimpleServerDeploymentActionResult(actionId, Result.EXECUTED);
        }
        // FIXME deal with shutdown possibilities
        return actionResult;
    }

}