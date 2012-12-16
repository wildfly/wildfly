/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.domain.management.util;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.client.ModelControllerClient;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILD_TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.COMPOSITE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_CHILDREN_NAMES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_CHILDREN_TYPES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RUNNING_SERVER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_CONFIG;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STEPS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author Emanuel Muckenhuber
 */
public class DomainTestUtils {

    private static final int DEFAULT_TIMEOUT = 60;

    private DomainTestUtils() {
        //
    }

    /**
     * Get the address for a host.
     *
     * @param hostName the host name
     * @return the address
     */
    public static ModelNode getHostAddress(final String hostName) {
        final ModelNode address = new ModelNode();
        address.add(HOST, hostName);
        return address;
    }

    /**
     * Get the address for a running server.
     *
     * @param hostName the host name
     * @param serverName the server name
     * @return the address
     */
    public static ModelNode getRunningServerAddress(final String hostName, final String serverName) {
        final ModelNode address = getHostAddress(hostName);
        address.add(RUNNING_SERVER, serverName);
        return address;
    }

    /**
     * Get the address for a server config.
     *
     * @param hostName the host name
     * @param serverName the server name
     * @return the address
     */
    public static ModelNode getServerConfigAddress(final String hostName, final String serverName) {
        final ModelNode address = getHostAddress(hostName);
        address.add(SERVER_CONFIG, serverName);
        return address;
    }

    /**
     * Create a composite operation.
     *
     * @param steps the individual steps
     * @return the composite operation
     */
    public static ModelNode createCompositeOperation(ModelNode... steps) {
        final ModelNode composite = new ModelNode();
        composite.get(OP).set(COMPOSITE);
        composite.get(OP_ADDR).setEmptyList();
        composite.get(STEPS).setEmptyList();
        for(final ModelNode step : steps) {
            composite.get(STEPS).add(step);
        }
        return composite;
    }

    /**
     * Execute multiple steps and check the result.
     *
     * @param client the controller client
     * @param steps the individual steps
     * @return the operation result
     * @throws IOException
     * @throws MgmtOperationException
     */
    public static List<ModelNode> executeStepsForResult(final ModelControllerClient client, final ModelNode... steps) throws IOException, MgmtOperationException {
        final ModelNode operationResult = executeForResult(createCompositeOperation(steps), client);
        if(! operationResult.hasDefined(RESULT)) {
            return Collections.singletonList(operationResult);
        }
        final List<ModelNode> result = new ArrayList<ModelNode>();
        final int size = operationResult.get(RESULT).asPropertyList().size();
        for(int i = 0; i < size; i++) {
            result.add(operationResult.get(RESULT).require("steps-" + (i+1)));
        }
        return result;
    }

    /**
     * Execute for a successful result.
     *
     * @param op the operation to execute
     * @param modelControllerClient the controller client
     * @return the result
     * @throws IOException
     * @throws MgmtOperationException
     */
    public static ModelNode executeForResult(final ModelNode op, final ModelControllerClient modelControllerClient) throws IOException, MgmtOperationException {
       final ModelNode ret = modelControllerClient.execute(op);

       if (! SUCCESS.equals(ret.get(OUTCOME).asString())) {
           System.out.println(ret);
           throw new MgmtOperationException("Management operation failed.", op, ret);
       }
       return ret.get(RESULT);
    }

    /**
     * Execute for a failed outcome.
     *
     * @param op the operation to execute
     * @param modelControllerClient the controller client
     * @return the failure description
     * @throws IOException
     * @throws MgmtOperationException
     */
    public static ModelNode executeForFailure(final ModelNode op, final ModelControllerClient modelControllerClient) throws IOException, MgmtOperationException {
        final ModelNode ret = modelControllerClient.execute(op);

        if (! FAILED.equals(ret.get(OUTCOME).asString())) {
            System.out.println(ret);
            throw new MgmtOperationException("Management operation succeeded.", op, ret);
        }
        return ret.get(FAILURE_DESCRIPTION);
    }

    /**
     * Wait until a server reached a given state or fail if the timeout was reached.
     *
     * @param client the controller client
     * @param hostName the host name
     * @param serverName the server name
     * @param state the required state
     * @throws IOException
     */
    public static void waitUntilState(final ModelControllerClient client, final String hostName, final String serverName, final String state) throws IOException {
        waitUntilState(client, getServerConfigAddress(hostName, serverName), state);
    }

    /**
     * Wait until a server reached a given state or fail if the timeout was reached.
     *
     * @param client the controller client
     * @param serverAddress the server address
     * @param state the required state
     * @throws IOException
     */
    public static void waitUntilState(final ModelControllerClient client, final ModelNode serverAddress, final String state) throws IOException {
        waitUntilState(client, serverAddress, state, DEFAULT_TIMEOUT, TimeUnit.SECONDS);
    }

    /**
     * Check if a path address exists.
     *
     * @param address the child address
     * @param client the controller client
     * @return whether the child exists or not
     * @throws IOException
     * @throws MgmtOperationException
     */
    public static boolean exists(ModelNode address, ModelControllerClient client) throws IOException, MgmtOperationException {
        return exists(PathAddress.pathAddress(address), client);
    }

    /**
     * Check if a path address exists.
     *
     * @param address the path address
     * @param client the controller client
     * @return whether the child exists or not
     * @throws IOException
     * @throws MgmtOperationException
     */
    public static boolean exists(PathAddress address, ModelControllerClient client) throws IOException, MgmtOperationException {
        final PathElement element = address.getLastElement();
        final PathAddress subAddress = address.subAddress(0, address.size() -1);
        final boolean checkType = element.isWildcard();
        final ModelNode e;
        final ModelNode operation;
        if(checkType) {
            e = new ModelNode().set(element.getKey());
            operation = createOperation(READ_CHILDREN_TYPES_OPERATION, subAddress);
        } else {
            e = new ModelNode().set(element.getValue());
            operation = createOperation(READ_CHILDREN_NAMES_OPERATION, subAddress);
            operation.get(CHILD_TYPE).set(element.getKey());
        }
        try {
            final ModelNode result = executeForResult(operation, client);
            return result.asList().contains(e);
        } catch (MgmtOperationException ex) {
            if(! checkType) {
                final String failureDescription = ex.getResult().get(FAILURE_DESCRIPTION).asString();
                if(failureDescription.contains("JBAS014793") && failureDescription.contains(element.getKey())) {
                    return false;
                }
            }
            throw ex;
        }
    }

    /**
     * Start a managed server.
     *
     * @param connection the mgmt connection
     * @param host the host name
     * @param server the server name
     * @return the server state
     * @throws IOException
     * @throws MgmtOperationException
     */
    public static String startServer(final ModelControllerClient connection, final String host, final String server) throws IOException, MgmtOperationException {
        return startServer(connection, host, server, true);
    }

    /**
     * Start a managed server.
     *
     * @param connection the mgmt connection
     * @param host the host name
     * @param server the server name
     * @param blocking whether to block until the server is started
     * @return the server state
     * @throws IOException
     * @throws MgmtOperationException
     */
    public static String startServer(final ModelControllerClient connection, final String host, final String server, final boolean blocking) throws IOException, MgmtOperationException {
        final ModelNode address = getServerConfigAddress(host, server);
        final ModelNode operation = new ModelNode();
        operation.get(OP).set("start");
        operation.get(OP_ADDR).set(address);
        operation.get("blocking").set(blocking);
        // Start
        executeForResult(operation, connection);
        // Check the starte
        return getServerState(connection, address);
    }

    /**
     * Wait until a server reached a given state or fail if the timeout was reached.
     *
     * @param client the controller client
     * @param serverAddress the server address
     * @param required the required state
     * @param timeout the timeout
     * @param unit the time unit
     * @throws IOException
     */
    public static void waitUntilState(final ModelControllerClient client, final ModelNode serverAddress, final String required, final long timeout, final TimeUnit unit) throws IOException {
        final long deadline = System.currentTimeMillis() + unit.toMillis(timeout);
        for(;;) {
            final long remaining = deadline - System.currentTimeMillis();
            if(remaining <= 0) {
                break;
            }
            if (checkState(client, serverAddress, required)) {
                return;
            }
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch(InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
        final String state = getServerState(client, serverAddress);
        Assert.assertEquals(serverAddress.toString(), required, state);
    }

    /**
     * Get the state for a given server.
     *
     * @param client the controller client
     * @param serverAddress the server config address
     * @return the server state
     * @throws IOException
     */
    public static String getServerState(final ModelControllerClient client, final ModelNode serverAddress) throws IOException {
        final ModelNode operation = new ModelNode();
        operation.get(OP).set(READ_ATTRIBUTE_OPERATION);
        operation.get(OP_ADDR).set(serverAddress);
        operation.get(NAME).set("status");

        ModelNode status = client.execute(operation);
        return status.get(RESULT).asString();
    }

    /**
     * Check the state of server.
     *
     * @param client the controller client
     * @param serverAddress the server config address
     * @param state the expected state
     * @return {@code true} if the state matches, {@code false} otherwise
     * @throws IOException
     */
    public static boolean checkState(final ModelControllerClient client, final ModelNode serverAddress, final String state) throws IOException {
        final String serverState = getServerState(client, serverAddress);
        return state.equals(serverState);
    }

    public static ModelNode createOperation(String op, ModelNode address) {
        final ModelNode operation = new ModelNode();
        operation.get(OP).set(op);
        operation.get(OP_ADDR).set(address);
        return operation;
    }

    public static ModelNode createOperation(String op, PathAddress address) {
        return createOperation(op, address.toModelNode());
    }

}
