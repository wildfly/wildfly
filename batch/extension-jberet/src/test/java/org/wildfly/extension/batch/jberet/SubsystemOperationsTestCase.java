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

package org.wildfly.extension.batch.jberet;

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
import org.junit.Test;
import org.wildfly.extension.batch.jberet.job.repository.InMemoryJobRepositoryDefinition;

public class SubsystemOperationsTestCase extends AbstractBatchTestCase {

    public SubsystemOperationsTestCase() {
        super(BatchSubsystemDefinition.NAME, new BatchSubsystemExtension());
    }

    @Override
    protected void standardSubsystemTest(final String configId) throws Exception {
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
                registerCapabilities(capabilityRegistry, "org.wildfly.batch.thread.pool.new-job-repo");
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
}
