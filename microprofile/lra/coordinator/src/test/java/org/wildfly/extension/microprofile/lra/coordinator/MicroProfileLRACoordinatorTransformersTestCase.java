/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.lra.coordinator;

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
 * Tests transformation of the current MicroProfile LRA coordinator management model back to the
 * {@code 1.0.0} model registered by the GA {@link ModelTestControllerVersion#WILDFLY_31_0_0} release.
 * <p>
 * The {@code 2.0.0} model differs from {@code 1.0.0} only in attribute metadata (the {@code server} and
 * {@code host} attributes gained capability references and lost their default values and expression
 * support). The DMR structure is unchanged, so a {@code 2.0.0} configuration with defined attributes
 * transforms identically to {@code 1.0.0}, and no attribute transformation is registered.
 * <p>
 * Note this is not a strictly clean transformation: an omitted (undefined) {@code server}/{@code host} is
 * only logically equivalent to the {@code 1.0.0} default ({@code default-server}/{@code default-host}) while
 * the Undertow subsystem's default server and host retain their canonical names. We lack the context to
 * detect that here, so it cannot be corrected during transformation.
 */
public class MicroProfileLRACoordinatorTransformersTestCase extends AbstractSubsystemTest {

    public MicroProfileLRACoordinatorTransformersTestCase() {
        super(MicroProfileLRACoordinatorExtension.SUBSYSTEM_NAME, new MicroProfileLRACoordinatorExtension());
    }

    @Test
    public void testTransformers_1_0_0() throws Exception {
        ModelVersion modelVersion = MicroProfileLRACoordinatorSubsystemModel.VERSION_1_0_0.getVersion();
        AdditionalInitialization additionalInitialization = AdditionalInitialization.withCapabilities(
                RuntimeCapability.resolveCapabilityName(Server.SERVICE_DESCRIPTOR, "default-server"),
                RuntimeCapability.resolveCapabilityName(Host.SERVICE_DESCRIPTOR, "default-server", "default-host"));

        KernelServicesBuilder builder = createKernelServicesBuilder(additionalInitialization);
        builder.setSubsystemXml(readResource("microprofile-lra-coordinator-2.0.xml"));
        builder.createLegacyKernelServicesBuilder(additionalInitialization, ModelTestControllerVersion.WILDFLY_31_0_0, modelVersion)
                .addMavenResourceURL("org.wildfly:wildfly-microprofile-lra-coordinator:" + ModelTestControllerVersion.WILDFLY_31_0_0.getMavenGavVersion())
                .dontPersistXml();

        KernelServices mainServices = builder.build();
        Assert.assertTrue(mainServices.isSuccessfulBoot());
        KernelServices legacyServices = mainServices.getLegacyServices(modelVersion);
        Assert.assertTrue(legacyServices.isSuccessfulBoot());

        checkSubsystemModelTransformation(mainServices, modelVersion);
    }
}
