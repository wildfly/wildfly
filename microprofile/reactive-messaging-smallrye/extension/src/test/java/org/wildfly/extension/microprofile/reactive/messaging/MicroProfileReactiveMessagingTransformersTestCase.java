/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.reactive.messaging;

import static org.wildfly.extension.microprofile.reactive.messaging.MicroProfileReactiveMessagingConnectorOpenTelemetryTracingResourceDefinition.MICROPROFILE_TELEMETRY_CAPABILITY_NAME;
import static org.wildfly.extension.microprofile.reactive.messaging.MicroProfileReactiveMessagingExtension.CONFIG_CAPABILITY_NAME;
import static org.wildfly.extension.microprofile.reactive.messaging.MicroProfileReactiveMessagingExtension.REACTIVE_STREAMS_OPERATORS_CAPABILITY_NAME;
import static org.wildfly.extension.microprofile.reactive.messaging.MicroProfileReactiveMessagingExtension.SUBSYSTEM_PATH;
import static org.wildfly.extension.microprofile.reactive.messaging.MicroProfileReactiveMessagingExtension.WELD_CAPABILITY_NAME;

import java.util.List;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.model.test.FailedOperationTransformationConfig;
import org.jboss.as.model.test.ModelTestControllerVersion;
import org.jboss.as.model.test.ModelTestUtils;
import org.jboss.as.subsystem.test.AbstractSubsystemTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.KernelServicesBuilder;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Test;

public class MicroProfileReactiveMessagingTransformersTestCase extends AbstractSubsystemTest {

    public MicroProfileReactiveMessagingTransformersTestCase() {
        super(MicroProfileReactiveMessagingExtension.SUBSYSTEM_NAME, new MicroProfileReactiveMessagingExtension());
    }


    @Test
    public void testTransformers_1_0_0() throws Exception {
        ModelVersion modelVersion = MicroProfileReactiveMessagingExtension.VERSION_1_0_0;

        KernelServicesBuilder builder = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT);
        builder.setSubsystemXml(readResource("microprofile-reactive-messaging-smallrye-1.0.xml"));
        builder.createLegacyKernelServicesBuilder(AdditionalInitialization.MANAGEMENT, ModelTestControllerVersion.EAP_XP_5, modelVersion)
            .addMavenResourceURL("org.jboss.eap.xp:wildfly-microprofile-reactive-messaging:" + ModelTestControllerVersion.EAP_XP_5.getMavenGavVersion())
            .dontPersistXml();

        KernelServices mainServices = builder.build();
        Assert.assertTrue(mainServices.isSuccessfulBoot());
        Assert.assertTrue(mainServices.getLegacyServices(modelVersion).isSuccessfulBoot());

        checkSubsystemModelTransformation(mainServices, modelVersion);
        ModelNode transformed = mainServices.readTransformedModel(modelVersion);
        Assert.assertTrue(transformed.isDefined());
    }

    @Test
    public void testRejectingTransformers_1_0_0() throws Exception {
        ModelVersion modelVersion = MicroProfileReactiveMessagingExtension.VERSION_1_0_0;
        AdditionalInitialization additionalInitialization = AdditionalInitialization.withCapabilities(
                CONFIG_CAPABILITY_NAME,
                MICROPROFILE_TELEMETRY_CAPABILITY_NAME,
                REACTIVE_STREAMS_OPERATORS_CAPABILITY_NAME,
                WELD_CAPABILITY_NAME);
        KernelServicesBuilder builder = createKernelServicesBuilder(additionalInitialization);

        builder.createLegacyKernelServicesBuilder(additionalInitialization, ModelTestControllerVersion.EAP_XP_5, modelVersion)
            .addMavenResourceURL("org.jboss.eap.xp:wildfly-microprofile-reactive-messaging:" + ModelTestControllerVersion.EAP_XP_5.getMavenGavVersion())
            .skipReverseControllerCheck()
            .dontPersistXml();

        KernelServices mainServices = builder.build();
        Assert.assertTrue(mainServices.isSuccessfulBoot());
        Assert.assertTrue(mainServices.getLegacyServices(modelVersion).isSuccessfulBoot());

        List<ModelNode> ops = builder.parseXmlResource("microprofile-reactive-messaging-smallrye-2.0.xml");

        PathAddress address = PathAddress.pathAddress(SUBSYSTEM_PATH);

        FailedOperationTransformationConfig config = new FailedOperationTransformationConfig();

        PathAddress OTEL_ADDR = address.append(MicroProfileReactiveMessagingConnectorOpenTelemetryTracingResourceDefinition.PATH);
        config.addFailedAttribute(OTEL_ADDR, FailedOperationTransformationConfig.REJECTED_RESOURCE);

        ModelTestUtils.checkFailedTransformedBootOperations(mainServices, modelVersion, ops, config);

    }
}
