/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.opentelemetry;

import static org.jboss.as.weld.Capabilities.WELD_CAPABILITY_NAME;
import static org.wildfly.extension.opentelemetry.OpenTelemetryConfigurationConstants.EXPORTER_OTLP;
import static org.wildfly.extension.opentelemetry.OpenTelemetryConfigurationConstants.EXPORTER_TYPE;
import static org.wildfly.extension.opentelemetry.OpenTelemetryConfigurationConstants.SYSTEM_METRICS;
import static org.wildfly.extension.opentelemetry.OpenTelemetrySubsystemModel.VERSION_1_0_0;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.model.test.ModelFixer;
import org.jboss.as.model.test.ModelTestControllerVersion;
import org.jboss.as.subsystem.test.AbstractSubsystemTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.KernelServicesBuilder;
import org.jboss.as.version.Stability;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Mixed domain transformation testing for the OpenTelemetry subsystem
 */
public class SubsystemTransformersTestCase extends AbstractSubsystemTest {

    public SubsystemTransformersTestCase() {
        super(OpenTelemetryConfigurationConstants.SUBSYSTEM_NAME, new OpenTelemetrySubsystemExtension());
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
    public void testTransformerEAPXP4() throws Exception {
        testTransformation(ModelTestControllerVersion.EAP_XP_4);
    }

    @Test
    public void testSystemMetricsDiscardedFrom_1_0_0() throws Exception {
        ModelTestControllerVersion controller = ModelTestControllerVersion.EAP_XP_4;
        ModelVersion version = VERSION_1_0_0.getVersion();
        ModelNode operation = Util.createAddOperation(
                PathAddress.pathAddress(OpenTelemetryConfigurationConstants.SUBSYSTEM_PATH));
        operation.get(SYSTEM_METRICS).set(false);

        KernelServicesBuilder builder = createKernelServicesBuilder(
                AdditionalInitialization.withCapabilities(Stability.COMMUNITY, WELD_CAPABILITY_NAME))
                .setBootOperations(operation);
        builder.createLegacyKernelServicesBuilder(createAdditionalInitialization(), controller, version)
                .addMavenResourceURL(controller.createGAV("wildfly-opentelemetry"))
                .skipReverseControllerCheck()
                .dontPersistXml();
        KernelServices services = builder.build();

        Assertions.assertTrue(services.isSuccessfulBoot(), ModelTestControllerVersion.MASTER + " boot failed");
        Assertions.assertTrue(services.getLegacyServices(version).isSuccessfulBoot(),
                controller.getMavenGavVersion() + " boot failed");
        ModelNode transformed = services.readTransformedModel(version)
                .get("subsystem", OpenTelemetryConfigurationConstants.SUBSYSTEM_NAME);
        Assertions.assertFalse(transformed.hasDefined(SYSTEM_METRICS), transformed.toString());
    }

    @SuppressWarnings("SameParameterValue")
    private void testTransformation(final ModelTestControllerVersion controller) throws Exception {
        final ModelVersion version = getModelVersion(controller).getVersion();
        final String subsystemXmlResource = String.format("transform-%d_%d_%d.xml", version.getMajor(), version.getMinor(), version.getMicro());

        KernelServices services = this.buildKernelServices(readResource(subsystemXmlResource), controller, version, getDependencies(controller));

        // check that both versions of the legacy model are the same and valid
        checkSubsystemModelTransformation(services, version, createModelFixer(version), false);
    }

    private KernelServices buildKernelServices(String xml, ModelTestControllerVersion controllerVersion, ModelVersion version, String... mavenResourceURLs) throws Exception {
        KernelServicesBuilder builder = this.createKernelServicesBuilder(createAdditionalInitialization()).setSubsystemXml(xml);

        builder.createLegacyKernelServicesBuilder(createAdditionalInitialization(), controllerVersion, version)
                .addMavenResourceURL(mavenResourceURLs)
                .skipReverseControllerCheck()
                .dontPersistXml();

        KernelServices services = builder.build();
        Assertions.assertTrue(services.isSuccessfulBoot(), ModelTestControllerVersion.MASTER + " boot failed");
        Assertions.assertTrue(services.getLegacyServices(version).isSuccessfulBoot(),
                controllerVersion.getMavenGavVersion() + " boot failed");
        return services;
    }

    private AdditionalInitialization createAdditionalInitialization() {
        return AdditionalInitialization.withCapabilities(
                WELD_CAPABILITY_NAME);
    }

    private static OpenTelemetrySubsystemModel getModelVersion(ModelTestControllerVersion controllerVersion) {
        //noinspection SwitchStatementWithTooFewBranches
        switch (controllerVersion) {
            case EAP_XP_4:
                return OpenTelemetrySubsystemModel.VERSION_1_0_0;
            default:
                throw new IllegalArgumentException();
        }
    }

    private static String[] getDependencies(ModelTestControllerVersion version) {
        //noinspection SwitchStatementWithTooFewBranches
        switch (version) {
            case EAP_XP_4:
                return new String[] {
                        version.createGAV("wildfly-opentelemetry")
                };
        }
        throw new IllegalArgumentException();
    }

    private static ModelFixer createModelFixer(ModelVersion version) {
        return model -> {
            if (OpenTelemetrySubsystemModel.VERSION_1_0_0.requiresTransformation(version)) {
                // The transformer will turn 'undefined' on the DC to 'otlp' on the legacy HC, so confirm that was done
                // and then switch it back to undefined so the comparison to DC passes
                Assertions.assertEquals(EXPORTER_OTLP, model.get(EXPORTER_TYPE).asString(), model.toString());
                model.get(EXPORTER_TYPE).set(ModelType.UNDEFINED);
            }
            return model;
        };
    }
}
