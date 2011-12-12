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

package org.jboss.as.test.smoke.embedded.mgmt;

import static org.jboss.as.arquillian.container.Authentication.getCallbackHandler;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ATTRIBUTES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INCLUDE_RUNTIME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PORT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_DESCRIPTION_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RECURSIVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.List;

import junit.framework.Assert;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.protocol.StreamUtils;
import org.jboss.as.test.smoke.modular.utils.ShrinkWrapUtils;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Basic management operation unit test.
 *
 * @author Emanuel Muckenhuber
 */
@RunWith(Arquillian.class)
@RunAsClient
public class BasicOperationsUnitTestCase {

    private ModelControllerClient client;

    @Deployment
    public static Archive<?> getDeployment() {
        return ShrinkWrapUtils.createEmptyJavaArchive("dummy");
    }

    // [ARQ-458] @Before not called with @RunAsClient
    private ModelControllerClient getModelControllerClient() throws UnknownHostException {
        StreamUtils.safeClose(client);
        client = ModelControllerClient.Factory.create(InetAddress.getByName("localhost"), 9999, getCallbackHandler());
        return client;
    }

    @After
    public void tearDown() {
        StreamUtils.safeClose(client);
    }

    @Test
    public void testSocketBindingsWildcards() throws IOException {

        final ModelNode address = new ModelNode();
        address.add("socket-binding-group", "*");
        address.add("socket-binding", "*");
        address.protect();

        final ModelNode operation = new ModelNode();
        operation.get(OP).set(READ_RESOURCE_OPERATION);
        operation.get(OP_ADDR).set(address);

        final ModelNode result = getModelControllerClient().execute(operation);
        Assert.assertTrue(result.hasDefined(RESULT));
        Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());
        final Collection<ModelNode> steps = getSteps(result.get(RESULT));
        Assert.assertFalse(steps.isEmpty());
        for(final ModelNode step : steps) {
            Assert.assertTrue(step.hasDefined(OP_ADDR));
            Assert.assertTrue(step.hasDefined(RESULT));
            Assert.assertEquals(SUCCESS, step.get(OUTCOME).asString());
        }
    }

    @Test
    public void testReadAttributeWildcards() throws IOException {

        final ModelNode address = new ModelNode();
        address.add("socket-binding-group", "*");
        address.add("socket-binding", "*");
        address.protect();

        final ModelNode operation = new ModelNode();
        operation.get(OP).set(READ_ATTRIBUTE_OPERATION);
        operation.get(OP_ADDR).set(address);
        operation.get(NAME).set(PORT);

        final ModelNode result = getModelControllerClient().execute(operation);
        Assert.assertTrue(result.hasDefined(RESULT));
        Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());
        final Collection<ModelNode> steps = getSteps(result.get(RESULT));
        Assert.assertFalse(steps.isEmpty());
        for(final ModelNode step : steps) {
            Assert.assertTrue(step.hasDefined(OP_ADDR));
            Assert.assertTrue(step.hasDefined(RESULT));
            Assert.assertTrue(step.get(RESULT).asInt() > 0);
        }
    }

    @Test
    public void testSocketBindingDescriptions() throws IOException {

        final ModelNode address = new ModelNode();
        address.add("socket-binding-group", "*");
        address.add("socket-binding", "*");
        address.protect();

        final ModelNode operation = new ModelNode();
        operation.get(OP).set(READ_RESOURCE_DESCRIPTION_OPERATION);
        operation.get(OP_ADDR).set(address);

        final ModelNode result = getModelControllerClient().execute(operation);
        Assert.assertTrue(result.hasDefined(RESULT));
        Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());
        final Collection<ModelNode> steps = result.get(RESULT).asList();
        Assert.assertFalse(steps.isEmpty());
        Assert.assertEquals("should only contain a single type", 1, steps.size());
        for(final ModelNode step : steps) {
            Assert.assertTrue(step.hasDefined(OP_ADDR));
            Assert.assertTrue(step.hasDefined(RESULT));
            Assert.assertEquals(SUCCESS, step.get(OUTCOME).asString());
            final ModelNode stepResult = step.get(RESULT);
            Assert.assertTrue(stepResult.hasDefined(DESCRIPTION));
            Assert.assertTrue(stepResult.hasDefined(ATTRIBUTES));
            Assert.assertTrue(stepResult.get(ModelDescriptionConstants.ATTRIBUTES).hasDefined(ModelDescriptionConstants.NAME));
            Assert.assertTrue(stepResult.get(ModelDescriptionConstants.ATTRIBUTES).hasDefined(ModelDescriptionConstants.INTERFACE));
            Assert.assertTrue(stepResult.get(ModelDescriptionConstants.ATTRIBUTES).hasDefined(ModelDescriptionConstants.PORT));
        }
    }

    @Test
    public void testRecursiveReadIncludingRuntime() throws IOException {
        final ModelNode operation = new ModelNode();
        operation.get(OP).set(READ_RESOURCE_OPERATION);
        operation.get(OP_ADDR).setEmptyList();
        operation.get(RECURSIVE).set(true);
        operation.get(INCLUDE_RUNTIME).set(true);

        final ModelNode result = getModelControllerClient().execute(operation);
        Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());
        Assert.assertTrue(result.hasDefined(RESULT));
    }

    @Test
    public void testHttpSocketBinding() throws IOException {
        final ModelNode address = new ModelNode();
        address.add("socket-binding-group", "*");
        address.add("socket-binding", "http");
        address.protect();

        final ModelNode operation = new ModelNode();
        operation.get(OP).set(READ_RESOURCE_OPERATION);
        operation.get(OP_ADDR).set(address);

        final ModelNode result = getModelControllerClient().execute(operation);
        Assert.assertTrue(result.hasDefined(RESULT));
        Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());
        final List<ModelNode> steps = getSteps(result.get(RESULT));
        Assert.assertEquals(1, steps.size());
        final ModelNode httpBinding = steps.get(0);
        Assert.assertEquals(8080, httpBinding.get(RESULT, "port").asInt());

    }

    @Test
    public void testSimpleReadAttribute() throws IOException {
        final ModelNode address = new ModelNode();
        address.add("subsystem", "deployment-scanner");
        address.add("scanner", "default");

        final ModelNode operation = createReadAttributeOperation(address, "path");
        final ModelNode result = getModelControllerClient().execute(operation);
        assertSuccessful(result);

        Assert.assertEquals("deployments", result.get(RESULT).asString());
    }

    @Test
    public void testMetricReadAttribute() throws IOException {
        final ModelNode address = new ModelNode();
        address.add("subsystem", "web");
        address.add("connector", "http");

        final ModelNode operation = createReadAttributeOperation(address, "bytesReceived");
        final ModelNode result = getModelControllerClient().execute(operation);
        assertSuccessful(result);
        Assert.assertTrue(result.asInt() >= 0);
    }

    public void testReadAttributeChild() throws IOException {
        final ModelNode address = new ModelNode();
        address.add("subsystem", "deployment-scanner");

        final ModelNode operation = createReadAttributeOperation(address, "scanner");
        final ModelNode result = getModelControllerClient().execute(operation);
        Assert.assertEquals(FAILED, result.get(OUTCOME));
    }

    static void assertSuccessful(final ModelNode result) {
        Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());
        Assert.assertTrue(result.hasDefined(RESULT));
    }

    static ModelNode createReadAttributeOperation(final ModelNode address, final String attributeName) {
        final ModelNode operation = new ModelNode();
        operation.get(OP).set(READ_ATTRIBUTE_OPERATION);
        operation.get(OP_ADDR).set(address);
        operation.get(NAME).set(attributeName);
        return operation;
    }

    protected static List<ModelNode> getSteps(final ModelNode result) {
        Assert.assertTrue(result.isDefined());
        return result.asList();
    }
}
