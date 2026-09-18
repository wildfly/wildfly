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
import org.jboss.as.model.test.ModelFixer;
import org.jboss.as.model.test.ModelTestControllerVersion;
import org.jboss.as.model.test.ModelTestUtils;
import org.jboss.as.subsystem.test.AbstractSubsystemTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.KernelServicesBuilder;
import org.jboss.as.version.Stability;
import org.jboss.dmr.ModelNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.wildfly.extension.micrometer.prometheus.PrometheusRegistryDefinitionRegistrar;
import org.wildfly.extension.observability.shared.FilterDefinitionRegistrar;

/**
 * Mixed domain transformation testing for the Micrometer subsystem
 */
public class SubsystemTransformersTestCase extends AbstractSubsystemTest {

    public SubsystemTransformersTestCase() {
        super(MicrometerConfigurationConstants.NAME, new MicrometerExtension());
    }

    /**
     * Initializes the subsystem parser before each Jupiter test because the superclass exposes JUnit 4 lifecycle hooks.
     *
     * @throws Exception if parser initialization fails
     */
    @BeforeEach
    public void setUp() throws Exception {
        initializeParser();
    }

    /**
     * Cleans up the subsystem parser after each Jupiter test because the superclass exposes JUnit 4 lifecycle hooks.
     *
     * @throws Exception if parser cleanup fails
     */
    @AfterEach
    public void tearDown() throws Exception {
        cleanup();
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
            .skipReverseControllerCheck()
            .dontPersistXml();

        KernelServices mainServices = builder.build();
        Assertions.assertTrue(mainServices.isSuccessfulBoot());
        Assertions.assertTrue(mainServices.getLegacyServices(modelVersion).isSuccessfulBoot());

        ModelFixer fixer = model -> {
            model.remove(FilterDefinitionRegistrar.PATH.getKey());
            return model;
        };
        checkSubsystemModelTransformation(mainServices, modelVersion, fixer, false);
        ModelNode transformed = mainServices.readTransformedModel(modelVersion);
        Assertions.assertTrue(transformed.hasDefined("subsystem", "micrometer"), transformed.toString());
        Assertions.assertEquals(20L, transformed.get("subsystem", "micrometer","step").asLong(), transformed.toString());
        Assertions.assertEquals("http://localhost:4318/v1/metrics",
                transformed.get("subsystem", "micrometer", "endpoint").asString(), transformed.toString());
        ModelNode main = mainServices.readWholeModel().get("subsystem", "micrometer");
        Assertions.assertEquals(20L, main.get("registry", "otlp", "step").asLong(), main.toString());
        Assertions.assertEquals("http://localhost:4318/v1/metrics",
                main.get("registry", "otlp", "endpoint").asString(), main.toString());
        Assertions.assertFalse(main.hasDefined("endpoint"), main.toString());
        Assertions.assertFalse(main.hasDefined("step"), main.toString());
    }

    @Test
    public void testRejectingTransformers_1_1_0() throws Exception {
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
        Assertions.assertTrue(mainServices.isSuccessfulBoot());
        Assertions.assertTrue(mainServices.getLegacyServices(modelVersion).isSuccessfulBoot());

        List<ModelNode> ops = builder.parseXmlResource("micrometer-community-2.0.xml");

        PathAddress address = PathAddress.pathAddress(SUBSYSTEM_PATH);

        FailedOperationTransformationConfig config = new FailedOperationTransformationConfig();

        PathAddress OTEL_ADDR = address.append(PrometheusRegistryDefinitionRegistrar.PATH);
        config.addFailedAttribute(OTEL_ADDR, FailedOperationTransformationConfig.REJECTED_RESOURCE);

        ModelTestUtils.checkFailedTransformedBootOperations(mainServices, modelVersion, ops, config);

    }
}
