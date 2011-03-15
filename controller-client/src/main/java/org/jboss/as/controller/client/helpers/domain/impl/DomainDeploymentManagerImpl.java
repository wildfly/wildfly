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

import static org.jboss.as.controller.client.helpers.ClientConstants.ADD;
import static org.jboss.as.controller.client.helpers.ClientConstants.COMPOSITE;
import static org.jboss.as.controller.client.helpers.ClientConstants.DEPLOYMENT;
import static org.jboss.as.controller.client.helpers.ClientConstants.DEPLOYMENT_DEPLOY_OPERATION;
import static org.jboss.as.controller.client.helpers.ClientConstants.DEPLOYMENT_FULL_REPLACE_OPERATION;
import static org.jboss.as.controller.client.helpers.ClientConstants.DEPLOYMENT_REDEPLOY_OPERATION;
import static org.jboss.as.controller.client.helpers.ClientConstants.DEPLOYMENT_REMOVE_OPERATION;
import static org.jboss.as.controller.client.helpers.ClientConstants.DEPLOYMENT_REPLACE_OPERATION;
import static org.jboss.as.controller.client.helpers.ClientConstants.DEPLOYMENT_UNDEPLOY_OPERATION;
import static org.jboss.as.controller.client.helpers.ClientConstants.NAME;
import static org.jboss.as.controller.client.helpers.ClientConstants.OP;
import static org.jboss.as.controller.client.helpers.ClientConstants.OP_ADDR;
import static org.jboss.as.controller.client.helpers.ClientConstants.ROLLBACK_ON_RUNTIME_FAILURE;
import static org.jboss.as.controller.client.helpers.ClientConstants.RUNTIME_NAME;
import static org.jboss.as.controller.client.helpers.ClientConstants.STEPS;
import static org.jboss.as.controller.client.helpers.ClientConstants.TO_REPLACE;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import org.jboss.as.controller.client.Cancellable;
import org.jboss.as.controller.client.Operation;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.as.controller.client.OperationResult;
import org.jboss.as.controller.client.ResultHandler;
import org.jboss.as.controller.client.helpers.domain.DeploymentPlan;
import org.jboss.as.controller.client.helpers.domain.DeploymentPlanResult;
import org.jboss.as.controller.client.helpers.domain.DomainDeploymentManager;
import org.jboss.as.controller.client.helpers.domain.DuplicateDeploymentNameException;
import org.jboss.as.controller.client.helpers.domain.InitialDeploymentPlanBuilder;
import org.jboss.as.controller.client.helpers.domain.ServerGroupDeploymentPlan;
import org.jboss.as.protocol.StreamUtils;
import org.jboss.dmr.ModelNode;

/**
 * Client-side {@link DomainDeploymentManager}.
 *
 * @author Brian Stansberry
 */
class DomainDeploymentManagerImpl implements DomainDeploymentManager {

    private final DomainClientImpl client;
    private final DeploymentContentDistributor contentDistributor;

    DomainDeploymentManagerImpl(final DomainClientImpl client) {
        assert client != null : "client is null";
        this.client = client;

        this.contentDistributor = new DeploymentContentDistributor() {
            @Override
            public byte[] distributeDeploymentContent(String name, String runtimeName, InputStream stream)
                    throws IOException, DuplicateDeploymentNameException {
                boolean unique = DomainDeploymentManagerImpl.this.client.isDeploymentNameUnique(name);
                if (!unique) {
                    throw new DuplicateDeploymentNameException(name, false);
                }
                return DomainDeploymentManagerImpl.this.client.addDeploymentContent(stream);
            }
            @Override
            public byte[] distributeReplacementDeploymentContent(String name, String runtimeName, InputStream stream)
                    throws IOException {
                return DomainDeploymentManagerImpl.this.client.addDeploymentContent(stream);
            }
        };
    }

    @Override
    public Future<DeploymentPlanResult> execute(DeploymentPlan plan) {
        if (!(plan instanceof DeploymentPlanImpl)) {
            throw new IllegalArgumentException("Cannot use a DeploymentPlan not created by this manager");
        }
        DeploymentPlanImpl planImpl = DeploymentPlanImpl.class.cast(plan);
        Map<UUID, String> actionsById = new HashMap<UUID, String>();
        Operation operation = getDeploymentPlanOperation(planImpl, actionsById);
        Handler handler = new Handler(operation);
        OperationResult c = client.execute(operation, handler.resultHandler);
        handler.setCancellable(c.getCancellable());
        return new DomainDeploymentPlanResultFuture(planImpl, handler, actionsById);
    }

    @Override
    public InitialDeploymentPlanBuilder newDeploymentPlan() {
        return InitialDeploymentPlanBuilderFactory.newInitialDeploymentPlanBuilder(this.contentDistributor);
    }

    private Operation getDeploymentPlanOperation(DeploymentPlanImpl plan, Map<UUID, String> actionsById) {
        Operation op = getCompositeOperation(plan, actionsById);
        addRollbackPlan(plan, op);
        return op;
    }

    private Operation getCompositeOperation(DeploymentPlanImpl plan, Map<UUID, String> actionsById) {

        Set<String> deployments = getCurrentDomainDeployments();
        Set<String> serverGroups = getServerGroupNames(plan);

        ModelNode op = new ModelNode();
        op.get(OP).set(COMPOSITE);
        op.get(OP_ADDR).setEmptyList();
        ModelNode steps = op.get(STEPS);
        steps.setEmptyList();
        op.get(ROLLBACK_ON_RUNTIME_FAILURE).set(plan.isSingleServerRollback());
        // FIXME deal with shutdown params

        OperationBuilder builder = OperationBuilder.Factory.create(op);
        int stepNum = 1;
        for (DeploymentActionImpl action : plan.getDeploymentActionImpls()) {

            actionsById.put(action.getId(), "step-" + stepNum);
            stepNum++;

            List<ModelNode> actionSteps = new ArrayList<ModelNode>();
            String uniqueName = action.getDeploymentUnitUniqueName();
            switch (action.getType()) {
            case ADD: {
                if (!deployments.contains(uniqueName)) {
                    // We need to add to the domain
                    ModelNode step = configureDeploymentOperation(ADD, uniqueName, null);
                    step.get(RUNTIME_NAME).set(action.getNewContentFileName());
                    step.get("hash").set(action.getNewContentHash());
                    actionSteps.add(step);
                }
                for (String group : serverGroups) {
                    ModelNode groupStep = configureDeploymentOperation(ADD, uniqueName, group);
                    actionSteps.add(groupStep);
                }
                break;
            }
            case DEPLOY: {
                for (String group : serverGroups) {
                    ModelNode groupStep = configureDeploymentOperation(DEPLOYMENT_DEPLOY_OPERATION, uniqueName, group);
                    actionSteps.add(groupStep);
                }
                break;
            }
            case FULL_REPLACE: {
                ModelNode step = new ModelNode();
                step.get(OP).set(DEPLOYMENT_FULL_REPLACE_OPERATION);
                step.get(OP_ADDR).setEmptyList();
                step.get(NAME).set(uniqueName);
                step.get(RUNTIME_NAME).set(action.getNewContentFileName());
                step.get("hash").set(action.getNewContentHash());
                actionSteps.add(step);
                break;
            }
            case REDEPLOY: {
                for (String group : serverGroups) {
                    ModelNode groupStep = configureDeploymentOperation(DEPLOYMENT_REDEPLOY_OPERATION, uniqueName, group);
                    actionSteps.add(groupStep);
                }
                break;
            }
            case REMOVE: {
                // From each group
                for (String group : serverGroups) {
                    ModelNode groupStep = configureDeploymentOperation(DEPLOYMENT_REMOVE_OPERATION, uniqueName, group);
                    actionSteps.add(groupStep);
                }
                // and from the domain
                ModelNode step = configureDeploymentOperation(DEPLOYMENT_REMOVE_OPERATION, uniqueName, null);
                actionSteps.add(step);
                break;
            }
            case REPLACE: {
                for (String group : serverGroups) {
                    ModelNode groupStep = new ModelNode();
                    groupStep.get(OP).set(DEPLOYMENT_REPLACE_OPERATION);
                    groupStep.get(OP_ADDR).add("server-group", group);
                    groupStep.get(NAME).set(uniqueName);
                    groupStep.get(TO_REPLACE).set(action.getReplacedDeploymentUnitUniqueName());
                    actionSteps.add(groupStep);
                }
                break;
            }
            case UNDEPLOY: {
                for (String group : serverGroups) {
                    ModelNode groupStep = configureDeploymentOperation(DEPLOYMENT_UNDEPLOY_OPERATION, uniqueName, group);
                    actionSteps.add(groupStep);
                }
                break;
            }
            default: {
                throw new IllegalStateException("Unknown action type " + action.getType());
            }
            }

            for (ModelNode actionStep : actionSteps) {
                steps.add(actionStep);
            }
        }

        return builder.build();
    }

    private Set<String> getCurrentDomainDeployments() {
        ModelNode op = new ModelNode();
        op.get("operation").set("read-children-names");
        op.get("child-type").set("deployment");
        ModelNode rsp = client.executeForResult(OperationBuilder.Factory.create(op).build());
        Set<String> deployments = new HashSet<String>();
        if (rsp.isDefined()) {
            for (ModelNode node : rsp.asList()) {
                deployments.add(node.asString());
            }
        }
        return deployments;
    }

    private void addRollbackPlan(DeploymentPlanImpl plan, Operation op) {
        ModelNode opNode = op.getOperation();
        ModelNode rolloutPlan = opNode.get("rollout-plan");
        rolloutPlan.get("rollback-across-groups").set(plan.isRollbackAcrossGroups());
        ModelNode series = rolloutPlan.get("in-series");
        for (Set<ServerGroupDeploymentPlan> concurrent : plan.getServerGroupDeploymentPlans()) {
            if (concurrent.size() == 1) {
                ModelNode single = new ModelNode();
                ServerGroupDeploymentPlan sgdp = concurrent.iterator().next();
                single.get("server-group", sgdp.getServerGroupName()).set(createServerGroupPlan(sgdp));
                series.add(single);
            }
            else {
                ModelNode multiple = new ModelNode();
                for (ServerGroupDeploymentPlan sgdp : concurrent) {
                    multiple.get("concurrent-groups", sgdp.getServerGroupName()).set(createServerGroupPlan(sgdp));
                }
                series.add(multiple);
            }
        }
    }

    private ModelNode createServerGroupPlan(ServerGroupDeploymentPlan sgdp) {
        ModelNode result = new ModelNode();
        result.get("rolling-to-servers").set(sgdp.isRollingToServers());
        if (sgdp.isRollback()) {
            if (sgdp.getMaxServerFailurePercentage() > 0) {
                result.get("max-failure-percentage").set(sgdp.getMaxServerFailurePercentage());
            }
            else {
                result.get("max-failed-servers").set(sgdp.getMaxServerFailures());
            }
        }
        else {
            result.get("max-failure-percentage").set(100);
        }
        return result;
    }

    private Set<String> getServerGroupNames(DeploymentPlan plan) {
        Set<String> names = new HashSet<String>();
        for (Set<ServerGroupDeploymentPlan> sgdps : plan.getServerGroupDeploymentPlans()) {
            for (ServerGroupDeploymentPlan sgdp : sgdps) {
                names.add(sgdp.getServerGroupName());
            }
        }
        return names;
    }

    private ModelNode configureDeploymentOperation(String operationName, String uniqueName, String serverGroup) {
        ModelNode op = new ModelNode();
        op.get(OP).set(operationName);
        if (serverGroup != null) {
            op.get(OP_ADDR).add("server-group", serverGroup);
        }
        op.get(OP_ADDR).add(DEPLOYMENT, uniqueName);
        return op;
    }

    private static class Handler implements Future<ModelNode> {

        private enum State {
            RUNNING, CANCELLED, DONE
        }

        private final Operation operation;
        private AtomicReference<State> state = new AtomicReference<State>(State.RUNNING);
        private final Thread runner = Thread.currentThread();
        private final ModelNode result = new ModelNode();
        private Cancellable cancellable;
        private final AtomicReference<Exception> exception = new AtomicReference<Exception>();

        private Handler(Operation operation) {
            this.operation = operation;
        }

        private final ResultHandler resultHandler = new ResultHandler() {
            @Override
            public void handleResultFragment(String[] location, ModelNode fragment) {
                if (state.get() == State.RUNNING) {
                    result.get(location).set(fragment);
                }
            }

            @Override
            public void handleResultComplete() {
                state.compareAndSet(State.RUNNING, State.DONE);
                cleanUpAndNotify();
            }

            @Override
            public void handleCancellation() {
                state.compareAndSet(State.RUNNING, State.CANCELLED);
                cleanUpAndNotify();
            }

            @Override
            public void handleFailed(ModelNode failureDescription) {
                String message = failureDescription.isDefined() ? failureDescription.toString() : "Operation failed with no failure description";
                Exception e = new Exception(message);
                exception.compareAndSet(null, e);
                state.compareAndSet(State.RUNNING, State.DONE);
                cleanUpAndNotify();
            }
        };

        synchronized void cleanUpAndNotify() {
            for (InputStream in : operation.getInputStreams()) {
                StreamUtils.safeClose(in);
            }
            notifyAll();
        }

        void setCancellable(Cancellable c) {
            this.cancellable = c;
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            boolean cancelled = false;
            if (state.get() == State.RUNNING) {
                try {
                    if (cancellable.cancel()) {
                        cancelled = state.compareAndSet(State.RUNNING, State.CANCELLED);
                    }
                } catch (IOException ignored) {
                    // ignore
                }

                if (!cancelled) {
                    if (mayInterruptIfRunning) {
                        runner.interrupt();
                    }
                    if (state.compareAndSet(State.RUNNING, State.DONE)) {
                        exception.set(new CancellationException());
                    }
                }
            }
            synchronized (this) {
                notifyAll();
            }
            return cancelled;
        }

        @Override
        public boolean isCancelled() {
            return state.get() == State.CANCELLED;
        }

        @Override
        public boolean isDone() {
            return state.get() != State.RUNNING;
        }

        @Override
        public ModelNode get() throws InterruptedException, ExecutionException {
            synchronized (this) {
                while (!isDone()) {
                    wait();
                }

                return getResult();
            }
        }

        @Override
        public ModelNode get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException,
                TimeoutException {
            long toWait = unit.toMillis(timeout);
            long expire = System.currentTimeMillis() + toWait;
            synchronized (this) {
                while (!isDone()) {
                    wait(toWait);
                    if (!isDone()) {
                        long now = System.currentTimeMillis();
                        if (now >= expire) {
                            throw new TimeoutException();
                        }
                        toWait = expire - now;
                    }
                }
                return getResult();
            }
        }

        private ModelNode getResult() throws ExecutionException {

            Exception e = exception.get();
            if (e instanceof ExecutionException) {
                throw (ExecutionException) e;
            } else if (e instanceof CancellationException) {
                throw (CancellationException) e;
            } else if (e != null) {
                throw new ExecutionException(e);
            } else if (state.get() == State.CANCELLED) {
                throw new CancellationException();
            } else {
                return result;
            }

        }

    }

}
