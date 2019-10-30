/*
* JBoss, Home of Professional Open Source.
* Copyright 2011, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.weld;

import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.model.test.FailedOperationTransformationConfig;
import org.jboss.as.model.test.FailedOperationTransformationConfig.ChainedConfig;
import org.jboss.as.model.test.FailedOperationTransformationConfig.NewAttributesConfig;
import org.jboss.as.model.test.ModelTestControllerVersion;
import org.jboss.as.model.test.ModelTestUtils;
import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.KernelServicesBuilder;
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
        return "schema/jboss-as-weld_4_0.xsd";
    }

    @Override
    protected String[] getSubsystemTemplatePaths() throws IOException {
        return new String[] {
            "/subsystem-templates/weld.xml"
        };
    }

    @Test
    @Override
    public void testSchemaOfSubsystemTemplates() throws Exception {
        super.testSchemaOfSubsystemTemplates();
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
    public void testTransformersASEAP620() throws Exception {
        testTransformers10(ModelTestControllerVersion.EAP_6_2_0);
    }

    @Test
    public void testTransformersASEAP630() throws Exception {
        testTransformers10(ModelTestControllerVersion.EAP_6_3_0);
    }

    @Test
    public void testTransformersASEAP640() throws Exception {
        testTransformers10(ModelTestControllerVersion.EAP_6_4_0);
    }

    private void testTransformers10(ModelTestControllerVersion controllerVersion) throws Exception {
        ModelVersion modelVersion = ModelVersion.create(1, 0, 0);
        KernelServicesBuilder builder = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT)
                .setSubsystemXmlResource("subsystem_3_0-transformers.xml");
        //which is why we need to include the jboss-as-controller artifact.
        builder.createLegacyKernelServicesBuilder(AdditionalInitialization.MANAGEMENT, controllerVersion, modelVersion)
                .addMavenResourceURL("org.jboss.as:jboss-as-weld:" + controllerVersion.getMavenGavVersion())
                .skipReverseControllerCheck()
                .dontPersistXml();

        KernelServices mainServices = builder.build();
        KernelServices legacyServices = mainServices.getLegacyServices(modelVersion);
        assertTrue(mainServices.isSuccessfulBoot());
        assertTrue(legacyServices.isSuccessfulBoot());

        checkSubsystemModelTransformation(mainServices, modelVersion);
    }

    @Test
    public void testTransformersEAP70() throws Exception {
        ModelVersion modelVersion = ModelVersion.create(3, 0, 0);
        KernelServicesBuilder builder = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT)
                .setSubsystemXmlResource("subsystem_4_0-transformers.xml");
        builder.createLegacyKernelServicesBuilder(AdditionalInitialization.MANAGEMENT, ModelTestControllerVersion.EAP_7_0_0, modelVersion)
                .addMavenResourceURL("org.jboss.eap:wildfly-weld:" + ModelTestControllerVersion.EAP_7_0_0.getMavenGavVersion())
                .dontPersistXml();
        KernelServices mainServices = builder.build();
        KernelServices legacyServices = mainServices.getLegacyServices(modelVersion);
        assertTrue(mainServices.isSuccessfulBoot());
        assertTrue(legacyServices.isSuccessfulBoot());
        checkSubsystemModelTransformation(mainServices, modelVersion);
    }

    @Test
    public void testTransformersRejectionASEAP620() throws Exception {
        testRejectTransformers10(ModelTestControllerVersion.EAP_6_2_0);
    }

    @Test
    public void testTransformersRejectionASEAP630() throws Exception {
        testRejectTransformers10(ModelTestControllerVersion.EAP_6_3_0);
    }

    @Test
    public void testTransformersRejectionASEAP640() throws Exception {
        testRejectTransformers10(ModelTestControllerVersion.EAP_6_4_0);
    }

    private void testRejectTransformers10(ModelTestControllerVersion controllerVersion) throws Exception {

        ModelVersion modelVersion = ModelVersion.create(1, 0, 0);
        KernelServicesBuilder builder = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT);

        //which is why we need to include the jboss-as-controller artifact.
        builder.createLegacyKernelServicesBuilder(AdditionalInitialization.MANAGEMENT, controllerVersion, modelVersion)
                .addMavenResourceURL("org.jboss.as:jboss-as-weld:" + controllerVersion.getMavenGavVersion())
                .dontPersistXml();

        KernelServices mainServices = builder.build();
        Assert.assertTrue(mainServices.isSuccessfulBoot());
        Assert.assertTrue(mainServices.getLegacyServices(modelVersion).isSuccessfulBoot());
        ModelTestUtils.checkFailedTransformedBootOperations(mainServices, modelVersion, parse(getSubsystemXml("subsystem-reject.xml")),
                new FailedOperationTransformationConfig()
                        .addFailedAttribute(
                                PathAddress.pathAddress(WeldExtension.PATH_SUBSYSTEM),
                                ChainedConfig.createBuilder(WeldResourceDefinition.NON_PORTABLE_MODE_ATTRIBUTE, WeldResourceDefinition.REQUIRE_BEAN_DESCRIPTOR_ATTRIBUTE).addConfig(new FalseOrUndefinedToTrueConfig (
                                        WeldResourceDefinition.NON_PORTABLE_MODE_ATTRIBUTE,
                                        WeldResourceDefinition.REQUIRE_BEAN_DESCRIPTOR_ATTRIBUTE
                                ))
                                .addConfig(new NewAttributesConfig(WeldResourceDefinition.DEVELOPMENT_MODE_ATTRIBUTE))
                                .addConfig(new NewAttributesConfig(WeldResourceDefinition.THREAD_POOL_SIZE_ATTRIBUTE))
                                .build()

                        )
        );
    }

    @Test
    public void testTransformersRejectionEAP700() throws Exception {
        ModelVersion modelVersion = ModelVersion.create(3, 0, 0);
        KernelServicesBuilder builder = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT);

        builder.createLegacyKernelServicesBuilder(AdditionalInitialization.MANAGEMENT, ModelTestControllerVersion.EAP_7_0_0, modelVersion)
                .addMavenResourceURL("org.jboss.eap:wildfly-weld:" + ModelTestControllerVersion.EAP_7_0_0.getMavenGavVersion())
                .dontPersistXml();

        KernelServices mainServices = builder.build();
        assertTrue(mainServices.isSuccessfulBoot());
        assertTrue(mainServices.getLegacyServices(modelVersion).isSuccessfulBoot());
        ModelTestUtils.checkFailedTransformedBootOperations(mainServices, modelVersion, parse(getSubsystemXml("subsystem-reject.xml")),
                new FailedOperationTransformationConfig().addFailedAttribute(PathAddress.pathAddress(WeldExtension.PATH_SUBSYSTEM),
                        ChainedConfig
                                .createBuilder(WeldResourceDefinition.NON_PORTABLE_MODE_ATTRIBUTE, WeldResourceDefinition.REQUIRE_BEAN_DESCRIPTOR_ATTRIBUTE)
                                .addConfig(new NewAttributesConfig(WeldResourceDefinition.THREAD_POOL_SIZE_ATTRIBUTE)).build()

                ));
    }


    private static class FalseOrUndefinedToTrueConfig extends FailedOperationTransformationConfig.AttributesPathAddressConfig<FalseOrUndefinedToTrueConfig>{

        FalseOrUndefinedToTrueConfig(AttributeDefinition...defs){
            super(convert(defs));
        }

        @Override
        protected boolean isAttributeWritable(String attributeName) {
            return true;
        }

        @Override
        protected boolean checkValue(String attrName, ModelNode attribute, boolean isWriteAttribute) {
            return !attribute.isDefined() || attribute.asString().equals("false");
        }

        @Override
        protected ModelNode correctValue(ModelNode toResolve, boolean isWriteAttribute) {
            return new ModelNode(true);
        }
    }

}
