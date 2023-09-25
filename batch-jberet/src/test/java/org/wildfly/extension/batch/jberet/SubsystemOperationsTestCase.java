/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.batch.jberet;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.jboss.as.controller.capability.registry.RuntimeCapabilityRegistry;
import org.jboss.as.controller.client.Operation;
import org.jboss.as.controller.client.helpers.Operations.CompositeOperationBuilder;
import org.jboss.as.controller.extension.ExtensionRegistry;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.SubsystemOperations;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Test;
import org.wildfly.extension.batch.jberet.job.repository.CommonAttributes;
import org.wildfly.extension.batch.jberet.job.repository.InMemoryJobRepositoryDefinition;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

public class SubsystemOperationsTestCase extends AbstractBatchTestCase {

    public SubsystemOperationsTestCase() {
        super(BatchSubsystemDefinition.NAME, new BatchSubsystemExtension());
    }

    @Override
    protected void standardSubsystemTest(final String configId) {
        // do nothing as this is not a subsystem parsing test
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("/default-subsystem.xml");
    }

    @Override
    protected AdditionalInitialization createAdditionalInitialization() {
        return new AdditionalInitialization() {

            @Override
            protected void initializeExtraSubystemsAndModel(ExtensionRegistry extensionRegistry, Resource rootResource, ManagementResourceRegistration rootRegistration, RuntimeCapabilityRegistry capabilityRegistry) {
                super.initializeExtraSubystemsAndModel(extensionRegistry, rootResource, rootRegistration, capabilityRegistry);
                registerCapabilities(capabilityRegistry,
                        "org.wildfly.batch.thread.pool.new-job-repo",
                        "org.wildfly.transactions.global-default-local-provider",
                        "org.wildfly.data-source.ExampleDS");
            }
        };
    }

    @Test
    public void testThreadPoolChange() throws Exception {
        final KernelServices kernelServices = boot();

        final CompositeOperationBuilder compositeOp = CompositeOperationBuilder.create();

        // Add a new thread-pool
        final ModelNode address = createAddress("thread-pool", "test-pool");
        final ModelNode addOp = SubsystemOperations.createAddOperation(address);
        addOp.get("max-threads").set(10L);
        final ModelNode keepAlive = addOp.get("keepalive-time");
        keepAlive.get("time").set(100L);
        keepAlive.get("unit").set(TimeUnit.MILLISECONDS.toString());
        compositeOp.addStep(addOp);


        // Write the new default
        compositeOp.addStep(SubsystemOperations.createWriteAttributeOperation(createAddress(null), "default-thread-pool", "test-pool"));

        executeOperation(kernelServices, compositeOp.build());
    }

    @Test
    public void testJobRepositoryChange() throws Exception {
        final KernelServices kernelServices = boot();

        final CompositeOperationBuilder compositeOp = CompositeOperationBuilder.create();

        // Add a new thread-pool
        final ModelNode address = createAddress(InMemoryJobRepositoryDefinition.NAME, "new-job-repo");
        compositeOp.addStep(SubsystemOperations.createAddOperation(address));


        // Write the new default
        compositeOp.addStep(SubsystemOperations.createWriteAttributeOperation(createAddress(null), "default-thread-pool", "new-job-repo"));

        executeOperation(kernelServices, compositeOp.build());
    }

    @Test
    public void testAddRemoveThreadPool() throws Exception {
        final KernelServices kernelServices = boot(getSubsystemXml("/minimal-subsystem.xml"));

        final ModelNode address = createAddress("thread-pool", "test-pool");
        final ModelNode addOp = SubsystemOperations.createAddOperation(address);
        addOp.get("max-threads").set(10L);
        final ModelNode keepAlive = addOp.get("keepalive-time");
        keepAlive.get("time").set(100L);
        keepAlive.get("unit").set(TimeUnit.MILLISECONDS.toString());
        executeOperation(kernelServices, addOp);

        final ModelNode removeOp = SubsystemOperations.createRemoveOperation(address);
        executeOperation(kernelServices, removeOp);

        // Add one more time to test a composite operation
        executeOperation(kernelServices, addOp);

        // Remove and add in a composite operation
        final Operation compositeOp = CompositeOperationBuilder.create()
                .addStep(removeOp)
                .addStep(addOp)
                .build();
        executeOperation(kernelServices, compositeOp);
    }

    @Test
    public void testAddSubsystem() throws Exception {
        // Boot with no subsystem
        final KernelServices kernelServices = boot(null);
        final CompositeOperationBuilder operationBuilder = CompositeOperationBuilder.create();
        // Create the base subsystem address
        final ModelNode subsystemAddress = createAddress(null);
        final ModelNode subsystemAddOp = SubsystemOperations.createAddOperation(subsystemAddress);
        subsystemAddOp.get("default-job-repository").set("in-memory");
        subsystemAddOp.get("default-thread-pool").set("batch");
        operationBuilder.addStep(subsystemAddOp);

        // Add a job repository
        operationBuilder.addStep(SubsystemOperations.createAddOperation(createAddress(InMemoryJobRepositoryDefinition.NAME, "in-memory")));

        final ModelNode threadPool = SubsystemOperations.createAddOperation(createAddress("thread-pool", "batch"));
        threadPool.get("max-threads").set(10);
        final ModelNode keepAlive = threadPool.get("keepalive-time");
        keepAlive.get("time").set(100L);
        keepAlive.get("unit").set(TimeUnit.MILLISECONDS.toString());
        operationBuilder.addStep(threadPool);

        // Execute the add operation
        executeOperation(kernelServices, operationBuilder.build());
    }

    @Test
    public void testRemoveSubsystem() throws Exception {
        final KernelServices kernelServices = boot();
        final ModelNode removeSubsystemOp = SubsystemOperations.createRemoveOperation(createAddress(null));
        executeOperation(kernelServices, removeSubsystemOp);
    }

    @Test
    public void testEnums() {
        for (Element e : Element.values()) {
            assertEquals(e, Element.forName(e.getLocalName()));
        }
        assertEquals(Element.UNKNOWN, Element.forName("zzz"));

        for (Attribute e : Attribute.values()) {
            assertEquals(e, Attribute.forName(e.getLocalName()));
        }
        assertEquals(Attribute.UNKNOWN, Attribute.forName("xxx"));

        for (Namespace e : Namespace.values()) {
            assertEquals(e, Namespace.forUri(e.getUriString()));
        }
        assertEquals(Namespace.UNKNOWN, Namespace.forUri("yyy"));
    }

    @Test
    public void testWriteExecutionRecordsLimit() throws Exception {
        String[] resourcePath = new String[] {SUBSYSTEM, BatchSubsystemDefinition.NAME, InMemoryJobRepositoryDefinition.NAME, "in-memory"};

        final KernelServices kernelServices = boot();
        Assert.assertEquals(200, kernelServices.readWholeModel().get(resourcePath).get("execution-records-limit").asInt());

        final ModelNode address = createAddress(InMemoryJobRepositoryDefinition.NAME, "in-memory");
        ModelNode operation = SubsystemOperations.createWriteAttributeOperation(address, CommonAttributes.EXECUTION_RECORDS_LIMIT, 250);

        executeOperation(kernelServices, operation);

        Assert.assertEquals(250, kernelServices.readWholeModel().get(resourcePath).get("execution-records-limit").asInt());
    }
}
