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

package org.jboss.as.demos.web.connector.runner;

import static org.jboss.as.protocol.StreamUtils.safeClose;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.demos.DeploymentUtils;
import org.jboss.as.demos.war.archive.SimpleServlet;
import org.jboss.dmr.ModelNode;

/**
 * @author Emanuel Muckenhuber
 */
public class ExampleRunner {

    public static void main(String[] args) throws Exception {
        DeploymentUtils utils = null;
        final ModelControllerClient client = ModelControllerClient.Factory.create(InetAddress.getByName("localhost"), 9999);
        try {
            utils = new DeploymentUtils();
            utils.addWarDeployment("war-example.war", true, SimpleServlet.class.getPackage());
            utils.deploy();

            // Create the test connector
            createTestConnector(client);

            URLConnection conn = null;
            InputStream in = null;
            try {
                // Use the created connector on port 8380
                URL url = new URL("http://localhost:8380/war-example/simple?input=Hello");
                System.out.println("Reading response from " + url + ":");
                conn = url.openConnection();
                conn.setDoInput(true);
                in = new BufferedInputStream(conn.getInputStream());
                int i = in.read();
                while (i != -1) {
                    System.out.print((char)i);
                    i = in.read();
                }
                System.out.println("");
            } finally {
                safeClose(in);
            }

            // And remove the connector again
            removeTestConnector(client);

        } finally {
            utils.undeploy();
            safeClose(utils);
            safeClose(client);
        }

    }

    static void createTestConnector(final ModelControllerClient client) throws OperationFailedException, IOException {
        final List<ModelNode> updates = new ArrayList<ModelNode>();
        ModelNode op = new ModelNode();
        op.get("operation").set("add");
        op.get("address").add("socket-binding-group", "standard-sockets");
        op.get("address").add("socket-binding", "http-test");
        op.get("interface").set("default");
        op.get("port").set(8380);

        updates.add(op);

        op = new ModelNode();
        op.get("operation").set("add");
        op.get("address").add("subsystem", "web");
        op.get("address").add("connector", "testConnector");
        op.get("socket-binding").set("http-test");
        op.get("enabled").set(true);
        op.get("protocol").set("HTTP/1.1");
        op.get("scheme").set("http");

        updates.add(op);

        applyUpdates(updates, client);
    }

    static void removeTestConnector(final ModelControllerClient client) throws OperationFailedException, IOException {
        final List<ModelNode> updates = new ArrayList<ModelNode>();

        ModelNode op = new ModelNode();
        op.get("operation").set("remove");
        op.get("address").add("subsystem", "web");
        op.get("address").add("connector", "testConnector");

        updates.add(op);

        op = new ModelNode();
        op.get("operation").set("remove");
        op.get("address").add("socket-binding-group", "standard-sockets");
        op.get("address").add("socket-binding", "http-test");
        updates.add(op);

        applyUpdates(updates, client);
    }

    static void applyUpdates(final List<ModelNode> updates, final ModelControllerClient client) throws OperationFailedException, IOException  {
        // TODO consider creating a composite operation
        for(ModelNode update : updates) {
            applyUpdate(update, client);
        }
    }

    static void applyUpdate(ModelNode update, final ModelControllerClient client) throws OperationFailedException, IOException {
        ModelNode result = client.execute(OperationBuilder.Factory.create(update).build());
        if (result.hasDefined("outcome") && "success".equals(result.get("outcome").asString())) {
            if (result.hasDefined("result")) {
                System.out.println(result.get("result"));
            }
        }
        else if (result.hasDefined("failure-description")){
            throw new RuntimeException(result.get("failure-description").toString());
        }
        else {
            throw new RuntimeException("Operation not successful; outcome = " + result.get("outcome"));
        }
    }

}
