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

package org.jboss.as.test.integration.messaging.mgmt;

import static org.jboss.as.controller.operations.common.Util.getEmptyOperation;

import java.io.IOException;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.common.jms.JMSOperations;
import org.jboss.as.test.integration.common.jms.JMSOperationsProvider;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests the management API for HornetQ core addresss.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
@RunAsClient()
@RunWith(Arquillian.class)
public class AddressControlManagementTestCase {

    private static long count = System.currentTimeMillis();

    @ContainerResource
    private static ManagementClient managementClient;

    private JMSOperations jmsOperations;

    @Before
    public void setup() throws Exception {
        jmsOperations = JMSOperationsProvider.getInstance(managementClient.getControllerClient());

        count++;

        jmsOperations.addCoreQueue(getQueueName(), getAddress(), false);
        jmsOperations.addCoreQueue(getOtherQueueName(), getAddress(), false);
    }

    @After
    public void cleanup() throws Exception {
        jmsOperations.removeCoreQueue(getQueueName());
        jmsOperations.removeCoreQueue(getOtherQueueName());
    }

    @Test
    public void testSubsystemRootOperations() throws Exception {

        ModelNode op = getSubsystemOperation("read-children-types");
        op.get("child-type").set("core-address");
        ModelNode result = execute(op, true);
        Assert.assertTrue(result.isDefined());
        boolean found = false;
        for (ModelNode type : result.asList()) {
            if ("core-address".equals(type.asString())) {
                found = true;
                break;
            }
        }
        Assert.assertTrue(found);

        op = getSubsystemOperation("read-children-names");
        op.get("child-type").set("core-address");
        result = execute(op, true);
        Assert.assertTrue(result.isDefined());
        found = false;
        for (ModelNode address : result.asList()) {
            if (getAddress().equals(address.asString())) {
                found = true;
                break;
            }
        }
        Assert.assertTrue(found);

        op = getSubsystemOperation("read-children-resources");
        op.get("child-type").set("core-address");
        result = execute(op, true);
        Assert.assertTrue(result.isDefined());
        found = false;
        for (Property address : result.asPropertyList()) {
            if (getAddress().equals(address.getName())) {
                found = true;
                break;
            }
        }
        Assert.assertTrue(found);
    }

    @Test
    public void testAddressGlobalOperations() throws Exception {

        ModelNode op = getAddressOperation("read-children-types");
        op.get("child-type").set("core-address");
        ModelNode result = execute(op, true);
        Assert.assertTrue(result.isDefined());
        Assert.assertEquals(1, result.asInt());

        op = getAddressOperation("read-children-names");
        op.get("child-type").set("role");
        result = execute(op, true);
        Assert.assertTrue(result.isDefined());
        Assert.assertEquals(1, result.asInt());

        op = getAddressOperation("read-children-resources");
        op.get("child-type").set("role");
        result = execute(op, true);
        Assert.assertTrue(result.isDefined());
        Assert.assertEquals(1, result.asInt());
    }

    @Test
    public void testReadResource() throws Exception {

        ModelNode op = getAddressOperation("read-resource");
        op.get("include-runtime").set(true);
        op.get("recursive").set(true);
        ModelNode result = execute(op, true);
        Assert.assertEquals(ModelType.OBJECT, result.getType());

        Assert.assertEquals(ModelType.OBJECT, result.get("role").getType());

        Assert.assertEquals(ModelType.INT, result.get("number-of-pages").getType());

        Assert.assertEquals(ModelType.LONG, result.get("number-of-bytes-per-page").getType());
        Assert.assertEquals(ModelType.LIST, result.get("binding-names").getType());
        boolean foundMain = false;
        boolean foundOther = false;
        for (ModelNode node : result.get("binding-names").asList()) {
            if (getQueueName().equals(node.asString())) {
                Assert.assertFalse(foundMain);
                foundMain = true;
            } else if (getOtherQueueName().equals(node.asString())) {
                Assert.assertFalse(foundOther);
                foundOther = true;
            }
        }
        Assert.assertTrue(foundMain);
        Assert.assertTrue(foundOther);
    }

    private ModelNode getSubsystemOperation(String operationName) {
        return getEmptyOperation(operationName, jmsOperations.getServerAddress().clone());
    }

    private ModelNode getAddressOperation(String operationName) {
        final ModelNode address = jmsOperations.getServerAddress().clone();
        address.add("core-address", getAddress());
        return getEmptyOperation(operationName, address);
    }


    private ModelNode execute(final ModelNode op, final boolean expectSuccess) throws IOException {
        ModelNode response = managementClient.getControllerClient().execute(op);
        final String outcome = response.get("outcome").asString();
        if (expectSuccess) {
            /*if (!"success".equals(outcome)) {
                System.out.println(response);
            }*/
            Assert.assertEquals("success", outcome);
            return response.get("result");
        } else {
            /*if ("success".equals(outcome)) {
               System.out.println(response);
            }*/
            Assert.assertEquals("failed", outcome);
            return response.get("failure-description");
        }
    }

    private static String getAddress() {
        return AddressControlManagementTestCase.class.getSimpleName() + count;
    }

    private static String getQueueName() {
        return getAddress();
    }

    private static String getOtherQueueName() {
        return getAddress() + "other";
    }
}
