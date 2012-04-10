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
package org.jboss.as.arquillian.container.domain;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Future;

import org.jboss.arquillian.container.spi.client.container.DeploymentException;
import org.jboss.as.controller.client.helpers.domain.DeploymentAction;
import org.jboss.as.controller.client.helpers.domain.DeploymentPlan;
import org.jboss.as.controller.client.helpers.domain.DeploymentPlanBuilder;
import org.jboss.as.controller.client.helpers.domain.DeploymentPlanResult;
import org.jboss.as.controller.client.helpers.domain.DomainDeploymentManager;
import org.jboss.as.controller.client.helpers.domain.InitialDeploymentPlanBuilder;
import org.jboss.as.controller.client.helpers.domain.ServerDeploymentPlanResult;
import org.jboss.as.controller.client.helpers.domain.ServerGroupDeploymentPlanResult;
import org.jboss.as.controller.client.helpers.domain.ServerUpdateResult;
import org.jboss.as.controller.client.helpers.standalone.ServerDeploymentManager;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;

/**
 * A deployer that uses the {@link ServerDeploymentManager}
 *
 * @author Thomas.Diesler@jboss.com
 * @since 17-Nov-2010
 */
public class ArchiveDeployer {

    private static final Logger log = Logger.getLogger(ArchiveDeployer.class);

    private DomainDeploymentManager deploymentManager;

    public ArchiveDeployer(DomainDeploymentManager deploymentManager) {
        this.deploymentManager = deploymentManager;
    }

    public String deploy(Archive<?> archive, String target) throws DeploymentException {
        try {
            final InputStream input = archive.as(ZipExporter.class).exportAsInputStream();
            try {
                InitialDeploymentPlanBuilder builder = deploymentManager.newDeploymentPlan();
                DeploymentPlan plan = builder.add(archive.getName(), input).andDeploy().toServerGroup(target).build();
                DeploymentAction deployAction = plan.getDeploymentActions().get(plan.getDeploymentActions().size() - 1);
                return executeDeploymentPlan(plan, deployAction);
            } finally {
                if (input != null)
                    try {
                        input.close();
                    } catch (IOException e) {
                        log.warnf(e, "Failed to close resource %s", input);
                    }
            }

        } catch (Exception e) {
            throw new DeploymentException("Could not deploy to container", e);
        }
    }

    public void undeploy(String runtimeName, String target) throws DeploymentException {
        try {
            DeploymentPlanBuilder builder = deploymentManager.newDeploymentPlan();
            DeploymentPlan plan = builder.undeploy(runtimeName).remove(runtimeName).toServerGroup(target).build();
            Future<DeploymentPlanResult> future = deploymentManager.execute(plan);
            future.get();
        } catch (Exception ex) {
            log.warn("Cannot undeploy: " + runtimeName + ":" + ex.getMessage());
        }
    }

    private String executeDeploymentPlan(DeploymentPlan plan, DeploymentAction deployAction) throws Exception {
        Future<DeploymentPlanResult> future = deploymentManager.execute(plan);
        DeploymentPlanResult planResult = future.get();

        Map<String, ServerGroupDeploymentPlanResult> actionResults = planResult.getServerGroupResults();
        for (Entry<String, ServerGroupDeploymentPlanResult> result : actionResults.entrySet()) {
            for (Entry<String, ServerDeploymentPlanResult> planServerResult : result.getValue().getServerResults().entrySet()) {
                ServerUpdateResult actionResult = planServerResult.getValue().getDeploymentActionResults()
                        .get(deployAction.getId());
                if (actionResult != null) {
                    Exception deploymentException = (Exception) actionResult.getFailureResult();
                    if (deploymentException != null)
                        throw deploymentException;
                }
            }
        }

        return deployAction.getDeploymentUnitUniqueName();
    }
}