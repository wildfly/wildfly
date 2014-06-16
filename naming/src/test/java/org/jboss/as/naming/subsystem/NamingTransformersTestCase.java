/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.naming.subsystem;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.naming.subsystem.NamingExtension.SUBSYSTEM_NAME;
import static org.jboss.as.naming.subsystem.NamingExtension.VERSION_1_1_0;
import static org.jboss.as.naming.subsystem.NamingExtension.VERSION_1_2_0;
import static org.jboss.as.naming.subsystem.NamingSubsystemModel.BINDING;
import static org.jboss.as.naming.subsystem.NamingSubsystemModel.BINDING_TYPE;
import static org.jboss.as.naming.subsystem.NamingSubsystemModel.CLASS;
import static org.jboss.as.naming.subsystem.NamingSubsystemModel.ENVIRONMENT;
import static org.jboss.as.naming.subsystem.NamingSubsystemModel.EXTERNAL_CONTEXT;
import static org.jboss.as.naming.subsystem.NamingSubsystemModel.MODULE;
import static org.jboss.as.naming.subsystem.NamingSubsystemModel.OBJECT_FACTORY;
import static org.jboss.as.naming.subsystem.NamingSubsystemModel.SIMPLE;
import static org.jboss.as.naming.subsystem.NamingSubsystemModel.TYPE;
import static org.jboss.as.naming.subsystem.NamingSubsystemModel.VALUE;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URL;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.model.test.FailedOperationTransformationConfig;
import org.jboss.as.model.test.ModelFixer;
import org.jboss.as.model.test.ModelTestControllerVersion;
import org.jboss.as.model.test.ModelTestUtils;
import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.KernelServicesBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;
import org.jboss.dmr.ValueExpression;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test case that verifies functionality of Naming 1.1.0 Transformers.
 *
 * @author Eduardo Martins
 */
public class NamingTransformersTestCase extends AbstractSubsystemBaseTest {

    public NamingTransformersTestCase() {
        super(SUBSYSTEM_NAME, new NamingExtension());
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("subsystem_with_expressions_compatible_1.1.0.xml");
    }

    @Override
    protected AdditionalInitialization createAdditionalInitialization() {
        return AdditionalInitialization.MANAGEMENT;
    }

    @Test
    public void testOperationCompatibility() throws Exception {
        KernelServicesBuilder builder = createKernelServicesBuilder(createAdditionalInitialization())
                .setSubsystemXmlResource("subsystem_with_expressions_compatible_1.1.0.xml");

        builder.createLegacyKernelServicesBuilder(createAdditionalInitialization(), ModelTestControllerVersion.V7_1_2_FINAL, VERSION_1_1_0)
                .addMavenResourceURL("org.jboss.as:jboss-as-naming:7.1.2.Final");

        KernelServices mainServices712 = builder.build();
        KernelServices legacyServices712 = mainServices712.getLegacyServices(VERSION_1_1_0);
        assertNotNull(legacyServices712);

        builder = createKernelServicesBuilder(createAdditionalInitialization())
                .setSubsystemXmlResource("subsystem_with_expressions_compatible_1.1.0.xml");

        builder.createLegacyKernelServicesBuilder(null, ModelTestControllerVersion.V7_1_3_FINAL, VERSION_1_1_0)
                .addMavenResourceURL("org.jboss.as:jboss-as-naming:7.1.3.Final");

        KernelServices mainServices713 = builder.build();
        KernelServices legacyServices713 = mainServices713.getLegacyServices(VERSION_1_1_0);
        assertNotNull(legacyServices713);
    }


    @Test
    public void testTransformers_AS712() throws Exception {
        testTransformers_1_1_0(ModelTestControllerVersion.V7_1_2_FINAL);
    }

    @Test
    public void testTransformers_AS713() throws Exception {
        testTransformers_1_1_0(ModelTestControllerVersion.V7_1_3_FINAL);
    }


    @Test
    public void testTransformers_EAP600() throws Exception {
        testTransformers_1_1_0(ModelTestControllerVersion.EAP_6_0_0);
    }

    @Test
    public void testTransformers_EAP601() throws Exception {
        testTransformers_1_1_0(ModelTestControllerVersion.EAP_6_0_1);
    }

    private void testTransformers_1_1_0(ModelTestControllerVersion version) throws Exception {
        KernelServicesBuilder builder = createKernelServicesBuilder(createAdditionalInitialization())
                .setSubsystemXmlResource("subsystem_with_expressions_compatible_1.1.0.xml");

        builder.createLegacyKernelServicesBuilder(null, version, VERSION_1_1_0)
                .addMavenResourceURL("org.jboss.as:jboss-as-naming:" + version.getMavenGavVersion())
                .skipReverseControllerCheck();

        KernelServices mainServices = builder.build();
        KernelServices legacyServices = mainServices.getLegacyServices(VERSION_1_1_0);
        assertNotNull(legacyServices);

        checkSubsystemModelTransformation(mainServices, VERSION_1_1_0, MODEL_FIXER_1_1_0);

        checkSimpleBindingTransformation(mainServices, VERSION_1_1_0);
        checkObjectFactoryWithEnvironmentBindingTransformation(mainServices, VERSION_1_1_0);

        checkSuccessfulObjectFactoryWithEnvironmentBindingTransformation(mainServices, VERSION_1_1_0);
        checkSuccessfulSimpleBindingTransformation(mainServices, VERSION_1_1_0);

        checkExternalContextEnvironmentBindingTransformationFails(mainServices, VERSION_1_1_0);
    }

    private void checkSuccessfulSimpleBindingTransformation(KernelServices mainServices, ModelVersion version_1_1_0)
            throws OperationFailedException {

        final String name = "java:global/as75140-1";
        final ModelNode address = new ModelNode();
        address.add(SUBSYSTEM, SUBSYSTEM_NAME);
        address.add(BINDING, name);
        // bind a URL
        final ModelNode bindingAdd = new ModelNode();
        bindingAdd.get(OP).set(ADD);
        bindingAdd.get(OP_ADDR).set(address);
        bindingAdd.get(BINDING_TYPE).set(SIMPLE);
        bindingAdd.get(VALUE).set("http://localhost");
        bindingAdd.get(TYPE).set("java.lang.String");

        ModelNode resultNode = mainServices.executeOperation(version_1_1_0,
                mainServices.transformOperation(version_1_1_0, bindingAdd));
        Assert.assertFalse(resultNode.get(FAILURE_DESCRIPTION).toString(), resultNode.get(FAILURE_DESCRIPTION).isDefined());
    }

    private void checkSimpleBindingTransformation(KernelServices mainServices, ModelVersion version_1_1_0)
            throws OperationFailedException {

        final String name = "java:global/as75140-2";
        final ModelNode address = new ModelNode();
        address.add(SUBSYSTEM, SUBSYSTEM_NAME);
        address.add(BINDING, name);
        // bind a URL
        final ModelNode bindingAdd = new ModelNode();
        bindingAdd.get(OP).set(ADD);
        bindingAdd.get(OP_ADDR).set(address);
        bindingAdd.get(BINDING_TYPE).set(SIMPLE);
        bindingAdd.get(VALUE).set("http://localhost");
        bindingAdd.get(TYPE).set(URL.class.getName());

        ModelNode resultNode = mainServices.executeOperation(version_1_1_0,
                mainServices.transformOperation(version_1_1_0, bindingAdd));
        Assert.assertTrue(resultNode.get(FAILURE_DESCRIPTION).isDefined());
    }

    private void checkObjectFactoryWithEnvironmentBindingTransformation(KernelServices mainServices, ModelVersion version_1_1_0)
            throws OperationFailedException {

        final ModelNode address = new ModelNode();
        address.add(SUBSYSTEM, SUBSYSTEM_NAME);
        address.add(BINDING, "java:global/as75140-3");
        final ModelNode bindingAdd = new ModelNode();
        bindingAdd.get(OP).set(ADD);
        bindingAdd.get(OP_ADDR).set(address);
        bindingAdd.get(BINDING_TYPE).set(OBJECT_FACTORY);
        bindingAdd.get(MODULE).set("org.jboss.as.naming");
        bindingAdd.get(CLASS).set("org.jboss.as.naming.ManagedReferenceObjectFactory");
        bindingAdd.get(ENVIRONMENT).set(new ModelNode().add("a", "a"));

        ModelNode resultNode = mainServices.executeOperation(version_1_1_0,
                mainServices.transformOperation(version_1_1_0, bindingAdd));
        Assert.assertTrue(resultNode.get(FAILURE_DESCRIPTION).isDefined());
    }

    private void checkSuccessfulObjectFactoryWithEnvironmentBindingTransformation(KernelServices mainServices,
                                                                                  ModelVersion version_1_1_0) throws OperationFailedException {

        final ModelNode address = new ModelNode();
        address.add(SUBSYSTEM, SUBSYSTEM_NAME);
        address.add(BINDING, "java:global/as75140-4");
        final ModelNode bindingAdd = new ModelNode();
        bindingAdd.get(OP).set(ADD);
        bindingAdd.get(OP_ADDR).set(address);
        bindingAdd.get(BINDING_TYPE).set(OBJECT_FACTORY);
        bindingAdd.get(MODULE).set("org.jboss.as.naming");
        bindingAdd.get(CLASS).set("org.jboss.as.naming.ManagedReferenceObjectFactory");

        ModelNode resultNode = mainServices.executeOperation(version_1_1_0,
                mainServices.transformOperation(version_1_1_0, bindingAdd));
        Assert.assertFalse(resultNode.get(FAILURE_DESCRIPTION).toString(), resultNode.get(FAILURE_DESCRIPTION).isDefined());
    }

    @Test
    public void testRejectExpressionsAS712() throws Exception {
        doTestRejectExpressions_1_1_0(ModelTestControllerVersion.V7_1_2_FINAL);
    }

    @Test
    public void testRejectExpressionsAS713() throws Exception {
        doTestRejectExpressions_1_1_0(ModelTestControllerVersion.V7_1_3_FINAL);
    }

    @Test
    public void testRejectExpressionsEAP600() throws Exception {
        doTestRejectExpressions_1_1_0(ModelTestControllerVersion.EAP_6_0_0);
    }

    @Test
    public void testRejectExpressionsEAP601() throws Exception {
        doTestRejectExpressions_1_1_0(ModelTestControllerVersion.EAP_6_0_1);
    }

    /**
     * Tests rejection of expressions in 1.1.0 model.
     *
     * @throws Exception
     */
    private void doTestRejectExpressions_1_1_0(ModelTestControllerVersion controllerVersion) throws Exception {
        KernelServicesBuilder builder = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT);

        // create builder for legacy subsystem version
        builder.createLegacyKernelServicesBuilder(null, controllerVersion, VERSION_1_1_0)
                .addMavenResourceURL("org.jboss.as:jboss-as-naming:" + controllerVersion.getMavenGavVersion());


        KernelServices mainServices = builder.build();
        assertTrue(mainServices.isSuccessfulBoot());
        KernelServices legacyServices = mainServices.getLegacyServices(VERSION_1_1_0);
        Assert.assertNotNull(legacyServices);
        assertTrue(legacyServices.isSuccessfulBoot());

        //Use the real xml with expressions for testing all the attributes
        ModelTestUtils.checkFailedTransformedBootOperations(mainServices, VERSION_1_1_0, parse(getSubsystemXml("subsystem_with_expressions_compatible_1.2.0.xml")),
                new FailedOperationTransformationConfig()
                        .addFailedAttribute(PathAddress.pathAddress(NamingExtension.SUBSYSTEM_PATH, NamingSubsystemModel.BINDING_PATH),
                                new FailedOperationTransformationConfig.NewAttributesConfig(NamingSubsystemModel.ENVIRONMENT))
        );
    }

    @Test
    public void testTransformers_AS720() throws Exception {
        testTransformers_1_2_0(ModelTestControllerVersion.V7_2_0_FINAL);
    }

    @Test
    public void testTransformers_EAP610() throws Exception {
        testTransformers_1_2_0(ModelTestControllerVersion.EAP_6_1_0);
    }

    @Test
    public void testTransformers_EAP611() throws Exception {
        testTransformers_1_2_0(ModelTestControllerVersion.EAP_6_1_1);
    }

    private void testTransformers_1_2_0(ModelTestControllerVersion version) throws Exception {
        KernelServicesBuilder builder = createKernelServicesBuilder(createAdditionalInitialization())
                .setSubsystemXmlResource("subsystem_with_expressions_compatible_1.2.0.xml");

        builder.createLegacyKernelServicesBuilder(null, version, VERSION_1_2_0)
                .addMavenResourceURL("org.jboss.as:jboss-as-naming:" + version.getMavenGavVersion())
                .skipReverseControllerCheck();

        KernelServices mainServices = builder.build();
        KernelServices legacyServices = mainServices.getLegacyServices(VERSION_1_2_0);
        assertNotNull(legacyServices);

        checkSubsystemModelTransformation(mainServices, VERSION_1_2_0);

        checkExternalContextEnvironmentBindingTransformationFails(mainServices, VERSION_1_2_0);
    }

    private void checkExternalContextEnvironmentBindingTransformationFails(KernelServices mainServices, ModelVersion version)
            throws OperationFailedException {

        final ModelNode address = new ModelNode();
        address.add(SUBSYSTEM, SUBSYSTEM_NAME);
        address.add(BINDING, "java:global/as75140-7");
        final ModelNode bindingAdd = new ModelNode();
        bindingAdd.get(OP).set(ADD);
        bindingAdd.get(OP_ADDR).set(address);
        bindingAdd.get(BINDING_TYPE).set(EXTERNAL_CONTEXT);
        bindingAdd.get(MODULE).set("org.jboss.as.naming");
        bindingAdd.get(CLASS).set("javax.naming.InitialContext");
        bindingAdd.get(ENVIRONMENT).set(new ModelNode().add("a", "a"));

        ModelNode resultNode = mainServices.executeOperation(version,
                mainServices.transformOperation(version, bindingAdd));
        Assert.assertTrue(resultNode.get(FAILURE_DESCRIPTION).isDefined());
    }

    private static ModelFixer MODEL_FIXER_1_1_0 = new ModelFixer() {
        //The legacy subsystem does not seem to set expressions correctly so fix them here to match the transformed resource
        @Override
        public ModelNode fixModel(ModelNode modelNode) {
            for (Property property : modelNode.get("binding").asPropertyList()) {
                ModelNode entry = property.getValue();
                for (Property nestedProp : entry.asPropertyList()) {
                    ModelNode value = nestedProp.getValue();
                    if (value.getType() == ModelType.STRING && value.asString().startsWith("$") && value.asString().endsWith("}")) {
                        ModelNode fixed = new ModelNode();
                        fixed.set(new ValueExpression(value.asString()));
                        modelNode.get("binding", property.getName(), nestedProp.getName()).set(fixed);
                    }
                }
            }
            return modelNode;
        }

    };
}
