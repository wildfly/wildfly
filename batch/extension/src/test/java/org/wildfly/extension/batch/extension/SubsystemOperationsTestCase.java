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

package org.wildfly.extension.batch.extension;

import java.io.IOException;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.SubsystemOperations;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Test;
import org.wildfly.extension.batch.BatchSubsystemDefinition;
import org.wildfly.extension.batch.BatchSubsystemExtension;
import org.wildfly.extension.batch.JobRepositoryDefinition;

public class SubsystemOperationsTestCase extends AbstractSubsystemBaseTest {

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

    @Test
    public void addFailure() throws Exception {
        final KernelServices kernelServices = boot();

        // Add another named job-repository
        ModelNode op = SubsystemOperations.createAddOperation(createAddress(JobRepositoryDefinition.NAME, "foo"));
        ModelNode result = kernelServices.executeOperation(op);
        Assert.assertFalse(SubsystemOperations.isSuccessfulOutcome(result));

        // Add another named thread-pool
        op = SubsystemOperations.createAddOperation(createAddress("thread-pool", "foo"));
        result = kernelServices.executeOperation(op);
        Assert.assertFalse(SubsystemOperations.isSuccessfulOutcome(result));
    }

    @Test
    public void addComposite() throws Exception {
        final KernelServices kernelServices = boot();

        final ModelNode jobRepositoryAddress = createAddress(JobRepositoryDefinition.IN_MEMORY.getPathElement());

        ModelNode op = SubsystemOperations.CompositeOperationBuilder.create()
                .addStep(SubsystemOperations.createRemoveOperation(jobRepositoryAddress))
                .addStep(SubsystemOperations.createAddOperation(jobRepositoryAddress))
                .build().getOperation();
        ModelNode result = kernelServices.executeOperation(op);
        boolean success = SubsystemOperations.isSuccessfulOutcome(result);
        Assert.assertTrue(success ? "" : SubsystemOperations.getFailureDescriptionAsString(result), success);

        // Add invalid name
        op = SubsystemOperations.CompositeOperationBuilder.create()
                .addStep(SubsystemOperations.createRemoveOperation(jobRepositoryAddress))
                .addStep(SubsystemOperations.createAddOperation(createAddress(JobRepositoryDefinition.NAME, "foo")))
                .build().getOperation();
        result = kernelServices.executeOperation(op);
        success = SubsystemOperations.isSuccessfulOutcome(result);
        Assert.assertFalse(success ? "" : SubsystemOperations.getFailureDescriptionAsString(result), success);

        final ModelNode threadPoolAddress = createAddress("thread-pool", "batch");

        ModelNode addThreadPoolOp = SubsystemOperations.createAddOperation(threadPoolAddress);
        addThreadPoolOp.get("max-threads").set(4);

        op = SubsystemOperations.CompositeOperationBuilder.create()
                .addStep(SubsystemOperations.createRemoveOperation(threadPoolAddress))
                .addStep(addThreadPoolOp)
                .build().getOperation();
        result = kernelServices.executeOperation(op);
        success = SubsystemOperations.isSuccessfulOutcome(result);
        Assert.assertTrue(success ? "" : SubsystemOperations.getFailureDescriptionAsString(result), success);

        addThreadPoolOp = SubsystemOperations.createAddOperation(createAddress("thread-pool", "foo"));
        addThreadPoolOp.get("max-threads").set(4);

        op = SubsystemOperations.CompositeOperationBuilder.create()
                .addStep(SubsystemOperations.createRemoveOperation(threadPoolAddress))
                .addStep(addThreadPoolOp)
                .build().getOperation();
        result = kernelServices.executeOperation(op);
        success = SubsystemOperations.isSuccessfulOutcome(result);
        Assert.assertFalse(success ? "" : SubsystemOperations.getFailureDescriptionAsString(result), success);
    }

    protected KernelServices boot() throws Exception {
        return createKernelServicesBuilder(createAdditionalInitialization()).setSubsystemXml(getSubsystemXml()).build();
    }

    protected static ModelNode createAddress(final PathElement pathElement) {
        return createAddress(pathElement.getKey(), pathElement.getValue());
    }

    protected static ModelNode createAddress(final String resourceKey, final String resourceValue) {
        final ModelNode address = new ModelNode().setEmptyList();
        address.add(ModelDescriptionConstants.SUBSYSTEM, BatchSubsystemDefinition.NAME);
        address.add(resourceKey, resourceValue);
        return address;
    }
}
