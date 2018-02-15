package org.wildfly.extension.batch.jberet;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.client.Operation;
import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.SubsystemOperations;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
abstract class AbstractBatchTestCase extends AbstractSubsystemBaseTest {
    public AbstractBatchTestCase(final String mainSubsystemName, final Extension mainExtension) {
        super(mainSubsystemName, mainExtension);
    }

    protected KernelServices boot() throws Exception {
        return boot(getSubsystemXml());
    }

    protected KernelServices boot(final String subsystemXml) throws Exception {
        final KernelServices result;
        if (subsystemXml == null) {
            result = createKernelServicesBuilder(createAdditionalInitialization()).build();
        } else {
            result = createKernelServicesBuilder(createAdditionalInitialization()).setSubsystemXml(subsystemXml).build();
        }
        Assert.assertTrue(result.isSuccessfulBoot());
        return result;
    }

    protected static ModelNode executeOperation(final KernelServices kernelServices, final Operation op) {
        return executeOperation(kernelServices, op.getOperation());
    }

    protected static ModelNode executeOperation(final KernelServices kernelServices, final ModelNode op) {
        final ModelNode result = kernelServices.executeOperation(op);
        Assert.assertTrue(SubsystemOperations.getFailureDescriptionAsString(result), SubsystemOperations.isSuccessfulOutcome(result));
        return result;
    }

    protected static ModelNode createAddress(final PathElement pathElement) {
        if (pathElement == null) {
            return PathAddress.pathAddress(BatchSubsystemDefinition.SUBSYSTEM_PATH).toModelNode();
        }
        return PathAddress.pathAddress(BatchSubsystemDefinition.SUBSYSTEM_PATH, pathElement).toModelNode();
    }

    protected static ModelNode createAddress(final String resourceKey, final String resourceValue) {
        return createAddress(PathElement.pathElement(resourceKey, resourceValue));
    }
}
