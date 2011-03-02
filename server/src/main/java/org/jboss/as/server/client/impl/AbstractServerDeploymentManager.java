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

package org.jboss.as.server.client.impl;

import static org.jboss.as.server.client.ClientConstants.ADD;
import static org.jboss.as.server.client.ClientConstants.COMPOSITE;
import static org.jboss.as.server.client.ClientConstants.DEPLOYMENT;
import static org.jboss.as.server.client.ClientConstants.DEPLOYMENT_DEPLOY_OPERATION;
import static org.jboss.as.server.client.ClientConstants.DEPLOYMENT_FULL_REPLACE_OPERATION;
import static org.jboss.as.server.client.ClientConstants.INPUT_STREAM_INDEX;
import static org.jboss.as.server.client.ClientConstants.NAME;
import static org.jboss.as.server.client.ClientConstants.OP;
import static org.jboss.as.server.client.ClientConstants.OP_ADDR;
import static org.jboss.as.server.client.ClientConstants.DEPLOYMENT_REDEPLOY_OPERATION;
import static org.jboss.as.server.client.ClientConstants.DEPLOYMENT_REMOVE_OPERATION;
import static org.jboss.as.server.client.ClientConstants.DEPLOYMENT_REPLACE_OPERATION;
import static org.jboss.as.server.client.ClientConstants.ROLLBACK_ON_RUNTIME_FAILURE;
import static org.jboss.as.server.client.ClientConstants.RUNTIME_NAME;
import static org.jboss.as.server.client.ClientConstants.STEPS;
import static org.jboss.as.server.client.ClientConstants.TO_REPLACE;
import static org.jboss.as.server.client.ClientConstants.DEPLOYMENT_UNDEPLOY_OPERATION;

import java.util.concurrent.Future;

import org.jboss.as.controller.client.ExecutionContext;
import org.jboss.as.controller.client.ExecutionContextBuilder;
import org.jboss.as.server.client.api.deployment.DeploymentPlan;
import org.jboss.as.server.client.api.deployment.InitialDeploymentPlanBuilder;
import org.jboss.as.server.client.api.deployment.ServerDeploymentManager;
import org.jboss.as.server.client.api.deployment.ServerDeploymentPlanResult;
import org.jboss.as.server.client.impl.deployment.DeploymentActionImpl;
import org.jboss.as.server.client.impl.deployment.DeploymentPlanImpl;
import org.jboss.as.server.client.impl.deployment.InitialDeploymentPlanBuilderFactory;
import org.jboss.dmr.ModelNode;

/**
 * @author Emanuel Muckenhuber
 * @author Brian Stansberry
 */
abstract class AbstractServerDeploymentManager implements ServerDeploymentManager {

    AbstractServerDeploymentManager() {
    }

    /** {@inheritDoc} */
    @Override
    public InitialDeploymentPlanBuilder newDeploymentPlan() {
        return InitialDeploymentPlanBuilderFactory.newInitialDeploymentPlanBuilder();
    }

    /** {@inheritDoc} */
    @Override
    public Future<ServerDeploymentPlanResult> execute(DeploymentPlan plan) {
        if (!(plan instanceof DeploymentPlanImpl)) {
            throw new IllegalArgumentException("Plan was not created by this manager");
        }
        DeploymentPlanImpl planImpl = (DeploymentPlanImpl) plan;
        ExecutionContext operation = getCompositeOperation(planImpl);
        Future<ModelNode> nodeFuture = executeOperation(operation);
        return new ServerDeploymentPlanResultFuture(planImpl, nodeFuture);
    }

    protected abstract Future<ModelNode> executeOperation(ExecutionContext context);

    private ExecutionContext getCompositeOperation(DeploymentPlanImpl plan) {

        ModelNode op = new ModelNode();
        op.get(OP).set(COMPOSITE);
        op.get(OP_ADDR).setEmptyList();
        ModelNode steps = op.get(STEPS);
        steps.setEmptyList();
        op.get(ROLLBACK_ON_RUNTIME_FAILURE).set(plan.isGlobalRollback());
        // FIXME deal with shutdown params

        ExecutionContextBuilder builder = ExecutionContextBuilder.Factory.create(op);

        int stream = 0;
        for (DeploymentActionImpl action : plan.getDeploymentActionImpls()) {
            ModelNode step = new ModelNode();
            String uniqueName = action.getDeploymentUnitUniqueName();
            switch (action.getType()) {
            case ADD: {
                configureDeploymentOperation(step, ADD, uniqueName);
                step.get(RUNTIME_NAME).set(action.getNewContentFileName());
                builder.addInputStream(action.getContents());
                step.get(INPUT_STREAM_INDEX).set(stream++);
                break;
            }
            case DEPLOY: {
                configureDeploymentOperation(step, DEPLOYMENT_DEPLOY_OPERATION, uniqueName);
                break;
            }
            case FULL_REPLACE: {
                step.get(OP).set(DEPLOYMENT_FULL_REPLACE_OPERATION);
                step.get(OP_ADDR).setEmptyList();
                step.get(NAME).set(uniqueName);
                step.get(RUNTIME_NAME).set(action.getNewContentFileName());
                builder.addInputStream(action.getContents());
                step.get(INPUT_STREAM_INDEX).set(stream++);
                break;
            }
            case REDEPLOY: {
                configureDeploymentOperation(step, DEPLOYMENT_REDEPLOY_OPERATION, uniqueName);
                break;
            }
            case REMOVE: {
                configureDeploymentOperation(step, DEPLOYMENT_REMOVE_OPERATION, uniqueName);
                break;
            }
            case REPLACE: {
                step.get(OP).set(DEPLOYMENT_REPLACE_OPERATION);
                step.get(OP_ADDR).setEmptyList();
                step.get(NAME).set(uniqueName);
                step.get(TO_REPLACE).set(action.getReplacedDeploymentUnitUniqueName());
                break;
            }
            case UNDEPLOY: {
                configureDeploymentOperation(step, DEPLOYMENT_UNDEPLOY_OPERATION, uniqueName);
                break;
            }
            default: {
                throw new IllegalStateException("Unknown action type " + action.getType());
            }
            }
            steps.add(step);
        }

        return builder.build();
    }

    private void configureDeploymentOperation(ModelNode op, String operationName, String uniqueName) {
        op.get(OP).set(operationName);
        op.get(OP_ADDR).add(DEPLOYMENT, uniqueName);
    }
}
