/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ee.subsystem;

import org.jboss.as.controller.client.helpers.ClientConstants;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

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
}
