/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.micrometer;

import static org.jboss.as.weld.Capabilities.WELD_CAPABILITY_NAME;
import static org.wildfly.extension.micrometer.MicrometerSubsystemRegistrar.SUBSYSTEM_PATH;
import static org.wildfly.extension.micrometer.otlp.OtlpRegistryDefinitionRegistrar.MICROMETER_OTLP_CONFIG_RUNTIME_CAPABILITY;

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
import org.jboss.as.version.Stability;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Test;
import org.wildfly.extension.micrometer.prometheus.PrometheusRegistryDefinitionRegistrar;

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

        var capabilityNames = new String[]{WELD_CAPABILITY_NAME, MICROMETER_OTLP_CONFIG_RUNTIME_CAPABILITY.getName()};

        KernelServicesBuilder builder = createKernelServicesBuilder(
            AdditionalInitialization.withCapabilities(Stability.COMMUNITY, capabilityNames));

        builder.setSubsystemXml(readResource("transform-1.1.xml"));
        builder.createLegacyKernelServicesBuilder(AdditionalInitialization.withCapabilities(capabilityNames),
                ModelTestControllerVersion.EAP_XP_5, modelVersion)
            .addMavenResourceURL("org.jboss.eap.xp:wildfly-micrometer:" + ModelTestControllerVersion.EAP_XP_5.getMavenGavVersion())
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
        ModelVersion modelVersion = MicrometerSubsystemModel.VERSION_1_1_0.getVersion();

        var capabilityNames = new String[]{WELD_CAPABILITY_NAME, MICROMETER_OTLP_CONFIG_RUNTIME_CAPABILITY.getName()};

        KernelServicesBuilder builder = createKernelServicesBuilder(
            AdditionalInitialization.withCapabilities(Stability.COMMUNITY, capabilityNames));

        builder.createLegacyKernelServicesBuilder(AdditionalInitialization.withCapabilities(capabilityNames),
                ModelTestControllerVersion.EAP_XP_5, modelVersion)
            .addMavenResourceURL("org.jboss.eap.xp:wildfly-micrometer:" +
                ModelTestControllerVersion.EAP_XP_5.getMavenGavVersion())
            .skipReverseControllerCheck()
            .dontPersistXml();

        KernelServices mainServices = builder.build();
        Assert.assertTrue(mainServices.isSuccessfulBoot());
        Assert.assertTrue(mainServices.getLegacyServices(modelVersion).isSuccessfulBoot());

        List<ModelNode> ops = builder.parseXmlResource("micrometer-2.0.xml");

        PathAddress address = PathAddress.pathAddress(SUBSYSTEM_PATH);

        FailedOperationTransformationConfig config = new FailedOperationTransformationConfig();

        PathAddress OTEL_ADDR = address.append(PrometheusRegistryDefinitionRegistrar.PATH);
        config.addFailedAttribute(OTEL_ADDR, FailedOperationTransformationConfig.REJECTED_RESOURCE);

        ModelTestUtils.checkFailedTransformedBootOperations(mainServices, modelVersion, ops, config);

    }
}
