/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.elytron.oidc;

import static org.wildfly.extension.elytron.oidc.ElytronOidcClientSubsystemModel.VERSION_2_0_0;
import static org.wildfly.extension.elytron.oidc.ElytronOidcClientSubsystemModel.VERSION_4_0_0;

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
    public static Collection<Object[]> parameters() {
        return List.of(
            new Object[] { ModelTestControllerVersion.EAP_8_0_0, VERSION_2_0_0.getVersion() },
            new Object[] { ModelTestControllerVersion.EAP_8_1_0, VERSION_4_0_0.getVersion() }
        );
    }

    private final ModelTestControllerVersion controllerVersion;
    private final ModelVersion targetModelVersion;

    public ElytronOidcClientSubsystemTransformerTestCase(ModelTestControllerVersion controllerVersion, ModelVersion targetModelVersion) {
        super(ElytronOidcExtension.SUBSYSTEM_NAME, new ElytronOidcExtension(), CURRENT_SCHEMA, CURRENT_SCHEMA);
        this.controllerVersion = controllerVersion;
        this.targetModelVersion = targetModelVersion;
    }

    @Test
    public void testTransformations() throws Exception {
        KernelServices services = this.buildKernelServices(controllerVersion, targetModelVersion);

        checkSubsystemModelTransformation(services, targetModelVersion, null, false);
        ModelNode transformed = services.readTransformedModel(targetModelVersion);
        Assert.assertTrue(transformed.isDefined());
    }

    @Test
    public void testRejection() throws Exception {
        testRejectingTransformers(controllerVersion, "elytron-oidc-client-reject.xml", forControllerVersion(controllerVersion));
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
                RuntimeCapability.buildDynamicCapabilityName("org.wildfly.security", "elytron")), controllerVersion, targetModelVersion)
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
        Assert.assertTrue(controllerVersion.getMavenGavVersion() + " boot failed", services.getLegacyServices(targetModelVersion).isSuccessfulBoot());

        List<ModelNode> ops = builder.parseXmlResource(subsystemXmlFile);
        ModelTestUtils.checkFailedTransformedBootOperations(services, targetModelVersion, ops, config);
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
