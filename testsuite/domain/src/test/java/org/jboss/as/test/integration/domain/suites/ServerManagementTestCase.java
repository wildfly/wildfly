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

import org.jboss.as.controller.client.ModelControllerClient;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.AUTO_START;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILD_TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;

import org.jboss.as.arquillian.container.domain.managed.DomainLifecycleUtil;
import org.jboss.as.controller.client.helpers.domain.DomainClient;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_CHILDREN_NAMES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING_PORT_OFFSET;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.START;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import org.jboss.as.test.integration.domain.DomainTestSupport;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;

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

    @Ignore("AS7-2653")
    @Test
    public void testAddAndRemoveServer() throws Exception {
        final DomainClient client = domainSlaveLifecycleUtil.getDomainClient();

        final ModelNode addServer = new ModelNode();
        addServer.get(OP).set(ADD);
        addServer.get(OP_ADDR).set(newServerConfigAddress);
        addServer.get(GROUP).set("main-server-group");
        addServer.get(SOCKET_BINDING_GROUP).set("standard-sockets");
        addServer.get(SOCKET_BINDING_PORT_OFFSET).set(650);
        addServer.get(AUTO_START).set(false);

        Assert.assertFalse(exists(client, newServerConfigAddress));
        Assert.assertFalse(exists(client, newRunningServerAddress));

        ModelNode result = client.execute(addServer);
        validateResponse(result);

        Assert.assertTrue(exists(client, newServerConfigAddress));
        Assert.assertFalse(exists(client, newRunningServerAddress));

        final ModelNode startServer = new ModelNode();
        startServer.get(OP).set(START);
        startServer.get(OP_ADDR).set(newServerConfigAddress);
        result = client.execute(startServer);
        validateResponse(result);
        waitUntilState(client, newServerConfigAddress, "STARTED");

        Assert.assertTrue(exists(client, newServerConfigAddress));
        Assert.assertTrue(exists(client, newRunningServerAddress));

        final ModelNode stopServer = new ModelNode();
        stopServer.get(OP).set("stop");
        stopServer.get(OP_ADDR).set(newServerConfigAddress);
        result = client.execute(stopServer);
        validateResponse(result);
        waitUntilState(client, newServerConfigAddress, "STOPPED");

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

    static ModelNode executeForResult(final ModelControllerClient client, final ModelNode operation) throws IOException {
        final ModelNode result = client.execute(operation);
        return validateResponse(result);
    }

    static ModelNode validateResponse(ModelNode response) {

        if(! SUCCESS.equals(response.get(OUTCOME).asString())) {
            System.out.println("Failed response:");
            System.out.println(response);
            Assert.fail(response.get(FAILURE_DESCRIPTION).toString());
        }

        Assert.assertTrue("result exists", response.has(RESULT));
        return response.get(RESULT);
    }

    static boolean exists(final ModelControllerClient client, final ModelNode address) throws IOException {
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

    static void waitUntilState(final ModelControllerClient client, final ModelNode serverAddress, final String state) throws IOException {

        final ModelNode operation = new ModelNode();
        operation.get(OP).set(READ_ATTRIBUTE_OPERATION);
        operation.get(OP_ADDR).set(serverAddress);
        operation.get(NAME).set("status");

        for(int i = 0; i < 10; i++) {
            try {
                Thread.sleep(500);
            } catch(InterruptedException e) {
                return;
            }
            final ModelNode status = client.execute(operation);
            if(state.equals(status.asString())) {
                return;
            }
        }
    }

}
