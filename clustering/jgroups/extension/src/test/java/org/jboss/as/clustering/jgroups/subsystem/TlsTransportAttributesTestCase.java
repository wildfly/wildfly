/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.jgroups.subsystem;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ACCESS_CONSTRAINTS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ATTRIBUTES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_DESCRIPTION_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SENSITIVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.List;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for TLS-related attributes on JGroups transports.
 */
public class TlsTransportAttributesTestCase extends OperationTestCaseBase {

    /**
     * Verifies that ssl context attributes are available on TCP transport (unsecured stack without SSL configured).
     */
    @Test
    public void testTcpSslContextAttributesAvailable() throws Exception {
        KernelServices services = this.buildKernelServices();

        // server-ssl-context should be readable (undefined when not set)
        ModelNode result = services.executeOperation(getTransportReadOperation("unsecured", "TCP", SecurableSocketTransportResourceDefinitionRegistrar.SERVER_SSL_CONTEXT));
        Assert.assertEquals(result.get(FAILURE_DESCRIPTION).asString(), SUCCESS, result.get(OUTCOME).asString());
        Assert.assertFalse("server-ssl-context should be undefined when not set", result.get(RESULT).isDefined());

        // client-ssl-context should be readable (undefined when not set)
        result = services.executeOperation(getTransportReadOperation("unsecured", "TCP", SecurableSocketTransportResourceDefinitionRegistrar.CLIENT_SSL_CONTEXT));
        Assert.assertEquals(result.get(FAILURE_DESCRIPTION).asString(), SUCCESS, result.get(OUTCOME).asString());
        Assert.assertFalse("client-ssl-context should be undefined when not set", result.get(RESULT).isDefined());
    }

    /**
     * Verifies that ssl context attributes are not available on UDP transport.
     */
    @Test
    public void testUdpSslContextAttributesNotAvailable() throws Exception {
        KernelServices services = this.buildKernelServices();

        // server-ssl-context should not exist on UDP transport
        ModelNode result = services.executeOperation(getTransportReadOperation("minimal", "UDP", SecurableSocketTransportResourceDefinitionRegistrar.SERVER_SSL_CONTEXT));
        Assert.assertEquals(FAILED, result.get(OUTCOME).asString());
        assertThat("Should fail with unknown attribute error", result.get(FAILURE_DESCRIPTION).asString(), containsString("WFLYCTL0201"));

        // client-ssl-context should not exist on UDP transport
        result = services.executeOperation(getTransportReadOperation("minimal", "UDP", SecurableSocketTransportResourceDefinitionRegistrar.CLIENT_SSL_CONTEXT));
        Assert.assertEquals(FAILED, result.get(OUTCOME).asString());
        assertThat("Should fail with unknown attribute error", result.get(FAILURE_DESCRIPTION).asString(), containsString("WFLYCTL0201"));
    }

    /**
     * Verifies that ssl context attributes are not available on TCP_NIO2 transport.
     */
    @Test
    public void testTcpNio2SslContextAttributesNotAvailable() throws Exception {
        KernelServices services = this.buildKernelServices();

        // server-ssl-context should not exist on TCP_NIO2 transport
        ModelNode result = services.executeOperation(getTransportReadOperation("nio2", "TCP_NIO2", SecurableSocketTransportResourceDefinitionRegistrar.SERVER_SSL_CONTEXT));
        Assert.assertEquals(FAILED, result.get(OUTCOME).asString());
        assertThat("Should fail with unknown attribute error", result.get(FAILURE_DESCRIPTION).asString(), containsString("WFLYCTL0201"));

        // client-ssl-context should not exist on TCP_NIO2 transport
        result = services.executeOperation(getTransportReadOperation("nio2", "TCP_NIO2", SecurableSocketTransportResourceDefinitionRegistrar.CLIENT_SSL_CONTEXT));
        Assert.assertEquals(FAILED, result.get(OUTCOME).asString());
        assertThat("Should fail with unknown attribute error", result.get(FAILURE_DESCRIPTION).asString(), containsString("WFLYCTL0201"));
    }

    /**
     * Verifies that SSL_REF RBAC constraint is set for ssl context attributes on TCP transport.
     */
    @Test
    public void testSslContextAccessConstraints() throws Exception {
        KernelServices services = this.buildKernelServices();

        ModelNode op = Util.createOperation(READ_RESOURCE_DESCRIPTION_OPERATION, getTransportAddress("unsecured", "TCP"));
        ModelNode result = services.executeOperation(op);
        Assert.assertEquals(result.get(FAILURE_DESCRIPTION).asString(), SUCCESS, result.get(OUTCOME).asString());

        ModelNode attributes = result.get(RESULT).get(ATTRIBUTES);
        for (AttributeDefinition attribute : List.of(SecurableSocketTransportResourceDefinitionRegistrar.CLIENT_SSL_CONTEXT, SecurableSocketTransportResourceDefinitionRegistrar.SERVER_SSL_CONTEXT)) {
            String attrName = attribute.getName();
            ModelNode accessConstraints = attributes.get(attrName).get(ACCESS_CONSTRAINTS);
            Assert.assertTrue("Attribute " + attrName + " should have access-constraints", accessConstraints.isDefined());
            ModelNode sslRef = accessConstraints.get(SENSITIVE).get("ssl-ref");
            Assert.assertTrue("Attribute " + attrName + " should have ssl-ref constraint", sslRef.isDefined());
            Assert.assertEquals("Attribute " + attrName + " should have core type constraint", "core", sslRef.get(TYPE).asString());
        }
    }
}
