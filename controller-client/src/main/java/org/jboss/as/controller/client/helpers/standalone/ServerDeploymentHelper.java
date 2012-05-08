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
package org.jboss.as.controller.client.helpers.standalone;

import java.io.InputStream;
import java.util.concurrent.Future;

import org.jboss.as.controller.client.ModelControllerClient;

/**
 * A simple helper for server deployments.
 *
 * @author thomas.diesler@jboss.com
 * @since 22-Mar-2012
 */
public class ServerDeploymentHelper {

    private final ServerDeploymentManager deploymentManager;

    public ServerDeploymentHelper(ModelControllerClient client) {
        deploymentManager = ServerDeploymentManager.Factory.create(client);
    }

    public ServerDeploymentHelper(ServerDeploymentManager deploymentManager) {
        this.deploymentManager = deploymentManager;
    }

    public String deploy(String name, InputStream input) throws ServerDeploymentException {
        String runtimeName;
        ServerDeploymentActionResult actionResult;
        try {
            DeploymentPlanBuilder builder = deploymentManager.newDeploymentPlan();
            builder = builder.add(name, input).andDeploy();
            DeploymentPlan plan = builder.build();
            DeploymentAction action = builder.getLastAction();
            runtimeName = action.getDeploymentUnitUniqueName();
            Future<ServerDeploymentPlanResult> future = deploymentManager.execute(plan);
            ServerDeploymentPlanResult planResult = future.get();
            actionResult = planResult.getDeploymentActionResult(action.getId());
        } catch (Exception ex) {
            throw new ServerDeploymentException(ex);
        }
        if (actionResult.getDeploymentException() != null)
            throw new ServerDeploymentException(actionResult);
        return runtimeName;
    }

    public void undeploy(String runtimeName) throws ServerDeploymentException {
        ServerDeploymentActionResult actionResult;
        try {
            DeploymentPlanBuilder builder = deploymentManager.newDeploymentPlan();
            builder = builder.undeploy(runtimeName).remove(runtimeName);
            DeploymentPlan plan = builder.build();
            DeploymentAction action = builder.getLastAction();
            Future<ServerDeploymentPlanResult> future = deploymentManager.execute(plan);
            ServerDeploymentPlanResult planResult = future.get();
            actionResult = planResult.getDeploymentActionResult(action.getId());
        } catch (Exception ex) {
            throw new ServerDeploymentException(ex);
        }
        if (actionResult.getDeploymentException() != null)
            throw new ServerDeploymentException(actionResult);
    }

    public static class ServerDeploymentException extends Exception {
        private static final long serialVersionUID = 1L;
        private final ServerDeploymentActionResult actionResult;

        private ServerDeploymentException(ServerDeploymentActionResult actionResult) {
            super(actionResult.getDeploymentException());
            this.actionResult = actionResult;
        }

        private ServerDeploymentException(Throwable cause) {
            super(cause);
            actionResult = null;
        }

        public ServerDeploymentActionResult getActionResult() {
            return actionResult;
        }
    }
}
