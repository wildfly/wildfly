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

package org.wildfly.extension.batch;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.TimeUnit;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.client.Operation;
import org.jboss.as.controller.client.helpers.Operations.CompositeOperationBuilder;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.SubsystemOperations;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Test;

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
        return new AdditionalInitialization();
    }

    @Test
    public void testFailure() throws Exception {
        final KernelServices kernelServices = boot();

        // Add another named job-repository
        ModelNode op = SubsystemOperations.createAddOperation(createAddress(JobRepositoryDefinition.NAME, "foo"));
        ModelNode result = kernelServices.executeOperation(op);
        Assert.assertFalse(SubsystemOperations.isSuccessfulOutcome(result));

        // Add another named thread-pool
        op = SubsystemOperations.createAddOperation(createAddress(BatchConstants.THREAD_POOL, "foo"));
        result = kernelServices.executeOperation(op);
        Assert.assertFalse(SubsystemOperations.isSuccessfulOutcome(result));
    }

    @Test
    public void testRemoveThreadPool() throws Exception {
        final KernelServices kernelServices = boot(getSubsystemXml("/minimal-subsystem.xml"));
        final ModelNode address = createAddress(BatchSubsystemDefinition.THREAD_POOL_PATH);
        // Remove the thread pool
        final ModelNode removeOp = SubsystemOperations.createRemoveOperation(address);
        executeOperation(kernelServices, removeOp);

        // Reboot with a default thread pool
        String marshalledXml = kernelServices.getPersistedSubsystemXml();
        try {
            boot(marshalledXml);
            Assert.fail("Should be missing <thread-pool/>");
        } catch (XMLStreamException ignore) {
        }

        // Add back a thread-pool, must be named batch
        final ModelNode addOp = SubsystemOperations.createAddOperation(address);
        addOp.get("max-threads").set(10);
        final ModelNode keepAlive = addOp.get("keepalive-time");
        keepAlive.get("time").set(100L);
        keepAlive.get("unit").set(TimeUnit.MILLISECONDS.toString());
        executeOperation(kernelServices, addOp);

        // Get the serialized output and boot
        marshalledXml = kernelServices.getPersistedSubsystemXml();
        try {
            final KernelServices k = boot(marshalledXml);
            Assert.assertTrue(k.isSuccessfulBoot());
        } catch (XMLStreamException e) {
            final StringWriter writer = new StringWriter();
            e.printStackTrace(new PrintWriter(writer));
            Assert.fail("Failed to parse XML; " + writer.toString());
        }

        // Remove and add in a composite operation
        final Operation compositeOp = CompositeOperationBuilder.create()
                .addStep(removeOp)
                .addStep(addOp)
                .build();
        executeOperation(kernelServices, compositeOp);

        // Get the serialized output and boot
        marshalledXml = kernelServices.getPersistedSubsystemXml();
        try {
            final KernelServices k = boot(marshalledXml);
            Assert.assertTrue(k.isSuccessfulBoot());
        } catch (XMLStreamException e) {
            final StringWriter writer = new StringWriter();
            e.printStackTrace(new PrintWriter(writer));
            Assert.fail("Failed to parse XML; " + writer.toString());
        }
    }

    @Test
    public void testAddSubsystem() throws Exception {
        // Boot with no subsystem
        final KernelServices kernelServices = boot(null);
        // Create the base subsystem address
        final ModelNode subsystemAddress = createAddress(null);
        final ModelNode addSubsystemOp = SubsystemOperations.createAddOperation(subsystemAddress);

        addSubsystemOp.get(BatchSubsystemDefinition.JOB_REPOSITORY_TYPE.getName()).set("in-memory");

        final ModelNode threadPool = addSubsystemOp.get(BatchConstants.THREAD_POOL, BatchConstants.THREAD_POOL_NAME);
        threadPool.get("max-threads").set(10);
        final ModelNode keepAlive = threadPool.get("keepalive-time");
        keepAlive.get("time").set(100L);
        keepAlive.get("unit").set(TimeUnit.MILLISECONDS.toString());

        // Execute the add operation
        executeOperation(kernelServices, addSubsystemOp);
    }

    @Test
    public void testRemoveSubsystem() throws Exception {
        final KernelServices kernelServices = boot();
        final ModelNode removeSubsystemOp = SubsystemOperations.createRemoveOperation(createAddress(null));
        executeOperation(kernelServices, removeSubsystemOp);
    }
}
