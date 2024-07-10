/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.singleton;

import java.util.EnumSet;
import java.util.List;

import org.jboss.as.clustering.controller.CommonUnaryRequirement;
import org.jboss.as.clustering.subsystem.AdditionalInitialization;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.model.test.FailedOperationTransformationConfig;
import org.jboss.as.model.test.ModelTestControllerVersion;
import org.jboss.as.model.test.ModelTestUtils;
import org.jboss.as.subsystem.test.AbstractSubsystemTest;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.KernelServicesBuilder;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.wildfly.clustering.singleton.SingletonCacheRequirement;
import org.wildfly.clustering.singleton.SingletonDefaultCacheRequirement;

/**
 * Transform/reject tests for singleton subsystem.
 *
 * @author Radoslav Husar
 */
@RunWith(value = Parameterized.class)
public class SingletonTransformersTestCase extends AbstractSubsystemTest {

    @Parameters
    public static Iterable<ModelTestControllerVersion> parameters() {
        return EnumSet.of(ModelTestControllerVersion.EAP_7_4_0, ModelTestControllerVersion.EAP_8_0_0);
    }

    private final ModelTestControllerVersion controller;
    private final ModelVersion version;

    public SingletonTransformersTestCase(ModelTestControllerVersion controller) {
        super(SingletonExtension.SUBSYSTEM_NAME, new SingletonExtension());

        this.controller = controller;
        this.version = this.getModelVersion().getVersion();
    }

    private String formatArtifact(String pattern) {
        return String.format(pattern, this.controller.getMavenGavVersion());
    }

    private String formatSubsystemArtifact() {
        return formatArtifact("org.jboss.eap:wildfly-clustering-singleton-extension:%s");
    }

    private SingletonSubsystemModel getModelVersion() {
        switch (this.controller) {
            case EAP_7_4_0:
            case EAP_8_0_0:
                return SingletonSubsystemModel.VERSION_3_0_0;
            default:
                throw new IllegalArgumentException();
        }
    }

    private String[] getDependencies() {
        switch (this.controller) {
            case EAP_7_4_0:
                return new String[] {
                        formatSubsystemArtifact(),
                        formatArtifact("org.jboss.eap:wildfly-clustering-api:%s"),
                        formatArtifact("org.jboss.eap:wildfly-clustering-common:%s"),
                        formatArtifact("org.jboss.eap:wildfly-clustering-server:%s"),
                        formatArtifact("org.jboss.eap:wildfly-clustering-service:%s"),
                        formatArtifact("org.jboss.eap:wildfly-clustering-singleton-api:%s"),
                        formatArtifact("org.jboss.eap:wildfly-clustering-spi:%s"),
                };
            case EAP_8_0_0:
                return new String[] {
                        formatSubsystemArtifact(),
                        formatArtifact("org.jboss.eap:wildfly-clustering-common:%s"),
                        formatArtifact("org.jboss.eap:wildfly-clustering-service:%s"),
                        formatArtifact("org.jboss.eap:wildfly-clustering-singleton-api:%s"),
                };
            default:
                throw new IllegalArgumentException();
        }
    }

    @SuppressWarnings("removal")
    protected org.jboss.as.subsystem.test.AdditionalInitialization createAdditionalInitialization() {
        return new AdditionalInitialization()
                .require(CommonUnaryRequirement.OUTBOUND_SOCKET_BINDING, "binding0", "binding1")
                .require(SingletonDefaultCacheRequirement.SINGLETON_SERVICE_BUILDER_FACTORY, "singleton-container")
                .require(SingletonCacheRequirement.SINGLETON_SERVICE_BUILDER_FACTORY, "singleton-container", "singleton-cache")
                .require(SingletonDefaultCacheRequirement.SINGLETON_SERVICE_CONFIGURATOR_FACTORY, "singleton-container")
                .require(SingletonCacheRequirement.SINGLETON_SERVICE_CONFIGURATOR_FACTORY, "singleton-container", "singleton-cache")
                ;
    }

    @Test
    public void testTransformation() throws Exception {
        String subsystemXmlResource = String.format("singleton-transform-%s.xml", this.version);

        KernelServices services = this.buildKernelServices(subsystemXmlResource, this.controller, this.version, this.getDependencies());

        checkSubsystemModelTransformation(services, this.version, null, false);
    }

    @Test
    public void testRejections() throws Exception {
        // create builder for current subsystem version
        KernelServicesBuilder builder = createKernelServicesBuilder();

        // initialize the legacy services
        builder.createLegacyKernelServicesBuilder(this.createAdditionalInitialization(), controller, version)
                .addSingleChildFirstClass(AdditionalInitialization.class)
                .addMavenResourceURL(this.getDependencies())
        ;

        KernelServices services = builder.build();
        KernelServices legacyServices = services.getLegacyServices(version);
        Assert.assertNotNull(legacyServices);
        Assert.assertTrue(services.isSuccessfulBoot());
        Assert.assertTrue(legacyServices.isSuccessfulBoot());

        // test failed operations involving backups
        List<ModelNode> xmlOps = builder.parseXmlResource("singleton-reject.xml");
        ModelTestUtils.checkFailedTransformedBootOperations(services, version, xmlOps, createFailedOperationConfig(version));
    }

    private static FailedOperationTransformationConfig createFailedOperationConfig(ModelVersion version) {
        return new FailedOperationTransformationConfig();
    }

    private KernelServicesBuilder createKernelServicesBuilder() {
        return this.createKernelServicesBuilder(createAdditionalInitialization());
    }

    private KernelServices buildKernelServices(String subsystemXml, ModelTestControllerVersion controllerVersion, ModelVersion version, String... mavenResourceURLs) throws Exception {
        KernelServicesBuilder builder = this.createKernelServicesBuilder().setSubsystemXmlResource(subsystemXml);

        builder.createLegacyKernelServicesBuilder(this.createAdditionalInitialization(), controllerVersion, version)
                .addSingleChildFirstClass(AdditionalInitialization.class)
                .addMavenResourceURL(mavenResourceURLs)
                .skipReverseControllerCheck()
        ;

        KernelServices services = builder.build();
        Assert.assertTrue(ModelTestControllerVersion.MASTER + " boot failed", services.isSuccessfulBoot());
        Assert.assertTrue(controllerVersion.getMavenGavVersion() + " boot failed", services.getLegacyServices(version).isSuccessfulBoot());
        return services;
    }
}
