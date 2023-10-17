/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.naming.subsystem;

import static org.junit.Assert.assertEquals;
import java.io.IOException;
import java.util.List;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.model.test.ModelTestUtils;
import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class NamingSubsystemTestCase extends AbstractSubsystemBaseTest {

    public NamingSubsystemTestCase() {
        super(NamingExtension.SUBSYSTEM_NAME, new NamingExtension());
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("subsystem.xml");
    }

    @Override
    protected String getSubsystemXsdPath() throws IOException {
        return "schema/jboss-as-naming_3_0.xsd";
    }

    @Test
    public void testOnlyExternalContextAllowsCache() throws Exception {
        KernelServices services = createKernelServicesBuilder(createAdditionalInitialization())
                .build();
        Assert.assertTrue(services.isSuccessfulBoot());

        List<ModelNode> list = parse(ModelTestUtils.readResource(this.getClass(), "subsystem.xml"));

        for (ModelNode addOp : list) {
            PathAddress addr = PathAddress.pathAddress(addOp.require(ModelDescriptionConstants.OP_ADDR));
            if (addr.size() == 2 && addr.getLastElement().getKey().equals(NamingSubsystemModel.BINDING) && BindingType.forName(addOp.get(NamingBindingResourceDefinition.BINDING_TYPE.getName()).asString()) != BindingType.EXTERNAL_CONTEXT) {
                //Add the cache attribute and make sure it fails
                addOp.get(NamingBindingResourceDefinition.CACHE.getName()).set(true);
                services.executeForFailure(addOp);

                //Remove the cache attribute and make sure it succeeds
                addOp.remove(NamingBindingResourceDefinition.CACHE.getName());
                ModelTestUtils.checkOutcome(services.executeOperation(addOp));

                //Try to write the cache attribute, which should fail
                ModelTestUtils.checkFailed(services.executeOperation(Util.getWriteAttributeOperation(addr, NamingBindingResourceDefinition.CACHE.getName(), ModelNode.TRUE)));

            } else {
                ModelTestUtils.checkOutcome(services.executeOperation(addOp));
            }
        }


    }

    /**
     * Asserts that bindings may be added through composite ops.
     *
     * @throws Exception
     */
    @Test
    public void testCompositeBindingOps() throws Exception {
        final KernelServices services = createKernelServicesBuilder(createAdditionalInitialization()).setSubsystemXml(getSubsystemXml()).build();
        // add binding 'alookup' through composite op
        // note that a binding-type of 'lookup' requires 'lookup' attr value, which in this case is set by a followup step
        final ModelNode addr = Operations.createAddress(ModelDescriptionConstants.SUBSYSTEM, NamingExtension.SUBSYSTEM_NAME, NamingSubsystemModel.BINDING, "java:global/alookup");
        final ModelNode addOp = Operations.createAddOperation(addr);
        addOp.get(NamingSubsystemModel.BINDING_TYPE).set(NamingSubsystemModel.LOOKUP);
        final ModelNode compositeOp = Operations.CompositeOperationBuilder.create()
                .addStep(addOp)
                .addStep(Operations.createWriteAttributeOperation(addr, NamingSubsystemModel.LOOKUP, "java:global/a"))
                .build().getOperation();
        ModelTestUtils.checkOutcome(services.executeOperation(compositeOp));
    }

    /**
     * Asserts that bindings may be updated through composite ops.
     *
     * @throws Exception
     */
    @Test
    public void testCompositeBindingUpdate() throws Exception {
        final KernelServices services = createKernelServicesBuilder(createAdditionalInitialization()).setSubsystemXml(getSubsystemXml()).build();
        // updates binding 'a' through composite op
        // binding-type used is lookup, op should succeed even if lookup value is set by a followup step
        final ModelNode addr = Operations.createAddress(ModelDescriptionConstants.SUBSYSTEM, NamingExtension.SUBSYSTEM_NAME, NamingSubsystemModel.BINDING, "java:global/a");
        final ModelNode compositeOp = Operations.CompositeOperationBuilder.create()
                .addStep(Operations.createWriteAttributeOperation(addr, NamingSubsystemModel.BINDING_TYPE, NamingSubsystemModel.LOOKUP))
                .addStep(Operations.createWriteAttributeOperation(addr, NamingSubsystemModel.LOOKUP, "java:global/b"))
                .build().getOperation();
        ModelTestUtils.checkOutcome(services.executeOperation(compositeOp));
    }

    @Test
    public void testExpressionInAttributeValue() throws Exception{

        final KernelServices services = createKernelServicesBuilder(createAdditionalInitialization()).setSubsystemXml(readResource("subsystem_expression.xml")).build();
        final ModelNode addr = Operations.createAddress("subsystem", "naming");
        final ModelNode op = Operations.createReadResourceOperation(addr, true);
        op.get(ModelDescriptionConstants.RESOLVE_EXPRESSIONS).set(true);
        final ModelNode result = services.executeOperation(op).get("result");

        ModelNode attribute = result.get(NamingSubsystemModel.BINDING).get("java:global/a");

        final String value = attribute.get(NamingSubsystemModel.VALUE).asString();
        assertEquals("100", value);

        final String type = attribute.get(NamingSubsystemModel.TYPE).asString();
        assertEquals("int", type);

        attribute = result.get(NamingSubsystemModel.BINDING).get("java:global/b");

        final String objclass = attribute.get(NamingSubsystemModel.CLASS).asString();
        assertEquals("org.jboss.as.naming.ManagedReferenceObjectFactory", objclass);

        final String module = attribute.get(NamingSubsystemModel.MODULE).asString();
        assertEquals("org.jboss.as.naming", module);

        attribute = result.get(NamingSubsystemModel.BINDING).get("java:global/c");

        final String lookup = attribute.get(NamingSubsystemModel.LOOKUP).asString();
        assertEquals("java:global/b", lookup);

        attribute = result.get(NamingSubsystemModel.BINDING).get("java:global/d");

        String properties = attribute.get(NamingSubsystemModel.PROPERTIES).asString();
        assertEquals(true, properties.contains("100"));
        assertEquals(true, properties.contains("int"));
    }

    @Override
    protected AdditionalInitialization createAdditionalInitialization() {
        return AdditionalInitialization.withCapabilities("org.wildfly.remoting.endpoint");
    }
}
