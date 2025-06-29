/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.web;

import java.util.EnumSet;
import java.util.List;

import org.jboss.as.clustering.subsystem.AdditionalInitialization;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
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
import org.wildfly.clustering.infinispan.client.service.HotRodServiceDescriptor;
import org.wildfly.clustering.infinispan.service.InfinispanServiceDescriptor;

/**
 * Transformer tests for distributable-web subsystem.
 * @author Paul Ferraro
 * @author Radoslav Husar
 */
@RunWith(value = Parameterized.class)
public class DistributableWebTransformerTestCase extends AbstractSubsystemTest {

    @Parameters
    public static Iterable<ModelTestControllerVersion> parameters() {
        return EnumSet.of(
                ModelTestControllerVersion.EAP_7_4_0,
                ModelTestControllerVersion.EAP_8_0_0,
                ModelTestControllerVersion.EAP_8_1_0
        );
    }

    private final ModelTestControllerVersion controller;
    private final org.jboss.as.subsystem.test.AdditionalInitialization additionalInitialization;
    private final ModelVersion version;

    public DistributableWebTransformerTestCase(ModelTestControllerVersion controller) {
        super(DistributableWebExtension.SUBSYSTEM_NAME, new DistributableWebExtension());
        this.controller = controller;
        this.version = this.getModelVersion().getVersion();
        this.additionalInitialization = new AdditionalInitialization()
                .require(InfinispanServiceDescriptor.CACHE_CONTAINER, "foo")
                .require(InfinispanServiceDescriptor.DEFAULT_CACHE_CONFIGURATION, "foo")
                .require(InfinispanServiceDescriptor.CACHE_CONFIGURATION, "foo", "bar")
                .require(InfinispanServiceDescriptor.CACHE, "foo", "routing")
                .require(InfinispanServiceDescriptor.CACHE_CONFIGURATION, "foo", "routing")
                .require(HotRodServiceDescriptor.REMOTE_CACHE_CONTAINER, "foo")
                ;
    }

    private String formatArtifact(String artifactIdSegment) {
        return this.getMavenGav(artifactIdSegment,false);
    }

    private String formatCoreArtifact(String artifactIdSegment) {
        return this.getMavenGav(artifactIdSegment,true);
    }

    // Workaround for org.jboss.as.model.test.ModelTestControllerVersion#getMavenGav(..)
    private String getMavenGav(String artifactIdSegment, boolean isCoreArtifact) {
        return String.format("%s:%s%s:%s",
                isCoreArtifact ? this.controller.getCoreMavenGroupId() : this.controller.getMavenGroupId(),
                this.controller.getArtifactIdPrefix(),
                artifactIdSegment,
                isCoreArtifact ? this.controller.getCoreVersion() : this.controller.getMavenGavVersion()
        );
    }

    private DistributableWebSubsystemModel getModelVersion() {
        return switch (this.controller) {
            case EAP_7_4_0 -> DistributableWebSubsystemModel.VERSION_2_0_0;
            case EAP_8_0_0, EAP_8_1_0 -> DistributableWebSubsystemModel.VERSION_4_0_0;
            default -> throw new IllegalArgumentException();
        };
    }

    private String[] getDependencies() {
        return switch (this.controller) {
            case EAP_7_4_0 -> new String[] {
                    formatArtifact("clustering-web-extension"),
                    formatArtifact("clustering-common"),
                    formatArtifact("clustering-ee-hotrod"),
                    formatArtifact("clustering-ee-infinispan"),
                    formatArtifact("clustering-ee-spi"),
                    formatArtifact("clustering-infinispan-client"),
                    formatArtifact("clustering-infinispan-spi"),
                    formatArtifact("clustering-marshalling-spi"),
                    formatArtifact("clustering-service"),
                    formatArtifact("clustering-web-container"),
                    formatArtifact("clustering-web-hotrod"),
                    formatArtifact("clustering-web-infinispan"),
                    formatArtifact("clustering-web-spi"),
            };
            case EAP_8_0_0 -> new String[] {
                    formatArtifact("clustering-web-extension"),
                    formatArtifact("clustering-common"),
                    formatArtifact("clustering-ee-hotrod"),
                    formatArtifact("clustering-ee-infinispan"),
                    formatArtifact("clustering-ee-spi"),
                    formatArtifact("clustering-infinispan-client-service"),
                    formatArtifact("clustering-infinispan-embedded-service"),
                    formatArtifact("clustering-marshalling-spi"),
                    formatArtifact("clustering-service"),
                    formatArtifact("clustering-web-container"),
                    formatArtifact("clustering-web-hotrod"),
                    formatArtifact("clustering-web-infinispan"),
                    formatArtifact("clustering-web-service"),
                    formatArtifact("clustering-web-spi"),
            };
            case EAP_8_1_0 -> new String[] {
                    formatArtifact("clustering-web-extension"),
                    formatArtifact("clustering-common"),
                    formatArtifact("clustering-infinispan-client-service"),
                    formatArtifact("clustering-infinispan-embedded-service"),
                    formatArtifact("clustering-server-service"),
                    formatArtifact("clustering-web-service"),
                    formatCoreArtifact("service"),
                    formatCoreArtifact("subsystem"),
            };
            default -> throw new IllegalArgumentException();
        };
    }

    /**
     * Tests transformation of model from current version into specified version.
     */
    @Test
    public void testTransformation() throws Exception {
        String subsystemXmlResource = String.format("distributable-web-transform-%s.xml", this.version);

        // create builder for current subsystem version
        KernelServicesBuilder builder = createKernelServicesBuilder(this.additionalInitialization)
                .setSubsystemXmlResource(subsystemXmlResource);

        // initialize the legacy services and add required jars
        builder.createLegacyKernelServicesBuilder(this.additionalInitialization, this.controller, this.version)
                .addMavenResourceURL(this.getDependencies())
                .addSingleChildFirstClass(AdditionalInitialization.class)
                .skipReverseControllerCheck()
                .dontPersistXml();

        KernelServices services = builder.build();

        Assert.assertTrue(services.isSuccessfulBoot());
        Assert.assertTrue(services.getLegacyServices(this.version).isSuccessfulBoot());

        // check that both versions of the legacy model are the same and valid
        checkSubsystemModelTransformation(services, this.version, null, false);
    }

    /**
     * Tests rejected transformation of the model from current version into specified version.
     */
    @Test
    public void testRejections() throws Exception {
        // create builder for current subsystem version
        KernelServicesBuilder builder = createKernelServicesBuilder(this.additionalInitialization);

        // initialize the legacy services and add required jars
        builder.createLegacyKernelServicesBuilder(this.additionalInitialization, this.controller, this.version)
                .addMavenResourceURL(this.getDependencies())
                .addSingleChildFirstClass(AdditionalInitialization.class)
                .dontPersistXml();

        KernelServices services = builder.build();
        Assert.assertTrue(services.isSuccessfulBoot());
        KernelServices legacyServices = services.getLegacyServices(this.version);
        Assert.assertNotNull(legacyServices);
        Assert.assertTrue(legacyServices.isSuccessfulBoot());

        List<ModelNode> operations = builder.parseXmlResource("distributable-web-reject.xml");
        ModelTestUtils.checkFailedTransformedBootOperations(services, this.version, operations, this.createFailedOperationTransformationConfig());
    }

    private FailedOperationTransformationConfig createFailedOperationTransformationConfig() {
        FailedOperationTransformationConfig config = new FailedOperationTransformationConfig();
        PathAddress subsystemAddress = PathAddress.pathAddress(ModelDescriptionConstants.SUBSYSTEM, DistributableWebExtension.SUBSYSTEM_NAME);

        if (DistributableWebSubsystemModel.VERSION_3_0_0.requiresTransformation(this.version)) {
            config.addFailedAttribute(subsystemAddress.append(InfinispanSessionManagementResourceDefinition.pathElement("protostream")), new FailedOperationTransformationConfig.NewAttributesConfig(SessionManagementResourceDefinition.Attribute.MARSHALLER.getName()));
            config.addFailedAttribute(subsystemAddress.append(HotRodSessionManagementResourceDefinition.pathElement("remote-protostream")), new FailedOperationTransformationConfig.NewAttributesConfig(SessionManagementResourceDefinition.Attribute.MARSHALLER.getName()));
        }

        return config;
    }
}
