/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.mod_cluster;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_OPERATION_NAMES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;

import java.io.IOException;

import org.jboss.as.clustering.subsystem.AdditionalInitialization;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.network.OutboundSocketBinding;
import org.jboss.as.subsystem.test.AbstractSubsystemTest;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test case for testing proxy operations.
 *
 * @author Radoslav Husar
 */
public class ModClusterOperationsTestCase extends AbstractSubsystemTest {

    public ModClusterOperationsTestCase() {
        super(ModClusterExtension.SUBSYSTEM_NAME, new ModClusterExtension());
    }

    @Test
    public void testProxyOperations() throws Exception {
        KernelServices services = this.buildKernelServices();

        ModelNode op = Util.createOperation(READ_OPERATION_NAMES_OPERATION, getProxyAddress("default"));

        ModelNode result = services.executeOperation(op);
        Assert.assertEquals(result.get(FAILURE_DESCRIPTION).asString(), SUCCESS, result.get(OUTCOME).asString());

        for (ProxyOperation proxyOperation : ProxyOperation.values()) {
            String operationName = proxyOperation.getDefinition().getName();
            Assert.assertTrue(String.format("'%s' operation is not registered at the proxy address", operationName), result.get(RESULT).asList().contains(new ModelNode(operationName)));
        }
    }

    // Addresses

    private static PathAddress getSubsystemAddress() {
        return PathAddress.pathAddress(ModClusterSubsystemResourceDefinition.PATH);
    }

    private static PathAddress getProxyAddress(String proxyName) {
        return getSubsystemAddress().append(ProxyConfigurationResourceDefinition.pathElement(proxyName));
    }

    // Setup

    private String getSubsystemXml() throws IOException {
        return readResource("subsystem-operations.xml");
    }

    private KernelServices buildKernelServices() throws Exception {
        return createKernelServicesBuilder(new AdditionalInitialization().require(OutboundSocketBinding.SERVICE_DESCRIPTOR, "proxy1")).setSubsystemXml(this.getSubsystemXml()).build();
    }
}