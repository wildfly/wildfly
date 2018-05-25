/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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
package org.jboss.as.clustering.jgroups.subsystem;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;

import org.jboss.as.controller.ExpressionResolver;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test case for testing individual management operations.
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class OperationsTestCase extends OperationTestCaseBase {

    /**
     * Tests access to subsystem attributes
     */
    @Test
    public void testSubsystemReadWriteOperations() throws Exception {

        KernelServices services = this.buildKernelServices();

        // read the default stack
        ModelNode result = services.executeOperation(getSubsystemReadOperation(JGroupsSubsystemResourceDefinition.Attribute.DEFAULT_CHANNEL));
        Assert.assertEquals(result.get(FAILURE_DESCRIPTION).asString(), SUCCESS, result.get(OUTCOME).asString());
        Assert.assertEquals("ee", result.get(RESULT).asString());

        // write the default stack
        result = services.executeOperation(getSubsystemWriteOperation(JGroupsSubsystemResourceDefinition.Attribute.DEFAULT_CHANNEL, "bridge"));
        Assert.assertEquals(result.get(FAILURE_DESCRIPTION).asString(), SUCCESS, result.get(OUTCOME).asString());

        // re-read the default stack
        result = services.executeOperation(getSubsystemReadOperation(JGroupsSubsystemResourceDefinition.Attribute.DEFAULT_CHANNEL));
        Assert.assertEquals(result.get(FAILURE_DESCRIPTION).asString(), SUCCESS, result.get(OUTCOME).asString());
        Assert.assertEquals("bridge", result.get(RESULT).asString());
    }

    /**
     * Tests access to transport attributes
     */
    @Test
    public void testTransportReadWriteOperation() throws Exception {

        KernelServices services = this.buildKernelServices();

        // read the transport rack attribute
        ModelNode result = services.executeOperation(getTransportReadOperation("maximal", "TCP", TransportResourceDefinition.Attribute.RACK));
        Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());
        Assert.assertEquals("rack1", ExpressionResolver.TEST_RESOLVER.resolveExpressions(result.get(RESULT)).asString());

        // write the rack attribute
        result = services.executeOperation(getTransportWriteOperation("maximal", "TCP", TransportResourceDefinition.Attribute.RACK, "new-rack"));
        Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());

        // re-read the rack attribute
        result = services.executeOperation(getTransportReadOperation("maximal", "TCP", TransportResourceDefinition.Attribute.RACK));
        Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());
        Assert.assertEquals("new-rack", result.get(RESULT).asString());
    }

    @Test
    public void testTransportReadWriteWithParameters() throws Exception {
        // Parse and install the XML into the controller
        KernelServices services = this.buildKernelServices();
        Assert.assertTrue("Could not create services", services.isSuccessfulBoot());

        // add a protocol stack specifying TRANSPORT and PROTOCOLS parameters
        ModelNode result = services.executeOperation(getProtocolStackAddOperationWithParameters("maximal2"));
        Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());

        // write the rack attribute
        result = services.executeOperation(getTransportWriteOperation("maximal", "TCP", TransportResourceDefinition.Attribute.RACK, "new-rack"));
        Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());

        // re-read the rack attribute
        result = services.executeOperation(getTransportReadOperation("maximal", "TCP", TransportResourceDefinition.Attribute.RACK));
        Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());
        Assert.assertEquals("new-rack", result.get(RESULT).asString());
    }

    @Test
    public void testTransportPropertyReadWriteOperation() throws Exception {
        KernelServices services = this.buildKernelServices();

        // read the enable_bundling transport property
        ModelNode result = services.executeOperation(getTransportGetPropertyOperation("maximal", "TCP", "enable_bundling"));
        Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());
        Assert.assertEquals("true", ExpressionResolver.TEST_RESOLVER.resolveExpressions(result.get(RESULT)).asString());

        // write the enable_bundling transport property
        result = services.executeOperation(getTransportPutPropertyOperation("maximal", "TCP", "enable_bundling", "false"));
        Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());

        // re-read the enable_bundling transport property
        result = services.executeOperation(getTransportGetPropertyOperation("maximal", "TCP", "enable_bundling"));
        Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());
        Assert.assertEquals("false", result.get(RESULT).asString());

        // remove the enable_bundling transport property
        result = services.executeOperation(getTransportRemovePropertyOperation("maximal", "TCP", "enable_bundling"));
        Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());

        // re-read the enable_bundling transport property
        result = services.executeOperation(getTransportGetPropertyOperation("maximal", "TCP", "enable_bundling"));
        Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());
        Assert.assertFalse(result.get(RESULT).isDefined());

        // Validate that add/read/write/remove via legacy property resource
        result = services.executeOperation(getTransportPropertyAddOperation("maximal", "TCP", "shared", "false"));
        Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());

        result = services.executeOperation(getTransportPropertyReadOperation("maximal", "TCP", "shared"));
        Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());
        Assert.assertEquals("false", result.get(RESULT).asString());

        result = services.executeOperation(getTransportGetPropertyOperation("maximal", "TCP", "shared"));
        Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());
        Assert.assertEquals("false", result.get(RESULT).asString());

        result = services.executeOperation(getTransportPropertyWriteOperation("maximal", "TCP", "shared", "true"));
        Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());

        result = services.executeOperation(getTransportPropertyReadOperation("maximal", "TCP", "shared"));
        Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());
        Assert.assertEquals("true", result.get(RESULT).asString());

        result = services.executeOperation(getTransportGetPropertyOperation("maximal", "TCP", "shared"));
        Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());
        Assert.assertEquals("true", result.get(RESULT).asString());

        result = services.executeOperation(getTransportPropertyRemoveOperation("maximal", "TCP", "shared"));
        Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());

        result = services.executeOperation(getTransportGetPropertyOperation("maximal", "TCP", "shared"));
        Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());
        Assert.assertFalse(result.get(RESULT).isDefined());
    }

    @Test
    public void testProtocolReadWriteOperation() throws Exception {
        KernelServices services = this.buildKernelServices();

        // add a protocol stack specifying TRANSPORT and PROTOCOLS parameters
        ModelNode result = services.executeOperation(getProtocolStackAddOperationWithParameters("maximal2"));
        Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());

        // read the socket binding attribute
        result = services.executeOperation(getProtocolReadOperation("maximal", "MPING", SocketBindingProtocolResourceDefinition.Attribute.SOCKET_BINDING));
        Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());
        Assert.assertEquals("jgroups-mping", result.get(RESULT).asString());

        // write the attribute
        result = services.executeOperation(getProtocolWriteOperation("maximal", "MPING", SocketBindingProtocolResourceDefinition.Attribute.SOCKET_BINDING, "new-socket-binding"));
        Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());

        // re-read the attribute
        result = services.executeOperation(getProtocolReadOperation("maximal", "MPING", SocketBindingProtocolResourceDefinition.Attribute.SOCKET_BINDING));
        Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());
        Assert.assertEquals("new-socket-binding", result.get(RESULT).asString());
    }

    @Test
    public void testProtocolPropertyReadWriteOperation() throws Exception {
        KernelServices services = this.buildKernelServices();

        // read the name protocol property
        ModelNode result = services.executeOperation(getProtocolGetPropertyOperation("maximal", "MPING", "name"));
        Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());
        Assert.assertEquals("value", ExpressionResolver.TEST_RESOLVER.resolveExpressions(result.get(RESULT)).asString());

        // write the property
        result = services.executeOperation(getProtocolPutPropertyOperation("maximal", "MPING", "name", "new-value"));
        Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());

        // re-read the property
        result = services.executeOperation(getProtocolGetPropertyOperation("maximal", "MPING", "name"));
        Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());
        Assert.assertEquals("new-value", result.get(RESULT).asString());

        // remove the property
        result = services.executeOperation(getProtocolRemovePropertyOperation("maximal", "MPING", "name"));
        Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());

        // re-read the property
        result = services.executeOperation(getProtocolGetPropertyOperation("maximal", "MPING", "name"));
        Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());
        Assert.assertFalse(result.get(RESULT).isDefined());

        // Validate property add/read/write/remove via legacy property resource
        result = services.executeOperation(getProtocolPropertyAddOperation("maximal", "MPING", "async_discovery", "false"));
        Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());

        result = services.executeOperation(getProtocolPropertyReadOperation("maximal", "MPING", "async_discovery"));
        Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());
        Assert.assertEquals("false", result.get(RESULT).asString());

        result = services.executeOperation(getProtocolGetPropertyOperation("maximal", "MPING", "async_discovery"));
        Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());
        Assert.assertEquals("false", result.get(RESULT).asString());

        result = services.executeOperation(getProtocolPropertyWriteOperation("maximal", "MPING", "async_discovery", "true"));
        Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());

        result = services.executeOperation(getProtocolPropertyReadOperation("maximal", "MPING", "async_discovery"));
        Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());
        Assert.assertEquals("true", result.get(RESULT).asString());

        result = services.executeOperation(getProtocolGetPropertyOperation("maximal", "MPING", "async_discovery"));
        Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());
        Assert.assertEquals("true", result.get(RESULT).asString());

        result = services.executeOperation(getProtocolPropertyRemoveOperation("maximal", "MPING", "async_discovery"));
        Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());

        result = services.executeOperation(getProtocolGetPropertyOperation("maximal", "MPING", "async_discovery"));
        Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());
        Assert.assertFalse(result.get(RESULT).isDefined());
    }

    @Test
    public void testLegacyProtocolAddRemoveOperation() throws Exception {
        KernelServices services = this.buildKernelServices();

        testProtocolAddRemoveOperation(services, "MERGE2");
        testProtocolAddRemoveOperation(services, "pbcast.NAKACK");
        testProtocolAddRemoveOperation(services, "UNICAST2");
    }

    private static void testProtocolAddRemoveOperation(KernelServices services, String protocol) {

        ModelNode result = services.executeOperation(getProtocolAddOperation("minimal", protocol));
        Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());

        result = services.executeOperation(getProtocolRemoveOperation("minimal", protocol));
        Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());
    }
}