/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.config.smallrye;

import static org.jboss.as.weld.Capabilities.WELD_CAPABILITY_NAME;
import static org.junit.Assert.assertTrue;
import static org.wildfly.extension.microprofile.config.smallrye.MicroProfileConfigExtension.CONFIG_SOURCE_PATH;
import static org.wildfly.extension.microprofile.config.smallrye.MicroProfileConfigExtension.SUBSYSTEM_PATH;
import static org.wildfly.extension.microprofile.config.smallrye.MicroProfileConfigExtension.VERSION_1_0_0;
import static org.wildfly.extension.microprofile.config.smallrye.MicroProfileConfigExtension.VERSION_1_1_0;

import java.io.IOException;
import java.util.List;
import java.util.Properties;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.model.test.FailedOperationTransformationConfig;
import org.jboss.as.model.test.ModelTestControllerVersion;
import org.jboss.as.model.test.ModelTestUtils;
import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.KernelServicesBuilder;
import org.jboss.dmr.ModelNode;
import org.junit.Test;

public class MPConfigSubsystemParsingTestCase extends AbstractSubsystemBaseTest {

    public MPConfigSubsystemParsingTestCase() {
        super(MicroProfileConfigExtension.SUBSYSTEM_NAME, new MicroProfileConfigExtension());
    }


    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("subsystem.xml");
    }

    @Override
    protected String getSubsystemXsdPath() {
        return "schema/wildfly-microprofile-config-smallrye_2_0.xsd";
    }

    protected Properties getResolvedProperties() {
        return System.getProperties();
    }

    @Test
    public void testRejectingTransformersEAP_XP4() throws Exception {
        testRejectingTransformers(ModelTestControllerVersion.EAP_XP_4, VERSION_1_1_0);
    }

    @Override
    protected AdditionalInitialization createAdditionalInitialization() {
        return AdditionalInitialization.withCapabilities(
                WELD_CAPABILITY_NAME);
    }

    private void testRejectingTransformers(ModelTestControllerVersion controllerVersion, ModelVersion microprofileConfigVersion) throws Exception {
        //Boot up empty controllers with the resources needed for the ops coming from the xml to work
        KernelServicesBuilder builder = createKernelServicesBuilder(createAdditionalInitialization());
        builder.createLegacyKernelServicesBuilder(createAdditionalInitialization(), controllerVersion, microprofileConfigVersion)
                .addMavenResourceURL(controllerVersion.createGAV("wildfly-microprofile-config-smallrye"))
                .skipReverseControllerCheck()
                .dontPersistXml();

        KernelServices mainServices = builder.build();
        assertTrue(mainServices.isSuccessfulBoot());
        assertTrue(mainServices.getLegacyServices(microprofileConfigVersion).isSuccessfulBoot());

        List<ModelNode> ops = builder.parseXmlResource("subsystem_reject_transformers.xml");

        PathAddress subsystemAddress = PathAddress.pathAddress(SUBSYSTEM_PATH);

        FailedOperationTransformationConfig config = new FailedOperationTransformationConfig();
        if (microprofileConfigVersion.compareTo(VERSION_1_0_0) >= 0) {
            config.addFailedAttribute(subsystemAddress.append(CONFIG_SOURCE_PATH.getKey(), "my-config-source-with-ordinal-expression"),
                    new FailedOperationTransformationConfig.NewAttributesConfig(
                            ConfigSourceDefinition.ORDINAL));
        }
        if (microprofileConfigVersion.compareTo(VERSION_1_1_0) >= 0) {
            config.addFailedAttribute(subsystemAddress.append(CONFIG_SOURCE_PATH.getKey(), "my-config-source-with-root"), new FailedOperationTransformationConfig.AttributesPathAddressConfig() {
                @Override
                protected boolean isAttributeWritable(String attributeName) {
                    return true;
                }

                @Override
                protected boolean checkValue(String attrName, ModelNode attribute, boolean isGeneratedWriteAttribute) {
                    if (attribute.hasDefined(ConfigSourceDefinition.ROOT.getName())) {
                        return attribute.get(ConfigSourceDefinition.ROOT.getName()).asString().equals("false");
                    }
                    return false;
                }

                @Override
                protected ModelNode correctValue(ModelNode toResolve, boolean isGeneratedWriteAttribute) {
                    toResolve.remove(ConfigSourceDefinition.ROOT.getName());
                    return toResolve;
                }
            });
        }

        ModelTestUtils.checkFailedTransformedBootOperations(mainServices, microprofileConfigVersion, ops, config);
    }

}
