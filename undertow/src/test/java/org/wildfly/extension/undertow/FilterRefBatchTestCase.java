/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Map;

import org.jboss.as.controller.Feature;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.KernelServicesBuilder;
import org.jboss.as.version.Stability;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Test;
import org.wildfly.extension.undertow.filters.FilterDefinitions;
import org.wildfly.extension.undertow.filters.FilterRefDefinition;
import org.wildfly.extension.undertow.filters.ResponseHeaderFilterDefinition;

/**
 * Reproducer for WFLY-21767.
 * Composite (batch) operation that first induces a reload-required state then adds a response-header filter and a filter-ref should succeed.
 *
 * @author Radoslav Husar
 */
public class FilterRefBatchTestCase extends AbstractUndertowSubsystemTestCase {

    private static final PathAddress SUBSYSTEM_ADDRESS = PathAddress.pathAddress(UndertowRootDefinition.REGISTRATION.getPathElement());

    public FilterRefBatchTestCase() {
        super(Feature.map(UndertowSubsystemSchema.CURRENT).get(Stability.DEFAULT));
    }

    private void batchFilterRefAfterWriteAttribute(ModelNode writeAttribute) throws Exception {
        KernelServicesBuilder builder = createKernelServicesBuilder(new RuntimeInitialization(this.values, super.schema)).setSubsystemXml(getSubsystemXml());
        KernelServices services = builder.build();
        if (!services.isSuccessfulBoot()) {
            Throwable t = services.getBootError();
            Assert.fail("Boot unsuccessful: " + (t != null ? t.toString() : "no boot error provided"));
        }
        try {
            PathAddress filterAddress = SUBSYSTEM_ADDRESS.append(FilterDefinitions.REGISTRATION.getPathElement()).append(PathElement.pathElement(ResponseHeaderFilterDefinition.REGISTRATION.getPathElement().getKey(), "connection-close-header"));
            PathAddress filterRefAddress = SUBSYSTEM_ADDRESS.append(PathElement.pathElement(ServerDefinition.REGISTRATION.getPathElement().getKey(), "some-server")).append(PathElement.pathElement(HostDefinition.REGISTRATION.getPathElement().getKey(), "other-host")).append(PathElement.pathElement(FilterRefDefinition.REGISTRATION.getPathElement().getKey(), "connection-close-header"));

            ModelNode addFilter = Util.createAddOperation(filterAddress, Map.of(ResponseHeaderFilterDefinition.NAME.getName(), new ModelNode("Connection"), ResponseHeaderFilterDefinition.VALUE.getName(), new ModelNode("close")));

            ModelNode composite = Util.createCompositeOperation(List.of(writeAttribute, addFilter, Util.createAddOperation(filterRefAddress)));
            ModelNode result = services.executeOperation(composite);
            assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());
        } finally {
            services.shutdown();
        }
    }

    @Test
    public void testBatchFilterRefAfterReloadRequiredWriteAttribute() throws Exception {
        // Servlet-container level change — the original reproducer from WFLY-21767
        batchFilterRefAfterWriteAttribute(Util.getWriteAttributeOperation(SUBSYSTEM_ADDRESS.append(PathElement.pathElement(ServletContainerDefinition.REGISTRATION.getPathElement().getKey(), "myContainer")), ServletContainerDefinition.DEFAULT_SESSION_TIMEOUT.getName(), 60));
        // Subsystem level change
        batchFilterRefAfterWriteAttribute(Util.getWriteAttributeOperation(SUBSYSTEM_ADDRESS, UndertowRootDefinition.INSTANCE_ID.getName(), "foobar"));
        // Server level change — worked before the regression as well
        batchFilterRefAfterWriteAttribute(Util.getWriteAttributeOperation(SUBSYSTEM_ADDRESS.append(PathElement.pathElement(ServerDefinition.REGISTRATION.getPathElement().getKey(), "some-server")), ServerDefinition.DEFAULT_HOST.getName(), "test"));
    }

    @Override
    public void testSubsystem() {
        // Ignore - this subsystem needs cleanup since AbstractUndertowSubsystemTestCase leaks these into each subclass!
    }

    @Override
    public void testRuntime() {
        // Ignore - this subsystem needs cleanup since AbstractUndertowSubsystemTestCase leaks these into each subclass!
    }

    @Override
    public void testSchema() {
        // Ignore - this subsystem needs cleanup since AbstractUndertowSubsystemTestCase leaks these into each subclass!
    }
}
