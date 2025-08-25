/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.weld;

import java.io.IOException;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.model.test.FailedOperationTransformationConfig;
import org.jboss.as.model.test.ModelTestControllerVersion;
import org.jboss.as.model.test.ModelTestUtils;
import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.KernelServicesBuilder;
import org.jboss.as.subsystem.test.LegacyKernelServicesInitializer;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class WeldSubsystemTestCase extends AbstractSubsystemBaseTest {

    public WeldSubsystemTestCase() {
        super(WeldExtension.SUBSYSTEM_NAME, new WeldExtension());
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("subsystem.xml");
    }

    @Override
    protected String getSubsystemXsdPath() throws Exception {
        return "schema/jboss-as-weld_5_0.xsd";
    }

    @Test
    public void testSubsystem10() throws Exception {
        standardSubsystemTest("subsystem_1_0.xml", false);
    }

    @Test
    public void testSubsystem20() throws Exception {
        standardSubsystemTest("subsystem_2_0.xml", false);
    }

    @Test
    public void testSubsystem30() throws Exception {
        standardSubsystemTest("subsystem_3_0.xml", false);
    }

    @Test
    public void testSubsystem40() throws Exception {
        standardSubsystemTest("subsystem_4_0.xml", false);
    }

    @Test
    public void testExpressions() throws Exception {
        standardSubsystemTest("subsystem_with_expression.xml");
    }

    @Test
    public void testTransformerEAP740() throws Exception {
        testTransformer(ModelTestControllerVersion.EAP_7_4_0, true);
    }

    @Test
    public void testTransformersRejectionEAP740() throws Exception {
        testTransformersRejection(ModelTestControllerVersion.EAP_7_4_0);
    }

    private void testTransformersRejection(ModelTestControllerVersion controllerVersion) throws Exception {
        ModelVersion modelVersion = controllerVersion.getSubsystemModelVersion(getMainSubsystemName());
        KernelServices mainServices = buildKernelServices(controllerVersion, null, false);
        Assert.assertTrue(mainServices.isSuccessfulBoot());
        Assert.assertTrue(mainServices.getLegacyServices(modelVersion).isSuccessfulBoot());

        ModelTestUtils.checkFailedTransformedBootOperations(mainServices, modelVersion, parse(getSubsystemXml("subsystem-reject.xml")),
                new FailedOperationTransformationConfig().addFailedAttribute(PathAddress.pathAddress(WeldExtension.PATH_SUBSYSTEM),
                        new FailedOperationTransformationConfig.NewAttributesConfig(WeldResourceDefinition.LEGACY_EMPTY_BEANS_XML_TREATMENT_ATTRIBUTE) {
                            @Override
                            protected boolean checkValue(String attrName, ModelNode attribute, boolean isGeneratedWriteAttribute) {
                                return !attribute.equals(ModelNode.TRUE);
                            }

                            @Override
                            protected ModelNode correctValue(ModelNode attribute, boolean isGeneratedWriteAttribute) {
                                // if it's 'false' change it to undefined to test handling of undefined as well
                                return attribute.isDefined() ? ModelNode.TRUE : new ModelNode();
                            }
                        }));
    }

    private void testTransformer(ModelTestControllerVersion controllerVersion, boolean fixLegacyEmptyXmlTreatment) throws Exception {
        KernelServices mainServices = buildKernelServices(controllerVersion, getSubsystemXml(), fixLegacyEmptyXmlTreatment);
        ModelVersion modelVersion = controllerVersion.getSubsystemModelVersion(getMainSubsystemName());
        // check that both versions of the legacy model are the same and valid
        checkSubsystemModelTransformation(mainServices, modelVersion, null, false);

        ModelNode transformed = mainServices.readTransformedModel(modelVersion);
        Assert.assertTrue(transformed.isDefined());
    }

    private KernelServices buildKernelServices(ModelTestControllerVersion legacyVersion, String subsystemXml, boolean fixLegacyEmptyXmlTreatment) throws Exception {
        ModelVersion modelVersion = legacyVersion.getSubsystemModelVersion(getMainSubsystemName());
        KernelServicesBuilder builder = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT);
        if (subsystemXml != null) {
            builder.setSubsystemXml(subsystemXml);
        }
        LegacyKernelServicesInitializer legacyInitializer =
                builder.createLegacyKernelServicesBuilder(AdditionalInitialization.MANAGEMENT, ModelTestControllerVersion.EAP_7_4_0, modelVersion)
                .addMavenResourceURL(ModelTestControllerVersion.EAP_7_4_0.createGAV("wildfly-weld"))
                .addParentFirstClassPattern("org.jboss.msc.*")
                .addParentFirstClassPattern("org.jboss.msc.service.*")
                .dontPersistXml();
        if (fixLegacyEmptyXmlTreatment) {
            // The legacy host won't configure "legacy-empty-beans-xml-treatment=true" as that is
            // the hard coded, unconfigurable behavior. So when we try and boot the current version
            // with the boot ops from the legacy host, fix that up
            legacyInitializer.configureReverseControllerCheck(AdditionalInitialization.MANAGEMENT,
                    null,
                    WeldSubsystemTestCase::fixLegacyAddOp);
        }
        KernelServices mainServices = builder.build();
        Assert.assertTrue(mainServices.isSuccessfulBoot());
        Assert.assertTrue(mainServices.getLegacyServices(modelVersion).isSuccessfulBoot());
        return mainServices;
    }

    private static ModelNode fixLegacyAddOp(ModelNode op) {
        ModelNode addr = op.get("address");
        if (addr.asInt() == 1 && addr.get(0).asProperty().getName().equals("subsystem")) {
            op.get("legacy-empty-beans-xml-treatment").set(true);
        }
        return op;
    }
}
