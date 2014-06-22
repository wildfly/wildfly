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

package org.jboss.as.test.integration.domain.suites;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.BLOCKING;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.COMPOSITE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CONCURRENT_GROUPS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.IN_SERIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.JVM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MAX_FAILURE_PERCENTAGE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PORT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RELOAD_REQUIRED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESTART_REQUIRED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ROLLOUT_PLAN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STEPS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;
import static org.jboss.as.test.integration.domain.management.util.DomainTestUtils.executeForResult;
import static org.jboss.as.test.integration.domain.management.util.DomainTestUtils.startServer;
import static org.jboss.as.test.integration.domain.management.util.DomainTestUtils.waitUntilState;

import java.io.IOException;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.domain.DomainClient;
import org.jboss.as.test.integration.domain.management.util.DomainLifecycleUtil;
import org.jboss.as.test.integration.domain.management.util.DomainTestSupport;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ValueExpression;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test conditions where the server should be put into a restart-required state.
 *
 * @author Emanuel Muckenhuber
 */
public class ServerRestartRequiredTestCase {

    private static DomainTestSupport testSupport;
    private static DomainLifecycleUtil domainMasterLifecycleUtil;
    private static DomainLifecycleUtil domainSlaveLifecycleUtil;

    private static final ModelNode reloadOneAddress = new ModelNode();
    private static final ModelNode reloadTwoAddress = new ModelNode();
    private static final ModelNode reloadOneConfigAddress = new ModelNode();
    private static final ModelNode reloadTwoConfigAddress = new ModelNode();

    static {
        // (host=slave),(server-config=reload-one)
        reloadOneConfigAddress.add("host", "master");
        reloadOneConfigAddress.add("server-config", "reload-one");
        // (host=slave),(server=reload-one)
        reloadOneAddress.add("host", "master");
        reloadOneAddress.add("server", "reload-one");
        // (host=slave),(server-config=reload-two)
        reloadTwoConfigAddress.add("host", "slave");
        reloadTwoConfigAddress.add("server-config", "reload-two");
        // (host=slave),(server=reload-two)
        reloadTwoAddress.add("host", "slave");
        reloadTwoAddress.add("server", "reload-two");
    }

    @BeforeClass
    public static void setupDomain() throws Exception {
        testSupport = DomainTestSuite.createSupport(ServerRestartRequiredTestCase.class.getSimpleName());

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

    @Before
    public void startServers() throws Exception {
        final DomainClient client = domainMasterLifecycleUtil.getDomainClient();

        // Start reload-one
        startServer(client, "master", "reload-one");
        // Start reload-two
        startServer(client, "slave", "reload-two");
        // Check the states
        waitUntilState(client, reloadOneConfigAddress, "STARTED");
        waitUntilState(client, reloadTwoConfigAddress, "STARTED");
    }

    @After
    public void stopServers() throws Exception {
        final DomainClient client = domainMasterLifecycleUtil.getDomainClient();

        final ModelNode stopServer = new ModelNode();
        stopServer.get(OP).set("stop");
        stopServer.get(OP_ADDR).set(reloadOneConfigAddress);
        client.execute(stopServer);
        waitUntilState(client, reloadOneConfigAddress, "DISABLED");
        stopServer.get(OP_ADDR).set(reloadTwoConfigAddress);
        client.execute(stopServer);
        waitUntilState(client, reloadTwoConfigAddress, "DISABLED");
    }

    @Test
    public void testSocketBinding() throws Exception {
        final DomainClient client = domainMasterLifecycleUtil.getDomainClient();

        // add a custom socket binding, referencing a non-existent system property
        final ModelNode socketBinding = new ModelNode();
        socketBinding.add(SOCKET_BINDING_GROUP, "reload-sockets");
        socketBinding.add(SOCKET_BINDING, "my-custom-binding");

        final ModelNode socketBindingAdd = new ModelNode();
        socketBindingAdd.get(OP).set(ADD);
        socketBindingAdd.get(OP_ADDR).set(socketBinding);
        socketBindingAdd.get(PORT).set(new ValueExpression("${my.custom.socket}"));

        // Don't rollback server failures, rather mark them as restart-required
        socketBindingAdd.get(OPERATION_HEADERS).get(ROLLOUT_PLAN)
                .get(IN_SERIES).add().get(CONCURRENT_GROUPS).get("reload-test-group").get(MAX_FAILURE_PERCENTAGE).set(100);

        executeOperation(socketBindingAdd, client);

        checkRestartOneAndTwo(client);
    }

    @Test
    public void testChangeGroup() throws Exception {

        final ModelNode address = reloadTwoConfigAddress;

        final DomainClient client = domainMasterLifecycleUtil.getDomainClient();
        final ModelNode stopOP = new ModelNode();
        stopOP.get(OP).set("stop");
        stopOP.get(OP_ADDR).set(address);

        // Stop and wait
        executeForResult(stopOP, client);
        waitUntilState(client, address, "DISABLED");

        final ModelNode composite = new ModelNode();
        composite.get(OP).set(COMPOSITE);
        composite.get(OP_ADDR).setEmptyList();

        final ModelNode updateGroup = composite.get(STEPS).add();
        updateGroup.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
        updateGroup.get(OP_ADDR).set(address);
        updateGroup.get(NAME).set("group");
        updateGroup.get(VALUE).set("reload-test-group");

        // Execute composite operation
        executeForResult(composite, client);

        // Trigger a reload required operation
        final ModelNode updateProfile = new ModelNode();
        updateProfile.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
        updateProfile.get(OP_ADDR).add(SERVER_GROUP, "reload-test-group");
        updateProfile.get(NAME).set("profile");
        updateProfile.get(VALUE).set("default");

        try {
            executeForResult(updateProfile, client);
            // Check that server one is in reload-required (two is stopped)
            assertServerState(reloadOneAddress, client, RELOAD_REQUIRED);
        } finally {
            final ModelNode restore = updateProfile.clone();
            restore.get(VALUE).set("minimal");
            executeForResult(restore, client);
        }

        final ModelNode reloadIfRequired = new ModelNode();
        reloadIfRequired.get(OP).set("reload-servers");
        reloadIfRequired.get(OP_ADDR).add(SERVER_GROUP, "reload-test-group");
        reloadIfRequired.get(BLOCKING).set(true);

        // Reload
        executeForResult(reloadIfRequired, client);
        assertServerState(reloadOneAddress, client, "running");
    }

    @Test
    public void testServerGroupJVMs() throws Exception {

        final DomainClient client = domainMasterLifecycleUtil.getDomainClient();

        final ModelNode address = new ModelNode();
        address.add(SERVER_GROUP, "reload-test-group");
        address.add(JVM, "default");

        final ModelNode operation = new ModelNode();
        operation.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
        operation.get(OP_ADDR).set(address);
        operation.get(NAME).set("heap-size");
        operation.get(VALUE).set("32M");

        // Update the JVM
        executeOperation(operation, client);

        checkRestartOneAndTwo(client);
    }

    @Test
    public void testCompositeNavigation() throws Exception {

        final DomainClient client = domainMasterLifecycleUtil.getDomainClient();

        checkProcessStateOneAndTwo(client, "running");

        final ModelNode composite = new ModelNode();
        composite.get(OP).set(COMPOSITE);
        composite.get(OP_ADDR).setEmptyList();

        final ModelNode steps = composite.get(STEPS).setEmptyList();

        final ModelNode rr = steps.add();
        rr.get(OP).set(READ_RESOURCE_OPERATION);
        rr.get(OP_ADDR).set(reloadOneConfigAddress).add(JVM, "default");

        final ModelNode rs = steps.add();
        rs.get(OP).set(READ_RESOURCE_OPERATION);
        rs.get(OP_ADDR).set(reloadTwoConfigAddress).add(JVM, "default");

        domainMasterLifecycleUtil.executeForResult(composite);

        checkProcessStateOneAndTwo(client, "running");
    }

    @Test
    public void testHostVM() throws Exception {

        final DomainClient client = domainMasterLifecycleUtil.getDomainClient();

        final ModelNode address = new ModelNode();
        address.add(HOST, "slave");
        address.add(JVM, "default");

        final ModelNode operation = new ModelNode();
        operation.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
        operation.get(OP_ADDR).set(address);
        operation.get(NAME).set("heap-size");
        operation.get(VALUE).set("32M");

        // Update the JVM
        executeOperation(operation, client);

        // check the process state for reload-one
        final ModelNode serverOne = reloadOneAddress;

        final ModelNode resultOne = getServerState(serverOne, client);
        Assert.assertEquals("running", resultOne.asString());

        // check the process state for reload-two
        final ModelNode serverTwo = reloadTwoAddress;
        final ModelNode resultTwo = getServerState(serverTwo, client);
        Assert.assertEquals(RESTART_REQUIRED, resultTwo.asString());
    }

    @Test
    public void testServerConfigVM() throws Exception {
        final DomainClient client = domainMasterLifecycleUtil.getDomainClient();

        final ModelNode address = reloadTwoConfigAddress.clone();
        address.add(JVM, "default");

        final ModelNode operation = new ModelNode();
        operation.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
        operation.get(OP_ADDR).set(address);
        operation.get(NAME).set("heap-size");
        operation.get(VALUE).set("32M");

        // Update the JVM
        executeOperation(operation, client);

        // check the process state for reload-one
        final ModelNode serverOne = reloadOneAddress;
        final ModelNode resultOne = getServerState(serverOne, client);
        Assert.assertEquals("running", resultOne.asString());

        // check the process state for reload-two
        final ModelNode serverTwo = reloadTwoAddress;
        final ModelNode resultTwo = getServerState(serverTwo, client);
        Assert.assertEquals(RESTART_REQUIRED, resultTwo.asString());
    }

    @Test
    public void testRestartGroup() throws Exception {

        final DomainClient client = domainMasterLifecycleUtil.getDomainClient();

        final ModelNode operation = new ModelNode();
        operation.get(OP).set("restart-servers");
        operation.get(OP_ADDR).add(SERVER_GROUP, "reload-test-group");
        operation.get("blocking").set(true);

        executeOperation(operation, client);

        // Check the states
        waitUntilState(client, reloadOneConfigAddress, "STARTED");
        waitUntilState(client, reloadTwoConfigAddress, "STARTED");

    }

    @Test
    public void testReloadGroup() throws Exception {

        final DomainClient client = domainMasterLifecycleUtil.getDomainClient();

        final ModelNode operation = new ModelNode();
        operation.get(OP).set("reload-servers");
        operation.get(OP_ADDR).add(SERVER_GROUP, "reload-test-group");
        operation.get("blocking").set(true);

        executeOperation(operation, client);

        // Check the states
        waitUntilState(client, reloadOneConfigAddress, "STARTED");
        waitUntilState(client, reloadTwoConfigAddress, "STARTED");

    }

    private void checkReloadOneAndTwo(final ModelControllerClient client) throws Exception {
        checkProcessStateOneAndTwo(client, RELOAD_REQUIRED);
    }

    private void checkRestartOneAndTwo(final ModelControllerClient client) throws Exception {
        checkProcessStateOneAndTwo(client, RESTART_REQUIRED);
    }

    private void checkProcessStateOneAndTwo(final ModelControllerClient client, final String expected) throws Exception {
        // check the process state for reload-one
        assertServerState(reloadOneAddress, client, expected);
        // check the process state for reload-two
        assertServerState(reloadTwoAddress, client, expected);
    }

    protected void assertServerState(final ModelNode address, final ModelControllerClient client, final String expected) throws Exception {
        final ModelNode resultTwo = getServerState(address, client);
        Assert.assertEquals(expected, resultTwo.asString());
    }

    private ModelNode getServerState(final ModelNode address, final ModelControllerClient client) throws Exception {

        final ModelNode operation = new ModelNode();
        operation.get(OP).set(READ_ATTRIBUTE_OPERATION);
        operation.get(OP_ADDR).set(address);
        operation.get(NAME).set("server-state");

        return executeOperation(operation, client);
    }

    private ModelNode executeOperation(final ModelNode op, final ModelControllerClient modelControllerClient) throws IOException, MgmtOperationException {
       ModelNode ret = modelControllerClient.execute(op);

       if (! SUCCESS.equals(ret.get(OUTCOME).asString())) {
           throw new MgmtOperationException("Management operation failed: ", op, ret);
       }
       return ret.get(RESULT);
   }

}
