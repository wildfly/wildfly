/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.smoke.socketbindings;

import java.io.IOException;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.helpers.ClientConstants;
import org.jboss.dmr.ModelNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.*;

/**
 * Test if the /socket-binding=* runtime attributes shows the open ports as bound.
 *
 * @author Claudio Miranda
 */
@ExtendWith(ArquillianExtension.class)
@RunAsClient
public class SocketBindingBoundPortsTestCase {

    private static final String BOUND ="bound";
    private static final String BOUND_PORT ="bound-port";
    private static final String STANDARD_SOCKETS = "standard-sockets";

    @ContainerResource
    private ManagementClient managementClient;

    @Test
    public void testHttpBoundSocket() throws IOException {
        final ModelNode address = new ModelNode();
        address.add(SOCKET_BINDING_GROUP, "standard-sockets").add(SOCKET_BINDING, "http");
        address.protect();
        final ModelNode operation = new ModelNode();
        operation.get(OP).set(READ_RESOURCE_OPERATION);
        operation.get(INCLUDE_RUNTIME).set(true);
        operation.get(OP_ADDR).set(address);
        ModelNode response = execute(operation);
        ModelNode result = response.get(RESULT);
        Assertions.assertTrue(result.get(BOUND).asBoolean(), "http socket binding is not set as bound.");
        Assertions.assertEquals(8080, result.get(BOUND_PORT).asInt());
    }

    @Test
    public void testHttpsBoundSocket() throws IOException {
        final ModelNode address = new ModelNode();
        address.add(SOCKET_BINDING_GROUP, STANDARD_SOCKETS).add(SOCKET_BINDING, "https");
        address.protect();
        final ModelNode operation = new ModelNode();
        operation.get(OP).set(READ_RESOURCE_OPERATION);
        operation.get(INCLUDE_RUNTIME).set(true);
        operation.get(OP_ADDR).set(address);
        ModelNode response = execute(operation);
        ModelNode result = response.get(RESULT);
        Assertions.assertTrue(result.get(BOUND).asBoolean(), "https socket binding is not set as bound.");
        Assertions.assertEquals(8443, result.get(BOUND_PORT).asInt());
    }

    @Test
    public void testIiopBoundSocket() throws IOException {
        final ModelNode address = new ModelNode();
        address.add(SOCKET_BINDING_GROUP, "standard-sockets").add(SOCKET_BINDING, "iiop");
        address.protect();
        final ModelNode operation = new ModelNode();
        operation.get(OP).set(READ_RESOURCE_OPERATION);
        operation.get(INCLUDE_RUNTIME).set(true);
        operation.get(OP_ADDR).set(address);
        ModelNode response = execute(operation);
        ModelNode result = response.get(RESULT);
        Assertions.assertTrue(result.get(BOUND).asBoolean(), "iiop socket binding is not set as bound.");
        Assertions.assertEquals(3528, result.get(BOUND_PORT).asInt());
    }

    /**
     * Executes the operation and returns the result if successful. Else throws an exception
     */
    private ModelNode execute(final ModelNode operation) throws
            IOException {
        final ModelNode result = managementClient.getControllerClient().execute(operation);
        if (result.hasDefined(ClientConstants.OUTCOME) && ClientConstants.SUCCESS.equals(
                result.get(ClientConstants.OUTCOME).asString())) {
            return result;
        } else if (result.hasDefined(ClientConstants.FAILURE_DESCRIPTION)) {
            final String failureDesc = result.get(ClientConstants.FAILURE_DESCRIPTION).toString();
            throw new RuntimeException(failureDesc);
        } else {
            throw new RuntimeException("Operation not successful; outcome = " + result.get(ClientConstants.OUTCOME));
        }
    }

}
