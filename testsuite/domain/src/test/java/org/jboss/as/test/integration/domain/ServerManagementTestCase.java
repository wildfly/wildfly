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

package org.jboss.as.test.integration.domain;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.AUTO_START;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.BLOCKING;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILD_TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.COMPOSITE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROFILE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_CHILDREN_NAMES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESTART;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESTART_SERVERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_CONFIG;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING_PORT_OFFSET;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.START;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.START_SERVERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STEPS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STOP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STOP_SERVERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import static org.jboss.as.test.integration.domain.management.util.DomainTestUtils.checkState;
import static org.jboss.as.test.integration.domain.management.util.DomainTestUtils.getServerConfigAddress;
import static org.jboss.as.test.integration.domain.management.util.DomainTestUtils.startServer;
import static org.jboss.as.test.integration.domain.management.util.DomainTestUtils.waitUntilState;

import java.io.IOException;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.domain.DomainClient;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.domain.management.util.DomainLifecycleUtil;
import org.jboss.as.test.integration.domain.management.util.DomainTestSupport;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author Emanuel Muckenhuber
 */
public class ServerManagementTestCase {

    private static DomainTestSupport testSupport;
    private static DomainLifecycleUtil domainMasterLifecycleUtil;
    private static DomainLifecycleUtil domainSlaveLifecycleUtil;

    private static final ModelNode slave = new ModelNode();
    private static final ModelNode mainOne = new ModelNode();
    private static final ModelNode newServerConfigAddress = new ModelNode();
    private static final ModelNode newRunningServerAddress = new ModelNode();

    static {
        // (host=slave)
        slave.add("host", "slave");
        // (host=slave),(server-config=new-server)
        newServerConfigAddress.add("host", "slave");
        newServerConfigAddress.add("server-config", "new-server");
        // (host=slave),(server=new-server)
        newRunningServerAddress.add("host", "slave");
        newRunningServerAddress.add("server", "new-server");
        // (host=master),(server-config=main-one)
        mainOne.add("host", "master");
        mainOne.add("server-config", "main-one");
    }

    @BeforeClass
    public static void setupDomain() throws Exception {
        testSupport = DomainTestSupport.createAndStartSupport(DomainTestSupport.Configuration.create(ServerManagementTestCase.class.getName(),
                "domain-configs/domain-minimal.xml", "host-configs/host-master.xml", "host-configs/host-slave.xml"));

        domainMasterLifecycleUtil = testSupport.getDomainMasterLifecycleUtil();
        domainSlaveLifecycleUtil = testSupport.getDomainSlaveLifecycleUtil();
    }

    @AfterClass
    public static void tearDownDomain() throws Exception {
        testSupport.stop();
        testSupport = null;
        domainMasterLifecycleUtil = null;
        domainSlaveLifecycleUtil = null;
    }

    @Test
    public void testRemoveStartedServer() throws Exception {
        final DomainClient client = domainMasterLifecycleUtil.getDomainClient();

        final ModelNode operation = new ModelNode();
        operation.get(OP).set(READ_ATTRIBUTE_OPERATION);
        operation.get(OP_ADDR).set(mainOne);
        operation.get(NAME).set("status");

        final ModelNode status = validateResponse(client.execute(operation));
        Assert.assertEquals("STARTED", status.asString());

        final ModelNode remove = new ModelNode();
        remove.get(OP).set(REMOVE);
        remove.get(OP_ADDR).set(mainOne);

        final ModelNode result = client.execute(remove);
        // Removing a started server should fail
        Assert.assertEquals(FAILED, result.get(OUTCOME).asString());

    }

    @Test
    public void testAddAndRemoveServer() throws Exception {
        final DomainClient client = domainSlaveLifecycleUtil.getDomainClient();

        final ModelNode addServer = new ModelNode();
        addServer.get(OP).set(ADD);
        addServer.get(OP_ADDR).set(newServerConfigAddress);
        addServer.get(GROUP).set("minimal");
        addServer.get(SOCKET_BINDING_GROUP).set("standard-sockets");
        addServer.get(SOCKET_BINDING_PORT_OFFSET).set(650);
        addServer.get(AUTO_START).set(false);

        Assert.assertFalse(exists(client, newServerConfigAddress));
        Assert.assertFalse(exists(client, newRunningServerAddress));

        ModelNode result = client.execute(addServer);

        validateResponse(result, false);

        Assert.assertTrue(exists(client, newServerConfigAddress));
        Assert.assertTrue(exists(client, newRunningServerAddress));

        startServer(client, "slave", "new-server");
        Assert.assertTrue(checkState(client, newServerConfigAddress, "STARTED"));

        Assert.assertTrue(exists(client, newServerConfigAddress));
        Assert.assertTrue(exists(client, newRunningServerAddress));

        final ModelNode stopServer = new ModelNode();
        stopServer.get(OP).set("stop");
        stopServer.get(OP_ADDR).set(newServerConfigAddress);
        stopServer.get("blocking").set(true);
        result = client.execute(stopServer);
        validateResponse(result);
        Assert.assertTrue(checkState(client, newServerConfigAddress, "DISABLED"));

        Assert.assertTrue(exists(client, newServerConfigAddress));
        Assert.assertTrue(exists(client, newRunningServerAddress));

        final ModelNode removeServer = new ModelNode();
        removeServer.get(OP).set(REMOVE);
        removeServer.get(OP_ADDR).set(newServerConfigAddress);

        result = client.execute(removeServer);
        validateResponse(result);

        Assert.assertFalse(exists(client, newServerConfigAddress));
        Assert.assertFalse(exists(client, newRunningServerAddress));
    }

    @Test
    public void testReloadServer() throws Exception {
        final DomainClient client = domainSlaveLifecycleUtil.getDomainClient();
        final ModelNode address = getServerConfigAddress("slave", "main-three");

        final ModelNode operation = new ModelNode();
        operation.get(OP).set("reload");
        operation.get(OP_ADDR).set(address);
        operation.get(BLOCKING).set(true);

        executeForResult(client, operation);

        Assert.assertTrue(checkState(client, address, "STARTED"));
    }

    @Test
    public void testBZ1015098() throws Exception {

        ModelNode compositeOp = Util.createEmptyOperation(COMPOSITE, PathAddress.EMPTY_ADDRESS);
        ModelNode steps = compositeOp.get(STEPS);
        steps.add(Util.createAddOperation(PathAddress.pathAddress(PathElement.pathElement(PROFILE, "BZ1015098"))));
        steps.add(Util.createEmptyOperation(RESTART, PathAddress.pathAddress(
                PathElement.pathElement(HOST, "slave"),
                PathElement.pathElement(SERVER_CONFIG, "other-two")
        )));

        ModelControllerClient client = testSupport.getDomainMasterLifecycleUtil().getDomainClient();
        validateResponse(client.execute(compositeOp));

        final ModelNode read = new ModelNode();
        read.get(OP).set(READ_RESOURCE_OPERATION);
        read.get(OP_ADDR).set(PathAddress.pathAddress(PathElement.pathElement(PROFILE, "BZ1015098")).toModelNode());

        validateResponse(client.execute(read));

    }

    @Test
    public void testDomainLifecycleMethods() throws Throwable {

        final DomainClient client = domainMasterLifecycleUtil.getDomainClient();
        try {
            executeLifecycleOperation(client, START_SERVERS);
            waitUntilState(client, "master", "main-one", "STARTED");
            waitUntilState(client, "master", "main-two", "STARTED");
            waitUntilState(client, "master", "other-one", "STARTED");
            waitUntilState(client, "slave", "main-three", "STARTED");
            waitUntilState(client, "slave", "main-four", "STARTED");
            waitUntilState(client, "slave", "other-two", "STARTED");

            executeLifecycleOperation(client, STOP_SERVERS);
            //When stopped auto-start=true -> STOPPED, auto-start=false -> DISABLED
            waitUntilState(client, "master", "main-one", "STOPPED");
            waitUntilState(client, "master", "main-two", "DISABLED");
            waitUntilState(client, "master", "other-one", "DISABLED");
            waitUntilState(client, "slave", "main-three", "STOPPED");
            waitUntilState(client, "slave", "main-four", "DISABLED");
            waitUntilState(client, "slave", "other-two", "STOPPED");

            executeLifecycleOperation(client, "other-server-group", START_SERVERS);
            //Check the affected servers have been started
            waitUntilState(client, "master", "other-one", "STARTED");
            waitUntilState(client, "slave", "other-two", "STARTED");
            //And that the remaining ones are still stopped
            waitUntilState(client, "master", "main-one", "STOPPED");
            waitUntilState(client, "master", "main-two", "DISABLED");
            waitUntilState(client, "slave", "main-three", "STOPPED");
            waitUntilState(client, "slave", "main-four", "DISABLED");

            executeLifecycleOperation(client, "other-server-group", RESTART_SERVERS);
            //Check the affected servers have been started
            waitUntilState(client, "master", "other-one", "STARTED");
            waitUntilState(client, "slave", "other-two", "STARTED");
            //And that the remaining ones are still stopped
            waitUntilState(client, "master", "main-one", "STOPPED");
            waitUntilState(client, "master", "main-two", "DISABLED");
            waitUntilState(client, "slave", "main-three", "STOPPED");
            waitUntilState(client, "slave", "main-four", "DISABLED");

            executeLifecycleOperation(client, "other-server-group", RESTART_SERVERS);
            //Check the affected servers have been started
            waitUntilState(client, "master", "other-one", "STARTED");
            waitUntilState(client, "slave", "other-two", "STARTED");
            //And that the remaining ones are still stopped
            waitUntilState(client, "master", "main-one", "STOPPED");
            waitUntilState(client, "master", "main-two", "DISABLED");
            waitUntilState(client, "slave", "main-three", "STOPPED");
            waitUntilState(client, "slave", "main-four", "DISABLED");

            executeLifecycleOperation(client, "other-server-group", STOP_SERVERS);
            //When stopped auto-start=true -> STOPPED, auto-start=false -> DISABLED
            waitUntilState(client, "master", "main-one", "STOPPED");
            waitUntilState(client, "master", "main-two", "DISABLED");
            waitUntilState(client, "master", "other-one", "DISABLED");
            waitUntilState(client, "slave", "main-three", "STOPPED");
            waitUntilState(client, "slave", "main-four", "DISABLED");
            waitUntilState(client, "slave", "other-two", "STOPPED");
        } finally {
            //Set everything back to how it was:
            try {
                resetServerToExpectedState(client, "master", "main-one", "STARTED");
                resetServerToExpectedState(client, "master", "main-two", "DISABLED");
                resetServerToExpectedState(client, "master", "other-one", "DISABLED");
                resetServerToExpectedState(client, "slave", "main-three", "STARTED");
                resetServerToExpectedState(client, "slave", "main-four", "DISABLED");
                resetServerToExpectedState(client, "slave", "other-two", "STARTED");
                waitUntilState(client, "master", "main-one", "STARTED");
                waitUntilState(client, "slave", "main-three", "STARTED");
                waitUntilState(client, "slave", "other-two", "STARTED");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void executeLifecycleOperation(final ModelControllerClient client, String opName) throws IOException {
        executeLifecycleOperation(client, null, opName);
    }

    private void executeLifecycleOperation(final ModelControllerClient client, String groupName, String opName) throws IOException {
        final ModelNode operation = new ModelNode();
        operation.get(OP).set(opName);
        if (groupName == null) {
            operation.get(OP_ADDR).setEmptyList();
        } else {
            operation.get(OP_ADDR).add(SERVER_GROUP, groupName);
        }
        validateResponse(client.execute(operation));
    }

    private ModelNode executeForResult(final ModelControllerClient client, final ModelNode operation) throws IOException {
        final ModelNode result = client.execute(operation);
        return validateResponse(result);
    }

    private ModelNode validateResponse(ModelNode response) {
        return validateResponse(response, true);
    }

    private ModelNode validateResponse(ModelNode response, boolean validateResult) {

        if(! SUCCESS.equals(response.get(OUTCOME).asString())) {
            System.out.println("Failed response:");
            System.out.println(response);
            Assert.fail(response.get(FAILURE_DESCRIPTION).toString());
        }

        if (validateResult) {
            Assert.assertTrue("result exists", response.has(RESULT));
        }
        return response.get(RESULT);
    }

    private boolean exists(final ModelControllerClient client, final ModelNode address) throws IOException {
        final ModelNode parentAddress = new ModelNode();
        final int size = address.asInt();
        for(int i = 0; i < size - 1; i++) {
            final Property property = address.get(i).asProperty();
            parentAddress.add(property.getName(), property.getValue());
        }
        final Property last = address.get(size -1).asProperty();

        final ModelNode childrenNamesOp = new ModelNode();
        childrenNamesOp.get(OP).set(READ_CHILDREN_NAMES_OPERATION);
        childrenNamesOp.get(OP_ADDR).set(parentAddress);
        childrenNamesOp.get(CHILD_TYPE).set(last.getName());

        final ModelNode result = executeForResult(client, childrenNamesOp);
        return result.asList().contains(last.getValue());
    }

    private void resetServerToExpectedState(final ModelControllerClient client, final String hostName, final String serverName, final String state) throws IOException {
        final ModelNode serverConfigAddress = new ModelNode().add(HOST, hostName).add(SERVER_CONFIG, serverName);
        if (!checkState(client, serverConfigAddress, state)) {
            final ModelNode operation = new ModelNode();
            operation.get(OP_ADDR).set(serverConfigAddress);
            if (state.equals("STARTED")) {
                //start server
                operation.get(OP).set(START);
            } else if (state.equals("STOPPED") || state.equals("DISABLED")) {
                //stop server
                operation.get(OP).set(STOP);
            }
            client.execute(operation);
        }
    }
}
