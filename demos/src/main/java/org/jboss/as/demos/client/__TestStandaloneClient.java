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

import static org.jboss.as.protocol.StreamUtils.safeClose;

import java.io.File;
import java.net.InetAddress;
import java.util.concurrent.Future;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.server.client.api.deployment.ServerDeploymentManager;
import org.jboss.as.server.client.api.deployment.ServerDeploymentPlanResult;
import org.jboss.dmr.ModelNode;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class __TestStandaloneClient {

    public static void main(String[] args) throws Exception {
        System.out.println("Creating client");
        ModelControllerClient client = null;
        try {
            client = ModelControllerClient.Factory.create(InetAddress.getByName("localhost"), 9999);
            System.out.println("Created client, getting model...");
            ModelNode op = new ModelNode();
            op.get("operation").set("read-resource");
            op.get("address").setEmptyList();
            op.get("recursive").set(true);
            ModelNode response = client.execute(op);
            if ("success".equals(op.get("outcome").asString())) {
                System.out.println("Got model " + response.get("result"));
            }
            else {
                System.out.println("Failed to get model -- " + response.get("failure-description"));
            }

            // Apply update
            op = new ModelNode();
            op.get("operation").set("add");
            op.get("address").add("path", "org.jboss.test");
            op.get("path").set("/home/emuckenh/Downloads");
            response = client.execute(op);
            if ("success".equals(op.get("outcome").asString())) {
                System.out.println("Added path");
            }
            else {
                System.out.println("Failed to add path -- " + response.get("failure-description"));
            }

            System.out.println("Created client, getting dm...");
            ServerDeploymentManager manager = ServerDeploymentManager.Factory.create(client);
            System.out.println("Got manager " + manager);
            File file = new File("sar-example.sar");
            if (!file.exists()) {
                throw new IllegalStateException("No file sar-example.sar");
            }
            String deployment = manager.addDeploymentContent(file.toURL());
            System.out.println("Added deployment " + deployment);

            Future<ServerDeploymentPlanResult> deploymentResult = manager.execute(manager.newDeploymentPlan().add(deployment, file).deploy(deployment).build());
            System.out.println("Deployment result:" + deploymentResult);
            System.out.println("Contained deployment result:" + deploymentResult.get());

            Thread.sleep(3000);

            manager.execute(manager.newDeploymentPlan().undeploy(deployment).remove(deployment).build());
        } finally {
            safeClose(client);
        }
    }
}
