/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ee.subsystem;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.model.test.ModelTestControllerVersion;
import org.jboss.as.subsystem.test.AbstractSubsystemTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.KernelServicesBuilder;
import org.junit.Assert;
import org.junit.Test;

public class EeTransformersTestCase extends AbstractSubsystemTest {

    public EeTransformersTestCase() {
        super(EeExtension.SUBSYSTEM_NAME, new EeExtension());
    }


    @Test
    public void testTransformers() throws Exception {
        String subsystemXml = "subsystem.xml";
        KernelServicesBuilder builder = createKernelServicesBuilder(createAdditionalInitialization())
                .setSubsystemXmlResource(subsystemXml);

        ModelTestControllerVersion legacyControllerVersion = ModelTestControllerVersion.WILDFLY_31_0_0;
        ModelVersion legacyModelVersion = EESubsystemModel.Version.v6_0_0;

        builder.createLegacyKernelServicesBuilder(createAdditionalInitialization(), legacyControllerVersion, legacyModelVersion)
                .addMavenResourceURL(legacyControllerVersion.createGAV("wildfly-ee"))
                .addMavenResourceURL("org.glassfish:jakarta.enterprise.concurrent:3.0.0")
                .skipReverseControllerCheck()
                .dontPersistXml();

        KernelServices mainServices = builder.build();
        Assert.assertTrue(mainServices.isSuccessfulBoot());

        KernelServices legacyServices = mainServices.getLegacyServices(legacyModelVersion);
        Assert.assertNotNull(legacyServices);
        Assert.assertTrue(legacyServices.isSuccessfulBoot());
        checkSubsystemModelTransformation(mainServices, legacyModelVersion, null, true);
    }

    protected AdditionalInitialization createAdditionalInitialization() {
        return AdditionalInitialization.MANAGEMENT;
    }

}