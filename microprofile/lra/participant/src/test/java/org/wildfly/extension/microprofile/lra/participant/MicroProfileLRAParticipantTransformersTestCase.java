/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.lra.participant;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.model.test.ModelTestControllerVersion;
import org.jboss.as.subsystem.test.AbstractSubsystemTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.KernelServicesBuilder;
import org.junit.Assert;
import org.junit.Test;
import org.wildfly.extension.undertow.Host;
import org.wildfly.extension.undertow.Server;

/**
 * Tests transformation of the current MicroProfile LRA participant management model back to the
 * {@code 1.0.0} model registered by the GA {@link ModelTestControllerVersion#WILDFLY_31_0_0} release.
 * <p>
 * The {@code 2.0.0} model differs from {@code 1.0.0} only in attribute metadata (the {@code proxy-server}
 * and {@code proxy-host} attributes gained capability references and lost their default values and
 * expression support). The DMR structure is unchanged, so a strict {@code 2.0.0} configuration transforms
 * cleanly to {@code 1.0.0}.
 */
public class MicroProfileLRAParticipantTransformersTestCase extends AbstractSubsystemTest {

    public MicroProfileLRAParticipantTransformersTestCase() {
        super(MicroProfileLRAParticipantExtension.SUBSYSTEM_NAME, new MicroProfileLRAParticipantExtension());
    }

    @Test
    public void testTransformers_1_0_0() throws Exception {
        ModelVersion modelVersion = MicroProfileLRAParticipantSubsystemModel.VERSION_1_0_0.getVersion();
        AdditionalInitialization additionalInitialization = AdditionalInitialization.withCapabilities(
                RuntimeCapability.buildDynamicCapabilityName(Server.SERVICE_DESCRIPTOR.getName(), "default-server"),
                RuntimeCapability.buildDynamicCapabilityName(Host.SERVICE_DESCRIPTOR.getName(), "default-server", "default-host"));

        KernelServicesBuilder builder = createKernelServicesBuilder(additionalInitialization);
        builder.setSubsystemXml(readResource("microprofile-lra-participant-2.0.xml"));
        builder.createLegacyKernelServicesBuilder(additionalInitialization, ModelTestControllerVersion.WILDFLY_31_0_0, modelVersion)
                .addMavenResourceURL("org.wildfly:wildfly-microprofile-lra-participant:" + ModelTestControllerVersion.WILDFLY_31_0_0.getMavenGavVersion())
                .dontPersistXml();

        KernelServices mainServices = builder.build();
        Assert.assertTrue(mainServices.isSuccessfulBoot());
        KernelServices legacyServices = mainServices.getLegacyServices(modelVersion);
        Assert.assertTrue(legacyServices.isSuccessfulBoot());

        checkSubsystemModelTransformation(mainServices, modelVersion);
    }
}
