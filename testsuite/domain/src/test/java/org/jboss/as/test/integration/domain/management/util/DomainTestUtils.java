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

import org.jboss.as.controller.client.ModelControllerClient;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.COMPOSITE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_CONFIG;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STEPS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author Emanuel Muckenhuber
 */
public class DomainTestUtils {

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
        address.add(SERVER_CONFIG, serverName);
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
     * Execute for result.
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
        waitUntilState(client, serverAddress, state, 30, TimeUnit.SECONDS);
    }

    /**
     * Wait until a server reached a given state or fail if the timeout was reached.
     *
     * @param client the controller client
     * @param serverAddress the server address
     * @param state the required state
     * @param timeout the timeout
     * @param unit the time unit
     * @throws IOException
     */
    public static void waitUntilState(final ModelControllerClient client, final ModelNode serverAddress, final String state, final long timeout, final TimeUnit unit) throws IOException {
        final long deadline = System.currentTimeMillis() + unit.toMillis(timeout);
        for(;;) {
            final long remaining = deadline - System.currentTimeMillis();
            if(remaining <= 0) {
                break;
            }
            if (checkState(client, serverAddress, state)) {
                return;
            }
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch(InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
        final String required = getServerState(client, serverAddress);
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

}
