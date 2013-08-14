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
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2013 Red Hat inc.
 */
@RunAsClient
@RunWith(Arquillian.class)
public class JMSResourceManagementTestCase {

    @ContainerResource
    private ManagementClient managementClient;

    @Deployment
    public static JavaArchive createArchive() {
        JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "JMSResourceDefinitionsTestCase.jar")
                .addPackage(MessagingBean.class.getPackage())
                .addAsManifestResource(
                        MessagingBean.class.getPackage(), "ejb-jar.xml", "ejb-jar.xml")
                .addAsManifestResource(
                        EmptyAsset.INSTANCE,
                        create("beans.xml"));

        System.out.println("archive = " + archive.toString(true));
        return archive;
    }

    @Test
    public void testManagementOfInjectedResource() throws Exception {
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

    private ModelNode getOperation(String resourceType, String resourceName, String operationName) {
        final ModelNode address = new ModelNode();
        address.add("deployment", "JMSResourceDefinitionsTestCase.jar");
        address.add("subsystem", "messaging");
        address.add("hornetq-server", "default");
        address.add(resourceType, resourceName);
        return getEmptyOperation(operationName, address);
    }

    private ModelNode execute(final ModelNode op, final boolean expectSuccess) throws IOException {
        ModelNode response = managementClient.getControllerClient().execute(op);
        final String outcome = response.get("outcome").asString();
        if (expectSuccess) {
            if (!"success".equals(outcome)) {
                System.out.println(response);
            }
            assertEquals("success", outcome);
            return response.get("result");
        } else {
            if ("success".equals(outcome)) {
                System.out.println(response);
            }
            assertEquals("failed", outcome);
            return response.get("failure-description");
        }
    }
}
