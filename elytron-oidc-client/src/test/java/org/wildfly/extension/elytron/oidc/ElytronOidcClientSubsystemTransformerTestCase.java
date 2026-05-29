/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.elytron.oidc;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.model.test.FailedOperationTransformationConfig;
import org.jboss.as.model.test.ModelTestControllerVersion;
import org.jboss.as.model.test.ModelTestUtils;
import org.jboss.as.subsystem.test.AbstractSubsystemSchemaTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.KernelServicesBuilder;
import org.jboss.as.version.Stability;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.wildfly.extension.elytron.oidc.OidcTestCase.DefaultInitializer;

@RunWith(value = Parameterized.class)
public class ElytronOidcClientSubsystemTransformerTestCase extends AbstractSubsystemSchemaTest<ElytronOidcSubsystemSchema> {

    private static final ElytronOidcSubsystemSchema CURRENT_SCHEMA = ElytronOidcSubsystemSchema.CURRENT.get(Stability.DEFAULT);
    private static final ModelVersion CURRENT_MODEL_VERSION = ElytronOidcClientSubsystemModel.CURRENT.getVersion();

    @Parameterized.Parameters
    public static Collection<ModelTestControllerVersion> parameters() {
        return List.of(
            ModelTestControllerVersion.EAP_8_0_0, ModelTestControllerVersion.EAP_8_1_0
        );
    }
    private final ModelTestControllerVersion controllerVersion;

    public ElytronOidcClientSubsystemTransformerTestCase(ModelTestControllerVersion controllerVersion) {
        super(ElytronOidcExtension.SUBSYSTEM_NAME, new ElytronOidcExtension(), CURRENT_SCHEMA, CURRENT_SCHEMA);
        this.controllerVersion = controllerVersion;
    }

    @Test
    public void testTransformations() throws Exception {
        KernelServices services = this.buildKernelServices(controllerVersion, CURRENT_MODEL_VERSION);

        checkSubsystemModelTransformation(services, CURRENT_MODEL_VERSION, null, false);
        ModelNode transformed = services.readTransformedModel(CURRENT_MODEL_VERSION);
        Assert.assertTrue(transformed.isDefined());
    }

    @Test
    public void testRejection() throws Exception {
        testRejectingTransformers(controllerVersion, "elytron-oidc-client-reject.xml", forControllerVersion(controllerVersion)
                // No attributes should fail as no default stability level changes since EAP 8.0.0
        );
    }

    private static FailedOperationTransformationConfig forControllerVersion(ModelTestControllerVersion controllerVersion) {
        FailedOperationTransformationConfig config = new FailedOperationTransformationConfig();

        // To begin with JBoss EAP 8.0 and 8.1 support the same attributes so anything new added to be rejected should be tested here.

        return config;
    }

    private void testRejectingTransformers(ModelTestControllerVersion controllerVersion, final String subsystemXmlFile, final FailedOperationTransformationConfig config) throws Exception {
        KernelServicesBuilder builder = createKernelServicesBuilder(AdditionalInitialization.withCapabilities(
                RuntimeCapability.buildDynamicCapabilityName("org.wildfly.security", "elytron")));

        builder.createLegacyKernelServicesBuilder(AdditionalInitialization.withCapabilities(
                RuntimeCapability.buildDynamicCapabilityName("org.wildfly.security", "elytron")), controllerVersion, CURRENT_MODEL_VERSION)
                .addMavenResourceURL(controllerVersion.createGAV("wildfly-elytron-oidc-client-subsystem"))
                .skipReverseControllerCheck()
                .addParentFirstClassPattern("org.jboss.as.controller.logging.ControllerLogger*")
                .addParentFirstClassPattern("org.jboss.as.controller.PathAddress")
                .addParentFirstClassPattern("org.jboss.as.controller.PathElement")
                .addParentFirstClassPattern("org.jboss.as.server.logging.*")
                .addParentFirstClassPattern("org.jboss.logging.*")
                .addParentFirstClassPattern("org.jboss.dmr.*")
                .dontPersistXml();

        KernelServices services = builder.build();
        Assert.assertTrue(ModelTestControllerVersion.MASTER + " boot failed", services.isSuccessfulBoot());
        Assert.assertTrue(controllerVersion.getMavenGavVersion() + " boot failed", services.getLegacyServices(CURRENT_MODEL_VERSION).isSuccessfulBoot());

        List<ModelNode> ops = builder.parseXmlResource(subsystemXmlFile);
        ModelTestUtils.checkFailedTransformedBootOperations(services, CURRENT_MODEL_VERSION, ops, config);
    }

    private KernelServices buildKernelServices(ModelTestControllerVersion controllerVersion, ModelVersion version) throws Exception {
        KernelServicesBuilder builder = this.createKernelServicesBuilder(new DefaultInitializer(this.getSubsystemSchema().getStability()))
                .setSubsystemXmlResource("elytron-oidc-client-transform.xml");
        builder.createLegacyKernelServicesBuilder(AdditionalInitialization.ADMIN_ONLY_HC, controllerVersion, version)
                .addMavenResourceURL(controllerVersion.createGAV("wildfly-elytron-oidc-client-subsystem"))
                .skipReverseControllerCheck()
                .addParentFirstClassPattern("org.jboss.as.controller.logging.ControllerLogger*")
                .addParentFirstClassPattern("org.jboss.as.controller.PathAddress")
                .addParentFirstClassPattern("org.jboss.as.controller.PathElement")
                .addParentFirstClassPattern("org.jboss.as.server.logging.*")
                .addParentFirstClassPattern("org.jboss.logging.*")
                .addParentFirstClassPattern("org.jboss.dmr.*")
                .dontPersistXml();

        KernelServices services = builder.build();
        Assert.assertTrue(ModelTestControllerVersion.MASTER + " boot failed", services.isSuccessfulBoot());
        Assert.assertTrue(controllerVersion.getMavenGavVersion() + " boot failed", services.getLegacyServices(version).isSuccessfulBoot());

        return services;
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("elytron-oidc-client-transform.xml");
    }
}
