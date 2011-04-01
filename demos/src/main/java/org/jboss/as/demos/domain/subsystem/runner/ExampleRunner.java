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

package org.jboss.as.demos.domain.subsystem.runner;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PORT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROFILE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import static org.jboss.as.protocol.StreamUtils.safeClose;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLConnection;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.protocol.StreamUtils;
import org.jboss.dmr.ModelNode;

/**
 * Example updating a subsystem.
 *
 * @author Emanuel Muckenhuber
 */
public class ExampleRunner {

    static final ModelNode SB_ADDR = new ModelNode();
    static final ModelNode WC_ADDR = new ModelNode();

    static {
        SB_ADDR.add(SOCKET_BINDING_GROUP, "standard-sockets");
        SB_ADDR.add(SOCKET_BINDING, "new-http-binding");
        SB_ADDR.protect();
        WC_ADDR.add(PROFILE, "default");
        WC_ADDR.add(SUBSYSTEM, "web");
        WC_ADDR.add("connector", "new");
        WC_ADDR.protect();
    }

    public static void main(String[] args) throws Exception {
        final ModelControllerClient client = ModelControllerClient.Factory.create(InetAddress.getByName("localhost"), 9999);
        try {
            new ExampleRunner().run(client);
        } finally {
            StreamUtils.safeClose(client);
        }
    }

    protected void run(ModelControllerClient client) throws Exception {
        // Create a new socket binding
        final ModelNode bindingResult = createSocketBinding(client);
        checkSuccess(bindingResult);
        // Add a new web connector
        final ModelNode connectorResult = createWebConnector(client);
        checkSuccess(connectorResult);

        try {
            InputStream in = null;
            try {
                final URLConnection conn = new URL("http://localhost:8181/").openConnection();
                conn.setDoInput(true);
                conn.connect();
                in = new BufferedInputStream(conn.getInputStream());
                int i = in.read();
                StringBuilder sb = new StringBuilder();
                while (i != -1) {
                    sb.append((char)i);
                    i = in.read();
                }
            } catch (Exception ignore) {
                // this will most likely fail since there is no root deployment
            } finally {
                safeClose(in);
            }

            final ModelNode address = new ModelNode();
            address.add(HOST, "local");
            address.add(SERVER, "server-one");
            address.add(SUBSYSTEM, "web");
            address.add("connector", "new");

            final ModelNode stats = new ModelNode();
            stats.get(OP).set(READ_ATTRIBUTE_OPERATION);
            stats.get(OP_ADDR).set(address);
            stats.get(NAME).set("requestCount");

            final ModelNode result = client.execute(stats);
            checkSuccess(result);

            System.out.println(result.get(RESULT));

        } finally {

            final ModelNode remove1 = new ModelNode();
            remove1.get(OP).set(REMOVE);
            remove1.get(OP_ADDR).set(WC_ADDR);

            final ModelNode result1 = client.execute(remove1);
            checkSuccess(result1);

            final ModelNode remove2 = new ModelNode();
            remove2.get(OP).set(REMOVE);
            remove2.get(OP_ADDR).set(SB_ADDR);

            final ModelNode result2 = client.execute(remove2);
            checkSuccess(result2);
        }
    }

    ModelNode createSocketBinding(final ModelControllerClient client) throws Exception {
        final ModelNode operation = new ModelNode();
        operation.get(OP).set(ADD);
        operation.get(OP_ADDR).set(SB_ADDR);
        operation.get(NAME).set("new-http-binding");
        operation.get(PORT).set(8181);

        return client.execute(operation);
    }

    ModelNode createWebConnector(final ModelControllerClient client) throws Exception {
        final ModelNode operation = new ModelNode();
        operation.get(OP).set(ADD);
        operation.get(OP_ADDR).set(WC_ADDR);
        operation.get("protocol").set("HTTP/1.1");
        operation.get("socket-binding").set("new-http-binding");
        operation.get("scheme").set("http");

        return client.execute(operation);
    }

    void checkSuccess(final ModelNode result) {
        if(!SUCCESS.equals(result.get(OUTCOME).asString())) {
            String msg = result.hasDefined("failure-description") ? result.get("failure-description").toString() : "Operation failed with no failure description provided";
            throw new IllegalStateException(msg);
        }
    }

}
