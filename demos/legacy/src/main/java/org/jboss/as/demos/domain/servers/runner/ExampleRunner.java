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
package org.jboss.as.demos.domain.servers.runner;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.protocol.StreamUtils;
import org.jboss.dmr.ModelNode;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.*;

/**
 * Demonstration of basic aspects of administering servers via the domain management API.
 *
 * TODO improve this by putting it in a loop that lists the set of (numbered) commands on sysout
 * and reads the desired command and parameter from stdin
 *
 * @author Brian Stansberry
 */
public class ExampleRunner {

    public static void main(String[] args) throws Exception {

        final ModelControllerClient client = ModelControllerClient.Factory.create("localhost", 9999);
        try {

            final ModelNode hostNamesOp = new ModelNode();
            hostNamesOp.get(OP).set(READ_CHILDREN_NAMES_OPERATION);
            hostNamesOp.get(OP_ADDR).setEmptyList();
            hostNamesOp.get(CHILD_TYPE).set(HOST);

            final ModelNode hostNames = client.execute(hostNamesOp);
            final Map<String, Set<String>> hosts = new HashMap<String, Set<String>>();
            for(final ModelNode host : hostNames.get(RESULT).asList()) {

                final String hostName = host.asString();
                final Set<String> serverNames = new HashSet<String>();
                hosts.put(host.asString(), serverNames);

                final ModelNode serverOp = new ModelNode();
                serverOp.get(OP).set(READ_CHILDREN_NAMES_OPERATION);
                serverOp.get(OP_ADDR).setEmptyList().add(HOST, host.asString());
                serverOp.get(CHILD_TYPE).set(SERVER_CONFIG);

                final ModelNode serversResult = client.execute(serverOp);
                for(final ModelNode server : serversResult.get(RESULT).asList()) {
                    final String serverName = server.asString();
                    serverNames.add(serverName);

                    final ModelNode serverAddress = new ModelNode();
                    serverAddress.add(HOST, hostName);
                    serverAddress.add(SERVER_CONFIG, serverName);

                    final ModelNode serverConfigOp = new ModelNode();
                    serverConfigOp.get(OP).set(READ_RESOURCE_OPERATION);
                    serverConfigOp.get(OP_ADDR).set(serverAddress);
                    serverConfigOp.get(INCLUDE_RUNTIME).set(true);

                    final ModelNode serverConfig = client.execute(serverConfigOp);

                    System.out.println("\nServer:\n");
                    System.out.println("server name:          " + serverName);
                    System.out.println("host controller name: " + hostName);
                    System.out.println("server group name:    " + serverConfig.get(RESULT, GROUP).asString());
                    System.out.println("status:               " + serverConfig.get(RESULT, "status"));

                    if("STARTED".equals(serverConfig.get(RESULT, "status").asString())) {
                        serverNames.add(serverName);
                    }
                }
            }

            for(final Entry<String, Set<String>> entry : hosts.entrySet()) {
                final String hostName = entry.getKey();

                for(final String serverName : entry.getValue()) {
                    final ModelNode address = new ModelNode();
                    address.add(HOST, hostName);
                    address.add(SERVER_CONFIG, serverName);

                    runServerOperation(client, address, "stop");
                }
            }

            Thread.sleep(2000); //

            for(final Entry<String, Set<String>> entry : hosts.entrySet()) {
                final String hostName = entry.getKey();

                for(final String serverName : entry.getValue()) {
                    final ModelNode address = new ModelNode();
                    address.add(HOST, hostName);
                    address.add(SERVER_CONFIG, serverName);

                    runServerOperation(client, address, "start");
                }
            }

            Thread.sleep(10000); //

            for(final Entry<String, Set<String>> entry : hosts.entrySet()) {
                final String hostName = entry.getKey();

                for(final String serverName : entry.getValue()) {
                    final ModelNode address = new ModelNode();
                    address.add(HOST, hostName);
                    address.add(SERVER_CONFIG, serverName);

                    runServerOperation(client, address, "restart");
                }
            }

            Thread.sleep(2000); //


        } finally {
            StreamUtils.safeClose(client);
        }
    }


    static ModelNode runServerOperation(final ModelControllerClient client, final ModelNode address, final String opName) throws Exception {
        final ModelNode stopOperation = new ModelNode();
        stopOperation.get(OP).set(opName);
        stopOperation.get(OP_ADDR).set(address);

        final ModelNode result = client.execute(stopOperation);
        checkSuccess(result);
        return result.get(RESULT);
    }

    static void checkSuccess(final ModelNode operationResult) throws OperationFailedException {
        if(!SUCCESS.equals(operationResult.get(OUTCOME).asString())) {
            throw new OperationFailedException(operationResult.get(FAILURE_DESCRIPTION));
        }
    }

}
