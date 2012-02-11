package org.jboss.as.controller.client.helpers.domain.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.jboss.as.controller.client.ControllerClientMessages;
import org.jboss.as.controller.client.helpers.ClientConstants;
import org.jboss.as.controller.client.helpers.domain.DeploymentAction;
import org.jboss.as.controller.client.helpers.domain.DeploymentActionResult;
import org.jboss.as.controller.client.helpers.domain.DeploymentPlanResult;
import org.jboss.as.controller.client.helpers.domain.ServerIdentity;
import org.jboss.as.controller.client.helpers.domain.UpdateFailedException;
import org.jboss.dmr.ModelNode;

/**
 * Adapts from Future<ModelNode> to Future<ServerDeploymentPlanResult>.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
class DomainDeploymentPlanResultFuture implements Future<DeploymentPlanResult> {

    private static final String DOMAIN_FAILURE_DESCRIPTION = "domain-failure-description";

    private static final String HOST_FAILURE_DESCRIPTION = "host-failure-description";

    private static final String ROLLED_BACK = "rolled-back";

    private final Future<ModelNode> nodeFuture;
    private final DeploymentPlanImpl plan;
    private final Map<UUID, List<String>> actionsById;
    private final Set<ServerIdentity> servers;

    DomainDeploymentPlanResultFuture(final DeploymentPlanImpl plan, final Future<ModelNode> nodeFuture, final Set<ServerIdentity> servers, final Map<UUID, List<String>> actionsById) {
        this.plan = plan;
        this.nodeFuture = nodeFuture;
        this.actionsById = actionsById;
        this.servers = servers;
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
        String outcome = planResultNode.get("outcome").asString();
        if ("cancelled".equals(outcome)) {
            createCancelledResults(actionResults);
        } else if ("success".equals(outcome)) {
            createSuccessResults(actionResults, planResultNode);
        } else {
            createFailureResults(actionResults, planResultNode);
        }
        return new DeploymentPlanResultImpl(plan, actionResults);
    }

    private void createFailureResults(Map<UUID, DeploymentActionResult> actionResults, ModelNode planResultNode) {
        boolean isDomainFailure = false;
        boolean isHostFailure = false;
        // Determine the failure type
        if (planResultNode.hasDefined(ClientConstants.FAILURE_DESCRIPTION)) {
            final ModelNode failureDescription = planResultNode.get(ClientConstants.FAILURE_DESCRIPTION);
            isDomainFailure = failureDescription.hasDefined(DOMAIN_FAILURE_DESCRIPTION);
            isHostFailure = failureDescription.hasDefined(HOST_FAILURE_DESCRIPTION);
        }
        final ModelNode result = planResultNode.get(ClientConstants.RESULT);
        for (DeploymentAction deploymentAction : plan.getDeploymentActions()) {
            // Get the steps used for this deployment action
            final List<String> steps = actionsById.get(deploymentAction.getId());
            for (String step : steps) {
                if (result.hasDefined(step)) {
                    // Get the result for this step
                    final ModelNode stepResult = result.get(step);
                    final boolean isRolledBack = (stepResult.hasDefined(ROLLED_BACK) && stepResult.get(ROLLED_BACK).asBoolean());
                    final UpdateFailedException updateFailedException;
                    // Create the failure description
                    if (stepResult.hasDefined(ClientConstants.FAILURE_DESCRIPTION)) {
                        updateFailedException = new UpdateFailedException(stepResult.get(ClientConstants.FAILURE_DESCRIPTION).asString());
                    } else if (planResultNode.hasDefined(ClientConstants.FAILURE_DESCRIPTION)) {
                        updateFailedException = new UpdateFailedException(planResultNode.get(ClientConstants.FAILURE_DESCRIPTION).asString());
                    } else {
                        updateFailedException = new UpdateFailedException(ControllerClientMessages.MESSAGES.noFailureDetails());
                    }
                    final BasicDomainUpdateResult domainUpdateResult;
                    if (isDomainFailure) {
                        domainUpdateResult = new BasicDomainUpdateResult(updateFailedException, isRolledBack);
                    } else if (isHostFailure) {
                        final Map<String, UpdateFailedException> hostExceptions = new HashMap<String, UpdateFailedException>();
                        for (ServerIdentity serverId : servers) {
                            hostExceptions.put(serverId.getHostName(), updateFailedException);
                        }
                        domainUpdateResult = new BasicDomainUpdateResult(hostExceptions, isRolledBack);
                    } else {
                        domainUpdateResult = new BasicDomainUpdateResult();
                    }
                    // Create the deployment action result
                    final DeploymentActionResultImpl deploymentActionResult = new DeploymentActionResultImpl(deploymentAction, domainUpdateResult);
                    final UpdateResultHandlerResponse resultHandlerResponse = UpdateResultHandlerResponse.createFailureResponse(updateFailedException);
                    // Update the action result with updates for each server
                    for (ServerIdentity serverId : servers) {
                        final ServerUpdateResultImpl serverUpdateResult = new ServerUpdateResultImpl(deploymentAction.getId(), serverId, resultHandlerResponse);
                        deploymentActionResult.storeServerUpdateResult(serverId, serverUpdateResult);
                    }
                    actionResults.put(deploymentAction.getId(), deploymentActionResult);
                }
            }
        }
    }

    private void createSuccessResults(Map<UUID, DeploymentActionResult> actionResults, ModelNode planResultNode) {
        createDefaultResults(actionResults, new BasicDomainUpdateResult(), UpdateResultHandlerResponse.createSuccessResponse(planResultNode));
    }

    private void createCancelledResults(Map<UUID, DeploymentActionResult> actionResults) {
        createDefaultResults(actionResults, new BasicDomainUpdateResult(true), UpdateResultHandlerResponse.createCancellationResponse());
    }

    private void createDefaultResults(final Map<UUID, DeploymentActionResult> actionResults, final BasicDomainUpdateResult domainUpdateResult, final UpdateResultHandlerResponse resultHandlerResponse) {
        for (DeploymentAction deploymentAction : plan.getDeploymentActions()) {
            // Create the deployment action result
            final DeploymentActionResultImpl deploymentActionResult = new DeploymentActionResultImpl(deploymentAction, domainUpdateResult);
            // Update the action result with updates for each server
            for (ServerIdentity serverId : servers) {
                final ServerUpdateResultImpl serverUpdateResult = new ServerUpdateResultImpl(deploymentAction.getId(), serverId, resultHandlerResponse);
                deploymentActionResult.storeServerUpdateResult(serverId, serverUpdateResult);
            }
            actionResults.put(deploymentAction.getId(), deploymentActionResult);
        }
    }
}