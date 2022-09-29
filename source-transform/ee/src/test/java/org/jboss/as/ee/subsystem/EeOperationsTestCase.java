/*
 * Copyright 2015 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.as.ee.subsystem;

import java.io.IOException;

import org.jboss.as.controller.RunningMode;
import org.jboss.as.controller.client.helpers.ClientConstants;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.controller.client.helpers.Operations.CompositeOperationBuilder;
import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.ControllerInitializer;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Test;
import org.wildfly.common.cpu.ProcessorInfo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class EeOperationsTestCase extends AbstractSubsystemBaseTest {

    public EeOperationsTestCase() {
        super(EeExtension.SUBSYSTEM_NAME, new EeExtension());
    }

    @Override
    protected void standardSubsystemTest(final String configId) throws Exception {
        // do nothing as this is not a subsystem parsing test
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("simple-subsystem.xml");
    }

    @Test
    public void testManagedExecutorFailureOperations() throws Exception {
        // Boot the container
        final KernelServices kernelServices = createKernelServicesBuilder(createAdditionalInitialization()).setSubsystemXml(getSubsystemXml()).build();

        // Default address
        final ModelNode address = Operations.createAddress(ClientConstants.SUBSYSTEM, EeExtension.SUBSYSTEM_NAME, "managed-executor-service", "default");

        // Create a composite operation that should fail; note that if the queue-length is undefined, 0 or Integer.MAX_VALUE the
        // core-threads must be greater than 0
        ModelNode op = Operations.createWriteAttributeOperation(address, "core-threads", 0);

        ModelNode result = kernelServices.executeOperation(op);
        Assert.assertFalse(Operations.isSuccessfulOutcome(result));

        op = CompositeOperationBuilder.create()
                .addStep(Operations.createWriteAttributeOperation(address, "queue-length", Integer.MAX_VALUE))
                .addStep(Operations.createWriteAttributeOperation(address, "core-threads", 0))
                .build().getOperation();

        result = kernelServices.executeOperation(op);
        Assert.assertFalse(Operations.isSuccessfulOutcome(result));

        op = CompositeOperationBuilder.create()
                .addStep(Operations.createWriteAttributeOperation(address, "queue-length", 0))
                .addStep(Operations.createWriteAttributeOperation(address, "core-threads", 0))
                .build().getOperation();

        result = kernelServices.executeOperation(op);
        Assert.assertFalse(Operations.isSuccessfulOutcome(result));

        // The max-threads must be greater than or equal to the core-threads
        op = CompositeOperationBuilder.create()
                .addStep(Operations.createWriteAttributeOperation(address, "core-threads", 4))
                .addStep(Operations.createWriteAttributeOperation(address, "max-threads", 1))
                .build().getOperation();

        result = kernelServices.executeOperation(op);
        Assert.assertFalse(Operations.isSuccessfulOutcome(result));


        // Test a failure at the runtime-stage
        op = CompositeOperationBuilder.create()
                .addStep(Operations.createWriteAttributeOperation(address, "queue-length", "${test.queue-length:10}"))
                .addStep(Operations.createWriteAttributeOperation(address, "core-threads", "${test.core-threads:500}"))
                .build().getOperation();

        result = kernelServices.executeOperation(op);
        Assert.assertFalse(Operations.isSuccessfulOutcome(result));

        // The max-threads must be greater than or equal to the core-threads
        final int calculatedMaxThreads = (ProcessorInfo.availableProcessors() * 2);
        op = CompositeOperationBuilder.create()
                .addStep(Operations.createWriteAttributeOperation(address, "core-threads", calculatedMaxThreads))
                .addStep(Operations.createWriteAttributeOperation(address, "max-threads", calculatedMaxThreads - 1))
                .build().getOperation();

        result = kernelServices.executeOperation(op);
        Assert.assertFalse(Operations.isSuccessfulOutcome(result));
    }

    @Test
    public void testManagedExecutorOperations() throws Exception {
        // Boot the container
        final KernelServices kernelServices = createKernelServicesBuilder(createAdditionalInitialization()).setSubsystemXml(getSubsystemXml()).build();

        // Default address
        final ModelNode address = Operations.createAddress(ClientConstants.SUBSYSTEM, EeExtension.SUBSYSTEM_NAME, "managed-executor-service", "default");

        ModelNode op = CompositeOperationBuilder.create()
                .addStep(Operations.createWriteAttributeOperation(address, "queue-length", Integer.MAX_VALUE))
                .addStep(Operations.createWriteAttributeOperation(address, "core-threads", 5))
                .build().getOperation();

        executeForSuccess(kernelServices, op);

        op = CompositeOperationBuilder.create()
                .addStep(Operations.createWriteAttributeOperation(address, "max-threads", 5))
                .addStep(Operations.createWriteAttributeOperation(address, "queue-length", 10))
                .addStep(Operations.createWriteAttributeOperation(address, "core-threads", 0))
                .build().getOperation();

        executeForSuccess(kernelServices, op);

        // The max-threads must be greater than or equal to the core-threads
        op = CompositeOperationBuilder.create()
                .addStep(Operations.createWriteAttributeOperation(address, "core-threads", 4))
                .addStep(Operations.createWriteAttributeOperation(address, "max-threads", 4))
                .build().getOperation();

        executeForSuccess(kernelServices, op);
    }

    @Test
    public void testAddExistingGlobalModule() throws Exception {
        // Boot the container
        final KernelServices kernelServices = createKernelServicesBuilder(createAdditionalInitialization()).setSubsystemXml(getSubsystemXml()).build();
        final ModelNode address = Operations.createAddress(ClientConstants.SUBSYSTEM, EeExtension.SUBSYSTEM_NAME);

        addModule(kernelServices, address, moduleNode("test.module.one", null));
        addModule(kernelServices, address, moduleNode("test.module.one", null));

        ModelNode res = executeForSuccess(kernelServices, Operations.createReadAttributeOperation(address, GlobalModulesDefinition.GLOBAL_MODULES));

        assertEquals(1, res.get(ClientConstants.RESULT).asList().size());
    }

    @Test
    public void testAddUniqueGlobalModule() throws Exception {
        // Boot the container
        final KernelServices kernelServices = createKernelServicesBuilder(createAdditionalInitialization()).setSubsystemXml(getSubsystemXml()).build();
        final ModelNode address = Operations.createAddress(ClientConstants.SUBSYSTEM, EeExtension.SUBSYSTEM_NAME);

        addModule(kernelServices, address, moduleNode("test.module.one", null));
        addModule(kernelServices, address, moduleNode("test.module.two", null));

        ModelNode res = executeForSuccess(kernelServices, Operations.createReadAttributeOperation(address, GlobalModulesDefinition.GLOBAL_MODULES));

        assertEquals(2, res.get(ClientConstants.RESULT).asList().size());
    }

    @Test
    public void testAddExistingGlobalModuleWithDifferentSlot() throws Exception {
        // Boot the container
        final KernelServices kernelServices = createKernelServicesBuilder(createAdditionalInitialization()).setSubsystemXml(getSubsystemXml()).build();
        final ModelNode address = Operations.createAddress(ClientConstants.SUBSYSTEM, EeExtension.SUBSYSTEM_NAME);

        addModule(kernelServices, address, moduleNode("test.module.one", "main"));
        addModule(kernelServices, address, moduleNode("test.module.one", "foo"));

        ModelNode res = executeForSuccess(kernelServices, Operations.createReadAttributeOperation(address, GlobalModulesDefinition.GLOBAL_MODULES));

        assertEquals(2, res.get(ClientConstants.RESULT).asList().size());
    }

    @Test
    public void testAddExistingGlobalModuleWithMetaInf() throws Exception {
        // Boot the container
        final KernelServices kernelServices = createKernelServicesBuilder(createAdditionalInitialization()).setSubsystemXml(getSubsystemXml()).build();
        final ModelNode address = Operations.createAddress(ClientConstants.SUBSYSTEM, EeExtension.SUBSYSTEM_NAME);

        ModelNode module = moduleNode("test.module.one", "main");
        addModule(kernelServices, address, module);
        module.get(GlobalModulesDefinition.META_INF).set(true);
        addModule(kernelServices, address, module);

        ModelNode res = executeForSuccess(kernelServices, Operations.createReadAttributeOperation(address, GlobalModulesDefinition.GLOBAL_MODULES));

        assertEquals(1, res.get(ClientConstants.RESULT).asList().size());
        ModelNode actualModule = res.get(ClientConstants.RESULT).asList().get(0);
        assertTrue(actualModule.get(GlobalModulesDefinition.META_INF).asBoolean());
    }

    private void addModule(KernelServices kernelServices, ModelNode address, ModelNode module) {
        executeForSuccess(kernelServices, listAddOp(address, module));
    }

    private ModelNode moduleNode(String moduleName, String slotName) {
        ModelNode module = new ModelNode();
        module.get(ClientConstants.NAME).set(moduleName);
        if (slotName != null) {
            module.get(GlobalModulesDefinition.SLOT).set(slotName);
        }
        return module;
    }

    private ModelNode listAddOp(ModelNode address, ModelNode list) {
        ModelNode op = Operations.createOperation("list-add", address);
        op.get(ClientConstants.NAME).set(GlobalModulesDefinition.GLOBAL_MODULES);
        op.get(ClientConstants.VALUE).set(list);
        return op;
    }

    private ModelNode executeForSuccess(final KernelServices kernelServices, final ModelNode op) {
        final ModelNode result = kernelServices.executeOperation(op);
        if (!Operations.isSuccessfulOutcome(result)) {
            Assert.fail(Operations.getFailureDescription(result).asString());
        }
        return result;
    }

    @Override
    protected AdditionalInitialization createAdditionalInitialization() {
        return new AdditionalInitialization() {

            @Override
            protected RunningMode getRunningMode() {
                return RunningMode.NORMAL;
            }

            @Override
            protected ControllerInitializer createControllerInitializer() {
                return new EeInitializer();
            }
        };
    }

    class EeInitializer extends ControllerInitializer {
        public EeInitializer() {
            addSystemProperty("test.queue-length", "0");
            addSystemProperty("test.core-threads", "0");
        }
    }
}
