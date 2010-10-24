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
package org.jboss.as.demos.client;

import java.io.File;
import java.net.InetAddress;
import java.util.concurrent.Future;

import org.jboss.as.deployment.client.api.server.ServerDeploymentManager;
import org.jboss.as.deployment.client.api.server.ServerDeploymentPlanResult;
import org.jboss.as.model.ServerModel;
import org.jboss.as.standalone.client.api.StandaloneClient;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class __TestStandaloneClient {

    public static void main(String[] args) throws Exception {
        System.out.println("Creating client");
        StandaloneClient client = new StandaloneClient.Factory().create(InetAddress.getByName("localhost"), 9999);
        System.out.println("Created client, getting model...");
        ServerModel model = client.getServerModel();
        System.out.println("Got model " + model);  //Why is this null?
        System.out.println("Created client, getting dm...");
        ServerDeploymentManager manager = client.getDeploymentManager();
        System.out.println("Got manager " + manager);
        File file = new File("sar-example.sar");
        if (!file.exists()) {
            throw new IllegalStateException("No file sar-example.sar");
        }
        String deployment = manager.addDeploymentContent(file.toURL());
        System.out.println("Added deployment " + deployment);

        Future<ServerDeploymentPlanResult> deploymentResult = manager.execute(manager.newDeploymentPlan().deploy(deployment).build());
        System.out.println("Deployment result:" + deploymentResult);

    }
}
