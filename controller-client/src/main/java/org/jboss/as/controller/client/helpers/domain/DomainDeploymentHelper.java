/*
 * JBoss, Home of Professional Open Source
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.controller.client.helpers.domain;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import org.jboss.as.controller.client.ModelControllerClient;

/**
 * A simple helper for server deployments.
 *
 * @author thomas.diesler@jboss.com
 * @since 22-Mar-2012
 */
public class DomainDeploymentHelper {

    private final DomainDeploymentManager deploymentManager;

    public DomainDeploymentHelper(ModelControllerClient client) {
        DomainClient domainClient = DomainClient.Factory.create(client);
        deploymentManager = domainClient.getDeploymentManager();
    }

    public DomainDeploymentHelper(DomainDeploymentManager deploymentManager) {
        this.deploymentManager = deploymentManager;
    }

    public String deploy(String name, InputStream input, List<String> serverGroups) throws DomainDeploymentException {
        return this.deploy(name, input, null, true, serverGroups);
    }

    public String deploy(String name, InputStream input, Map<String, Object> userdata, List<String> serverGroups) throws DomainDeploymentException {
        return this.deploy(name, input, userdata, true, serverGroups);
    }

    public String deploy(String name, InputStream input, boolean start, List<String> serverGroups) throws DomainDeploymentException {
        return this.deploy(name, input, null, start, serverGroups);
    }

    public String deploy(String name, InputStream input, Map<String, Object> userdata, boolean start, List<String> serverGroups) throws DomainDeploymentException {
        String runtimeName;
        DeploymentPlanResult planResult;
        try {
            DeploymentPlanBuilder builder = deploymentManager.newDeploymentPlan();
            AddDeploymentPlanBuilder addBuilder = builder.add(name, input);
            for (String group : serverGroups) {
                addBuilder = addBuilder.addMetadata(userdata);
                if (start == false) {
                    addBuilder = addBuilder.andNoStart();
                }
                DeploymentActionsCompleteBuilder completeBuilder = addBuilder.andDeploy();
                builder = completeBuilder.toServerGroup(group);
            }
            DeploymentPlan plan = builder.build();
            runtimeName = builder.getLastAction().getDeploymentUnitUniqueName();
            Future<DeploymentPlanResult> future = deploymentManager.execute(plan);
            planResult = future.get();
        } catch (Exception ex) {
            throw new DomainDeploymentException(ex);
        }
        Throwable failure = getDeploymentFailure(planResult);
        if (failure != null)
            throw new DomainDeploymentException(planResult, failure);
        return runtimeName;
    }

    public void undeploy(String runtimeName, List<String> serverGroups) throws DomainDeploymentException {
        DeploymentPlanResult planResult;
        try {
            DeploymentPlanBuilder builder = deploymentManager.newDeploymentPlan();
            for (String group : serverGroups) {
                DeploymentActionsCompleteBuilder completeBuilder = builder.undeploy(runtimeName).remove(runtimeName);
                builder = completeBuilder.toServerGroup(group);
            }
            DeploymentPlan plan = builder.build();
            Future<DeploymentPlanResult> future = deploymentManager.execute(plan);
            planResult = future.get();
        } catch (Exception ex) {
            throw new DomainDeploymentException(ex);
        }
        Throwable failure = getDeploymentFailure(planResult);
        if (failure != null)
            throw new DomainDeploymentException(planResult, failure);
    }

    private Throwable getDeploymentFailure(DeploymentPlanResult planResult) {
        for (DeploymentActionResult actionResult : planResult.getDeploymentActionResults().values()) {
            if (actionResult.getDomainControllerFailure() != null) {
                return actionResult.getDomainControllerFailure();
            }
            Map<String, UpdateFailedException> hostFailures = actionResult.getHostControllerFailures();
            if (hostFailures.size() > 0) {
                return hostFailures.values().iterator().next();
            }
            for (ServerGroupDeploymentActionResult serverGroupResult : actionResult.getResultsByServerGroup().values()) {
                for (ServerUpdateResult serverResult : serverGroupResult.getResultByServer().values()) {
                    if (serverResult.getFailureResult() != null)
                        return serverResult.getFailureResult();
                }
            }
        }
        return null;
    }

    public static class DomainDeploymentException extends Exception {
        private static final long serialVersionUID = 1L;
        private final DeploymentPlanResult planResult;

        private DomainDeploymentException(DeploymentPlanResult planResult, Throwable cause) {
            super(cause);
            this.planResult = planResult;
        }

        private DomainDeploymentException(Throwable cause) {
            super(cause);
            planResult = null;
        }

        public DeploymentPlanResult getPlanResult() {
            return planResult;
        }
    }
}
