/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.singleton;

import java.util.EnumSet;
import java.util.List;

import org.jboss.as.clustering.subsystem.AdditionalInitialization;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.model.test.FailedOperationTransformationConfig;
import org.jboss.as.model.test.ModelTestControllerVersion;
import org.jboss.as.model.test.ModelTestUtils;
import org.jboss.as.network.OutboundSocketBinding;
import org.jboss.as.subsystem.test.AbstractSubsystemTest;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.KernelServicesBuilder;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.wildfly.clustering.infinispan.service.InfinispanServiceDescriptor;
import org.wildfly.clustering.singleton.service.SingletonServiceTargetFactory;

/**
 * Transform/reject tests for singleton subsystem.
 *
 * @author Radoslav Husar
 */
@RunWith(value = Parameterized.class)
public class SingletonTransformersTestCase extends AbstractSubsystemTest {

    @Parameters
    public static Iterable<ModelTestControllerVersion> parameters() {
        return EnumSet.of(
                ModelTestControllerVersion.EAP_7_4_0,
                ModelTestControllerVersion.EAP_8_0_0,
                ModelTestControllerVersion.EAP_8_1_0
        );
    }

    private final ModelTestControllerVersion controllerVersion;
    private final ModelVersion version;

    public SingletonTransformersTestCase(ModelTestControllerVersion controllerVersion) {
        super(SingletonSubsystemResourceDefinitionRegistrar.REGISTRATION.getName(), new SingletonExtension());

        this.controllerVersion = controllerVersion;
        this.version = this.getModelVersion().getVersion();
    }

    private SingletonSubsystemModel getModelVersion() {
        return switch (this.controllerVersion) {
            case EAP_7_4_0, EAP_8_0_0, EAP_8_1_0 -> SingletonSubsystemModel.VERSION_3_0_0;
            default -> throw new IllegalArgumentException();
        };
    }

    private String[] getDependencies() {
        return switch (this.controllerVersion) {
            case EAP_7_4_0 -> new String[] {
                    createGAV("wildfly-clustering-singleton-extension"),
                    createGAV("wildfly-clustering-api"),
                    createGAV("wildfly-clustering-common"),
                    createGAV("wildfly-clustering-server"),
                    createGAV("wildfly-clustering-service"),
                    createGAV("wildfly-clustering-singleton-api"),
                    createGAV("wildfly-clustering-spi"),
            };
            case EAP_8_0_0 -> new String[] {
                    createGAV("wildfly-clustering-singleton-extension"),
                    createGAV("wildfly-clustering-common"),
                    createGAV("wildfly-clustering-service"),
                    createGAV("wildfly-clustering-singleton-api"),
            };
            case EAP_8_1_0 -> new String[] {
                    createGAV("wildfly-clustering-singleton-extension"),
                    createGAV("wildfly-clustering-common"),
                    createGAV("wildfly-clustering-server-service"),
                    createGAV("wildfly-clustering-singleton-api"),
                    createCoreGAV("wildfly-subsystem"),
            };
            default -> throw new IllegalArgumentException();
        };
    }

    // TODO Replace with variants from after wf-core upgrade https://issues.redhat.com/browse/WFCORE-7298
    // n.b. workaround for https://issues.redhat.com/browse/WFCORE-7297
    public String createGAV(String artifactId) {
        return String.format("%s:%s:%s", this.controllerVersion.getMavenGroupId(), artifactId, this.controllerVersion.getMavenGavVersion());
    }

    public String createCoreGAV(String artifactId) {
        return String.format("%s:%s:%s", this.controllerVersion.getCoreMavenGroupId(), artifactId, this.controllerVersion.getCoreVersion());
    }

    @SuppressWarnings("removal")
    protected org.jboss.as.subsystem.test.AdditionalInitialization createAdditionalInitialization() {
        return new AdditionalInitialization()
                .require(OutboundSocketBinding.SERVICE_DESCRIPTOR, "binding0")
                .require(OutboundSocketBinding.SERVICE_DESCRIPTOR, "binding1")
                .require(InfinispanServiceDescriptor.DEFAULT_CACHE_CONFIGURATION, "singleton-container")
                .require(SingletonServiceTargetFactory.DEFAULT_SERVICE_DESCRIPTOR, "singleton-container")
                .require(org.wildfly.clustering.singleton.service.SingletonServiceConfiguratorFactory.DEFAULT_SERVICE_DESCRIPTOR, "singleton-container")
                .require(InfinispanServiceDescriptor.CACHE_CONFIGURATION, "singleton-container", "singleton-cache")
                .require(SingletonServiceTargetFactory.SERVICE_DESCRIPTOR, "singleton-container", "singleton-cache")
                .require(org.wildfly.clustering.singleton.service.SingletonServiceConfiguratorFactory.SERVICE_DESCRIPTOR, "singleton-container", "singleton-cache")
                ;
    }

    @Test
    public void testTransformation() throws Exception {
        String subsystemXmlResource = String.format("singleton-transform-%s.xml", this.version);

        KernelServices services = this.buildKernelServices(subsystemXmlResource, this.controllerVersion, this.version, this.getDependencies());

        checkSubsystemModelTransformation(services, this.version, null, false);
    }

    @Test
    public void testRejections() throws Exception {
        // create builder for current subsystem version
        KernelServicesBuilder builder = createKernelServicesBuilder();

        // initialize the legacy services
        builder.createLegacyKernelServicesBuilder(this.createAdditionalInitialization(), controllerVersion, version)
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
