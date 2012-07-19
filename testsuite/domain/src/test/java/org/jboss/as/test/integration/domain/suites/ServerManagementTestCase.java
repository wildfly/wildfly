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

package org.jboss.as.test.integration.domain.suites;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.AUTO_START;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILD_TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_CHILDREN_NAMES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESTART_SERVERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_CONFIG;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING_PORT_OFFSET;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.START;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.START_SERVERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STOP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STOP_SERVERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import static org.jboss.as.test.integration.domain.management.util.DomainTestUtils.checkState;
import static org.jboss.as.test.integration.domain.management.util.DomainTestUtils.startServer;
import static org.jboss.as.test.integration.domain.management.util.DomainTestUtils.waitUntilState;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.domain.DomainClient;
import org.jboss.as.test.integration.domain.DomainTestSupport;
import org.jboss.as.test.integration.domain.management.util.DomainLifecycleUtil;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

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
        testSupport = DomainTestSuite.createSupport(ServerManagementTestCase.class.getSimpleName());

        domainMasterLifecycleUtil = testSupport.getDomainMasterLifecycleUtil();
        domainSlaveLifecycleUtil = testSupport.getDomainSlaveLifecycleUtil();
    }

    @AfterClass
    public static void tearDownDomain() throws Exception {
        DomainTestSuite.stopSupport();
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
        Assert.assertFalse(exists(client, newRunningServerAddress));

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
        Assert.assertFalse(exists(client, newRunningServerAddress));

        final ModelNode removeServer = new ModelNode();
        removeServer.get(OP).set(REMOVE);
        removeServer.get(OP_ADDR).set(newServerConfigAddress);

        result = client.execute(removeServer);
        validateResponse(result);

        Assert.assertFalse(exists(client, newServerConfigAddress));
        Assert.assertFalse(exists(client, newRunningServerAddress));
    }

    @Ignore("AS7-2653")
    @Test
    public void testDomainLifecycleMethods() throws Throwable {
        Throwable t = null;
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
        } catch (Throwable thr) {
            t = thr;
        } finally {
            //Set everything back to how it was:
            try {
                resetServerToExpectedState(client, "master", "main-one", "STARTED");
                resetServerToExpectedState(client, "master", "main-two", "DISABLED");
                resetServerToExpectedState(client, "master", "other-one", "DISABLED");
                resetServerToExpectedState(client, "slave", "main-three", "STARTED");
                resetServerToExpectedState(client, "slave", "main-four", "DISABLED");
                resetServerToExpectedState(client, "slave", "other-two", "STARTED");
            } catch (Exception e) {
                if (t == null) {
                    throw e;
                }
                e.printStackTrace();
            }
            if (t != null) {
                throw t;
            }
        }
    }

    @Test
    public void testAdminOnlyMode() throws Exception {

        // restart master HC in admin only mode
        final DomainClient masterClient = domainMasterLifecycleUtil.getDomainClient();
        ModelNode op = new ModelNode();
        op.get(OP_ADDR).add(HOST, "master");
        op.get(OP).set("reload");
        op.get("admin-only").set(true);
        domainMasterLifecycleUtil.executeAwaitConnectionClosed(op);

        // Try to reconnect to the hc
        domainMasterLifecycleUtil.connect();

        op = new ModelNode();
        op.get(OP).set(READ_ATTRIBUTE_OPERATION);
        op.get(OP_ADDR).add(HOST, "master");
        op.get(NAME).set("running-mode");

        // Validate that we are started in the --admin-only mode
        final ModelNode result = executeForResult(masterClient, op);
        Assert.assertEquals("ADMIN_ONLY", result.asString());

        // restart back to normal mode
        op = new ModelNode();
        op.get(OP_ADDR).add(HOST, "master");
        op.get(OP).set("reload");
        op.get("admin-only").set(false);
        domainMasterLifecycleUtil.executeAwaitConnectionClosed(op);

        // Try to reconnect to the hc
        domainMasterLifecycleUtil.connect();

        // check that the servers are up
        waitUntilState(masterClient, "master", "main-one", "STARTED");
        // waitUntilState(masterClient, "master", "other-one", "STARTED");

        // Wait for the slave to reconnect
        waitForHost(masterClient, "slave");
        //Assert.assertTrue(checkState(masterClient, getServerConfigAddress("slave", "main-tree"), "STARTED"));
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
        final ModelNode result = validateResponse(client.execute(operation));
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
        if(result.asList().contains(last.getValue())) {
            return true;
        }
        return false;
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
        }
    }

    private static void waitForHost(final ModelControllerClient client, final String hostName) throws Exception {
        final ModelNode operation = new ModelNode();
        operation.get(OP).set(READ_CHILDREN_NAMES_OPERATION);
        operation.get(OP_ADDR).setEmptyList();
        operation.get(CHILD_TYPE).set(HOST);
        final ModelNode host = new ModelNode().set(hostName);
        final long timeout = 30L;
        final TimeUnit timeUnit = TimeUnit.SECONDS;
        final long deadline = System.currentTimeMillis() + timeUnit.toMillis(timeout);
        for(;;) {
            final long remaining = deadline - System.currentTimeMillis();
            final ModelNode result = client.execute(operation);
            if(result.get(RESULT).asList().contains(host)) {
                return;
            }
            if(remaining <= 0) {
                return;
            }
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }
}
