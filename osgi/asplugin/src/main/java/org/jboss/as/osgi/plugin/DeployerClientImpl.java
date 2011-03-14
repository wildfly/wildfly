/*
 * JBoss, Home of Professional Open Source
 * Copyright 2005, JBoss Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.osgi.plugin;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URL;
import java.util.concurrent.Future;

import org.jboss.as.controller.client.helpers.standalone.DeploymentAction;
import org.jboss.as.controller.client.helpers.standalone.DeploymentPlan;
import org.jboss.as.controller.client.helpers.standalone.DeploymentPlanBuilder;
import org.jboss.as.controller.client.helpers.standalone.ServerDeploymentActionResult;
import org.jboss.as.controller.client.helpers.standalone.ServerDeploymentManager;
import org.jboss.as.controller.client.helpers.standalone.ServerDeploymentPlanResult;
import org.jboss.logging.Logger;
import org.jboss.osgi.testing.OSGiDeployerClient;
import org.jboss.osgi.testing.OSGiRuntime;

/**
 * An abstract deployer for the {@link OSGiRuntime}
 *
 * @author Thomas.Diesler@jboss.org
 * @since 09-Nov-2010
 */
public class DeployerClientImpl implements OSGiDeployerClient {

    // Provide logging
    private static final Logger log = Logger.getLogger(DeployerClientImpl.class);

    private ServerDeploymentManager deploymentManager;

    public DeployerClientImpl() throws IOException {
        InetAddress address = InetAddress.getByName("127.0.0.1");
        deploymentManager = ServerDeploymentManager.Factory.create(address, 9999);
    }

    @Override
    public String deploy(URL url) throws Exception {
        DeploymentPlanBuilder builder = deploymentManager.newDeploymentPlan();
        builder = builder.add(url).andDeploy();
        DeploymentPlan plan = builder.build();
        DeploymentAction deployAction = builder.getLastAction();
        return executeDeploymentPlan(plan, deployAction);
    }

    @Override
    public String deploy(String name, InputStream input) throws Exception {
        DeploymentPlanBuilder builder = deploymentManager.newDeploymentPlan();
        builder = builder.add(name, input).andDeploy();
        DeploymentPlan plan = builder.build();
        DeploymentAction deployAction = builder.getLastAction();
        return executeDeploymentPlan(plan, deployAction);
    }

    @Override
    public void undeploy(String uniqueName) {
        try {
            DeploymentPlanBuilder builder = deploymentManager.newDeploymentPlan();
            DeploymentPlan plan = builder.undeploy(uniqueName).remove(uniqueName).build();
            Future<ServerDeploymentPlanResult> future = deploymentManager.execute(plan);
            future.get();
        } catch (Throwable ex) {
            log.warn("Cannot undeploy: " + uniqueName, ex);
        }
    }

    private String executeDeploymentPlan(DeploymentPlan plan, DeploymentAction deployAction) throws Exception {
        Future<ServerDeploymentPlanResult> future = deploymentManager.execute(plan);
        ServerDeploymentPlanResult planResult = future.get();

        ServerDeploymentActionResult actionResult = planResult.getDeploymentActionResult(deployAction.getId());
        if (actionResult != null) {
            Exception deploymentException = (Exception) actionResult.getDeploymentException();
            if (deploymentException != null)
                throw deploymentException;
        }

        return deployAction.getDeploymentUnitUniqueName();
    }
}