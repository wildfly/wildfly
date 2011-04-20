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
package org.jboss.as.arquillian.common;

import java.io.InputStream;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.jboss.arquillian.api.ArchiveDeployer;
import org.jboss.as.controller.client.helpers.standalone.DeploymentAction;
import org.jboss.as.controller.client.helpers.standalone.DeploymentPlan;
import org.jboss.as.controller.client.helpers.standalone.DeploymentPlanBuilder;
import org.jboss.as.controller.client.helpers.standalone.ServerDeploymentActionResult;
import org.jboss.as.controller.client.helpers.standalone.ServerDeploymentManager;
import org.jboss.as.controller.client.helpers.standalone.ServerDeploymentPlanResult;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;

/**
 * A JBossAS archive deployer
 *
 * @author Thomas.Diesler@jboss.com
 * @since 16-Feb-2011
 */
public final class ArchiveDeployerImpl implements ArchiveDeployer {

    private ServerDeploymentManager deploymentManager;

    public ArchiveDeployerImpl(ServerDeploymentManager deploymentManager) {
        this.deploymentManager = deploymentManager;
    }

    @Override
    public String deploy(Archive<?> archive) throws Exception {
        InputStream input = archive.as(ZipExporter.class).exportZip();
        DeploymentPlanBuilder builder = deploymentManager.newDeploymentPlan();
        builder = builder.add(archive.getName(), input).andDeploy();
        DeploymentPlan plan = builder.build();
        DeploymentAction deployAction = builder.getLastAction();
        String runtimeName = executeDeploymentPlan(plan, deployAction);
        return runtimeName;
    }

    @Override
    public void undeploy(String runtimeName) throws Exception {
        DeploymentPlanBuilder builder = deploymentManager.newDeploymentPlan();
        DeploymentPlan plan = builder.undeploy(runtimeName).remove(runtimeName).build();
        Future<ServerDeploymentPlanResult> future = deploymentManager.execute(plan);
        future.get();
    }

    private String executeDeploymentPlan(DeploymentPlan plan, DeploymentAction deployAction) throws Exception {
        Future<ServerDeploymentPlanResult> future = deploymentManager.execute(plan);
        ServerDeploymentPlanResult planResult = future.get(5, TimeUnit.SECONDS);

        ServerDeploymentActionResult actionResult = planResult.getDeploymentActionResult(deployAction.getId());
        if (actionResult != null) {
            Exception deploymentException = (Exception) actionResult.getDeploymentException();
            if (deploymentException != null)
                throw deploymentException;
        }

        return deployAction.getDeploymentUnitUniqueName();
    }
}