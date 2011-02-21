/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.webservices.deployer;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;

import org.jboss.as.server.client.api.deployment.DeploymentAction;
import org.jboss.as.server.client.api.deployment.DeploymentPlan;
import org.jboss.as.server.client.api.deployment.DeploymentPlanBuilder;
import org.jboss.as.server.client.api.deployment.ServerDeploymentActionResult;
import org.jboss.as.server.client.api.deployment.ServerDeploymentManager;
import org.jboss.as.server.client.api.deployment.ServerDeploymentPlanResult;
import org.jboss.logging.Logger;
import org.jboss.wsf.spi.deployer.Deployer;

/**
 * Remote deployer that uses AS7 client deployment API.
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public final class RemoteDeployer implements Deployer {

    private static final Logger LOGGER = Logger.getLogger(RemoteDeployer.class);
    private final Map<URL, String> url2Id = new HashMap<URL, String>();

    private ServerDeploymentManager deploymentManager;

    public RemoteDeployer() throws IOException {
        final InetAddress address = InetAddress.getByName("127.0.0.1");
        deploymentManager = ServerDeploymentManager.Factory.create(address, 9999);
    }

    @Override
    public void deploy(final URL url) throws Exception {
        final DeploymentPlanBuilder builder = deploymentManager.newDeploymentPlan().add(url).andDeploy();
        final DeploymentPlan plan = builder.build();
        final DeploymentAction deployAction = builder.getLastAction();
        final String uniqueId = executeDeploymentPlan(plan, deployAction);
        url2Id.put(url, uniqueId);
    }

    @Override
    public void undeploy(final URL archive) throws Exception {
        final DeploymentPlanBuilder builder = deploymentManager.newDeploymentPlan();
        final String uniqueName = url2Id.get(archive);
        final DeploymentPlan plan = builder.undeploy(uniqueName).remove(uniqueName).build();
        final Future<ServerDeploymentPlanResult> future = deploymentManager.execute(plan);
        url2Id.remove(archive);
        future.get();
    }

    private String executeDeploymentPlan(final DeploymentPlan plan, final DeploymentAction deployAction) throws Exception {
        final ServerDeploymentPlanResult planResult = deploymentManager.execute(plan).get();

        final ServerDeploymentActionResult actionResult = planResult.getDeploymentActionResult(deployAction.getId());
        if (actionResult != null) {
            final Exception deploymentException = (Exception) actionResult.getDeploymentException();
            if (deploymentException != null)
                throw deploymentException;
        }

        return deployAction.getDeploymentUnitUniqueName();
    }

}
