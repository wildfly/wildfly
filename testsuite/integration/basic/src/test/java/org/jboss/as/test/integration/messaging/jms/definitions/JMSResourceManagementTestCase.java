/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.messaging.jms.definitions;

import static org.jboss.as.controller.operations.common.Util.getEmptyOperation;
import static org.jboss.shrinkwrap.api.ArchivePaths.create;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.IOException;
import java.util.List;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.common.jms.JMSOperations;
import org.jboss.as.test.integration.common.jms.JMSOperationsProvider;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2013 Red Hat inc.
 */
@RunAsClient
@RunWith(Arquillian.class)
public class JMSResourceManagementTestCase {
    private static final Logger LOGGER = Logger.getLogger(JMSResourceManagementTestCase.class);

    @ContainerResource
    private ManagementClient managementClient;

    private JMSOperations jmsOperations;

    @Before
    public void setUp() {
        jmsOperations = JMSOperationsProvider.getInstance(managementClient);
    }

    @Deployment
    public static JavaArchive createArchive() {
        JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "JMSResourceDefinitionsTestCase.jar")
                .addPackage(MessagingBean.class.getPackage())
                .addAsManifestResource(
                        MessagingBean.class.getPackage(), "ejb-jar.xml", "ejb-jar.xml")
                .addAsManifestResource(
                        EmptyAsset.INSTANCE,
                        create("beans.xml"));

        return archive;
    }

    @Test
    public void testManagementOfDestinations() throws Exception {
        ModelNode readResourceWithRuntime = getOperation("jms-queue", "myQueue1", "read-resource");
        readResourceWithRuntime.get("include-runtime").set(true);
        ModelNode result = execute(readResourceWithRuntime, true);
        assertEquals(true, result.get("durable").asBoolean());
        assertFalse(result.hasDefined("selector"));

        // injectedQueue3 has been declared in the annotation with selector => color = 'red'
        // and durable = false
        readResourceWithRuntime = getOperation("jms-queue", "myQueue2", "read-resource");
        readResourceWithRuntime.get("include-runtime").set(true);
        result = execute(readResourceWithRuntime, true);
        assertEquals(false, result.get("durable").asBoolean());
        assertEquals("color = 'red'", result.get("selector").asString());

        // injectedQueue3 has been declared in the ejb-jar.xml with selector => color = 'blue'
        // and durable => false
        readResourceWithRuntime = getOperation("jms-queue", "myQueue3", "read-resource");
        readResourceWithRuntime.get("include-runtime").set(true);
        result = execute(readResourceWithRuntime, true);
        assertEquals(false, result.get("durable").asBoolean());
        assertEquals("color = 'blue'", result.get("selector").asString());

        // injectedTopic1 has been declared in the annotation
        readResourceWithRuntime = getOperation("jms-topic", "myTopic1", "read-resource");
        readResourceWithRuntime.get("include-runtime").set(true);
        execute(readResourceWithRuntime, true);

        // injectedTopic2 has been declared in the ejb-jar.xml
        readResourceWithRuntime = getOperation("jms-topic", "myTopic2", "read-resource");
        readResourceWithRuntime.get("include-runtime").set(true);
        execute(readResourceWithRuntime, true);
    }

    @Test
    public void testManagementOfConnectionFactories() throws Exception {
        ModelNode readResourceWithRuntime = getOperation("pooled-connection-factory", "*", "read-resource-description");
        readResourceWithRuntime.get("include-runtime").set(true);
        readResourceWithRuntime.get("operations").set(true);
        execute(readResourceWithRuntime, true);

        readResourceWithRuntime = getOperation("pooled-connection-factory", "*", "read-resource-description");
        readResourceWithRuntime.get("include-runtime").set(true);
        readResourceWithRuntime.get("operations").set(true);
        ModelNode result = execute(readResourceWithRuntime, true);
        //System.out.println("result = " + result.toJSONString(false));

        readResourceWithRuntime = getOperation("pooled-connection-factory", "JMSResourceDefinitionsTestCase_JMSResourceDefinitionsTestCase_java_module/myFactory1", "read-resource");
        readResourceWithRuntime.get("include-runtime").set(true);
        result = execute(readResourceWithRuntime, true);
        //System.out.println("result = " + result.toJSONString(false));
        assertEquals(1, result.get("min-pool-size").asInt());
        assertEquals(2, result.get("max-pool-size").asInt());
        assertEquals(3, result.get("initial-connect-attempts").asInt());
        assertEquals("guest", result.get("user").asString());
        assertEquals("guest", result.get("password").asString());
        assertEquals("myClientID1", result.get("client-id").asString());

        readResourceWithRuntime = getOperation("pooled-connection-factory", "JMSResourceDefinitionsTestCase_MessagingBean_java_comp/env/myFactory2", "read-resource");
        readResourceWithRuntime.get("include-runtime").set(true);
        result = execute(readResourceWithRuntime, true);
        //System.out.println("result = " + result.toJSONString(false));
        assertEquals(0, result.get("min-pool-size").asInt());
        assertEquals(20, result.get("max-pool-size").asInt());
        assertFalse(result.toJSONString(false), result.hasDefined("user"));
        assertFalse(result.hasDefined("password"));
        assertFalse(result.hasDefined("client-id"));

        readResourceWithRuntime = getOperation("pooled-connection-factory", "JMSResourceDefinitionsTestCase_JMSResourceDefinitionsTestCase_java_global/myFactory3", "read-resource");
        readResourceWithRuntime.get("include-runtime").set(true);
        result = execute(readResourceWithRuntime, true);
        assertEquals(3, result.get("min-pool-size").asInt());
        assertEquals(4, result.get("max-pool-size").asInt());
        assertEquals(5, result.get("initial-connect-attempts").asInt());
        assertEquals("guest", result.get("user").asString());
        assertEquals("guest", result.get("password").asString());
        assertFalse(result.hasDefined("client-id"));

        readResourceWithRuntime = getOperation("pooled-connection-factory", "JMSResourceDefinitionsTestCase_JMSResourceDefinitionsTestCase_java_app/myFactory4", "read-resource");
        readResourceWithRuntime.get("include-runtime").set(true);
        result = execute(readResourceWithRuntime, true);
        assertEquals(4, result.get("min-pool-size").asInt());
        assertEquals(5, result.get("max-pool-size").asInt());
        assertEquals(6, result.get("initial-connect-attempts").asInt());
        assertEquals("guest", result.get("user").asString());
        assertEquals("guest", result.get("password").asString());
        assertEquals("myClientID4", result.get("client-id").asString());
    }

    @Test
    public void testRuntimeQueues() throws Exception {
        //Tests https://issues.jboss.org/browse/WFLY-2807
        PathAddress addr = PathAddress.pathAddress("subsystem", "messaging-activemq");
        addr = addr.append("server", "default");
        ModelNode readResource = Util.createEmptyOperation("read-resource", addr);
        readResource.get("recursive").set(true);

        ModelNode result = execute(readResource, true);
        Assert.assertTrue(result.has("runtime-queue"));
        Assert.assertFalse(result.hasDefined("runtime-queue"));

        readResource.get("include-runtime").set(true);
        result = execute(readResource, true);
        Assert.assertTrue(result.hasDefined("runtime-queue"));
        ModelNode queues = result.get("runtime-queue");
        List<Property> propsList = queues.asPropertyList();
        Assert.assertTrue(propsList.size() > 0);
        for (Property prop : propsList) {
            Assert.assertTrue(prop.getValue().isDefined());
        }
    }


    private ModelNode getOperation(String resourceType, String resourceName, String operationName) {
        final ModelNode address = new ModelNode();
        address.add("deployment", "JMSResourceDefinitionsTestCase.jar");
        for (Property prop: jmsOperations.getServerAddress().asPropertyList()) {
            address.add(prop.getName(), prop.getValue().asString());
        }
        address.add(resourceType, resourceName);
        return getEmptyOperation(operationName, address);
    }

    private ModelNode execute(final ModelNode op, final boolean expectSuccess) throws IOException {
        ModelNode response = managementClient.getControllerClient().execute(op);
        final String outcome = response.get("outcome").asString();
        if (expectSuccess) {
            if (!"success".equals(outcome)) {
                LOGGER.trace(response);
            }
            assertEquals("success", outcome);
            return response.get("result");
        } else {
            if ("success".equals(outcome)) {
                LOGGER.trace(response);
            }
            assertEquals("failed", outcome);
            return response.get("failure-description");
        }
    }
}
