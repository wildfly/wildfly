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
package org.jboss.as.controller.client.helpers;

import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.Future;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.standalone.AddDeploymentPlanBuilder;
import org.jboss.as.controller.client.helpers.standalone.DeploymentAction;
import org.jboss.as.controller.client.helpers.standalone.DeploymentPlan;
import org.jboss.as.controller.client.helpers.standalone.DeploymentPlanBuilder;
import org.jboss.as.controller.client.helpers.standalone.ServerDeploymentActionResult;
import org.jboss.as.controller.client.helpers.standalone.ServerDeploymentManager;
import org.jboss.as.controller.client.helpers.standalone.ServerDeploymentPlanResult;

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

    public String deploy(String name, InputStream input, Map<String, Object> userdata) throws Exception {
        DeploymentPlanBuilder builder = deploymentManager.newDeploymentPlan();
        AddDeploymentPlanBuilder addBuilder = builder.add(name, input);
        builder = addBuilder.addMetadata(userdata).andDeploy();
        DeploymentPlan plan = builder.build();
        DeploymentAction deployAction = builder.getLastAction();
        return executeDeploymentPlan(plan, deployAction);
    }

    public void undeploy(String runtimeName) throws Exception {
        DeploymentPlanBuilder builder = deploymentManager.newDeploymentPlan();
        DeploymentPlan plan = builder.undeploy(runtimeName).remove(runtimeName).build();
        Future<ServerDeploymentPlanResult> future = deploymentManager.execute(plan);
        future.get();
    }

    private String executeDeploymentPlan(DeploymentPlan plan, DeploymentAction action) throws Exception {
        Future<ServerDeploymentPlanResult> future = deploymentManager.execute(plan);
        ServerDeploymentPlanResult planResult = future.get();
        ServerDeploymentActionResult actionResult = planResult.getDeploymentActionResult(action.getId());
        if (actionResult != null) {
            Exception deploymentException = (Exception) actionResult.getDeploymentException();
            if (deploymentException != null)
                throw deploymentException;
        }
        return action.getDeploymentUnitUniqueName();
    }
}
