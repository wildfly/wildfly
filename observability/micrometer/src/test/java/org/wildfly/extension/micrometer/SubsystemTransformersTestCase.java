/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.micrometer;

import static org.jboss.as.weld.Capabilities.WELD_CAPABILITY_NAME;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.model.test.ModelTestControllerVersion;
import org.jboss.as.subsystem.test.AbstractSubsystemTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.KernelServicesBuilder;
import org.jboss.as.version.Stability;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Test;

/**
 * Mixed domain transformation testing for the Micrometer subsystem
 */
public class SubsystemTransformersTestCase extends AbstractSubsystemTest {

    public SubsystemTransformersTestCase() {
        super(MicrometerConfigurationConstants.NAME, new MicrometerExtension());
    }

    @Test
    public void testTransformers_1_1_0() throws Exception {
        ModelVersion modelVersion = MicrometerSubsystemModel.VERSION_1_1_0.getVersion();

        KernelServicesBuilder builder = createKernelServicesBuilder(
                AdditionalInitialization.withCapabilities(Stability.COMMUNITY, WELD_CAPABILITY_NAME));
        builder.setSubsystemXml(readResource("transform-1.1.xml"));
        builder.createLegacyKernelServicesBuilder(AdditionalInitialization.MANAGEMENT, ModelTestControllerVersion.EAP_XP_5, modelVersion)
                .addMavenResourceURL("org.jboss.eap.xp:wildfly-micrometer:" + ModelTestControllerVersion.EAP_XP_5.getMavenGavVersion())
                .dontPersistXml();

        KernelServices mainServices = builder.build();
        Assert.assertTrue(mainServices.isSuccessfulBoot());
        Assert.assertTrue(mainServices.getLegacyServices(modelVersion).isSuccessfulBoot());

        checkSubsystemModelTransformation(mainServices, modelVersion);
        ModelNode transformed = mainServices.readTransformedModel(modelVersion);
        Assert.assertTrue(transformed.isDefined());
    }
}
