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
import org.jboss.as.controller.PathElement;
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
 */
@RunWith(value = Parameterized.class)
public class DistributableWebTransformerTestCase extends AbstractSubsystemTest {

    @Parameters
    public static Iterable<ModelTestControllerVersion> parameters() {
        return EnumSet.of(ModelTestControllerVersion.EAP_7_4_0, ModelTestControllerVersion.EAP_8_0_0);
    }

    private final ModelTestControllerVersion controller;
    private final org.jboss.as.subsystem.test.AdditionalInitialization additionalInitialization;
    private final ModelVersion version;

    public DistributableWebTransformerTestCase(ModelTestControllerVersion controller) {
        super(DistributableWebSubsystemResourceDefinitionRegistrar.REGISTRATION.getName(), new DistributableWebExtension());
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

    private String formatSubsystemArtifact() {
        return this.formatArtifact("org.jboss.eap:wildfly-clustering-web-extension:%s");
    }

    private String formatArtifact(String pattern) {
        return String.format(pattern, this.controller.getMavenGavVersion());
    }

    private DistributableWebSubsystemModel getModelVersion() {
        switch (this.controller) {
            case EAP_7_4_0:
                return DistributableWebSubsystemModel.VERSION_2_0_0;
            case EAP_8_0_0:
                return DistributableWebSubsystemModel.VERSION_4_0_0;
            default:
                throw new IllegalArgumentException();
        }
    }

    private String[] getDependencies() {
        switch (this.controller) {
            case EAP_7_4_0:
                return new String[] {
                        formatSubsystemArtifact(),
                        formatArtifact("org.jboss.eap:wildfly-clustering-common:%s"),
                        formatArtifact("org.jboss.eap:wildfly-clustering-ee-hotrod:%s"),
                        formatArtifact("org.jboss.eap:wildfly-clustering-ee-infinispan:%s"),
                        formatArtifact("org.jboss.eap:wildfly-clustering-ee-spi:%s"),
                        formatArtifact("org.jboss.eap:wildfly-clustering-infinispan-client:%s"),
                        formatArtifact("org.jboss.eap:wildfly-clustering-infinispan-spi:%s"),
                        formatArtifact("org.jboss.eap:wildfly-clustering-marshalling-spi:%s"),
                        formatArtifact("org.jboss.eap:wildfly-clustering-service:%s"),
                        formatArtifact("org.jboss.eap:wildfly-clustering-web-container:%s"),
                        formatArtifact("org.jboss.eap:wildfly-clustering-web-hotrod:%s"),
                        formatArtifact("org.jboss.eap:wildfly-clustering-web-infinispan:%s"),
                        formatArtifact("org.jboss.eap:wildfly-clustering-web-spi:%s"),
                };
            case EAP_8_0_0:
                return new String[] {
                        formatSubsystemArtifact(),
                        formatArtifact("org.jboss.eap:wildfly-clustering-common:%s"),
                        formatArtifact("org.jboss.eap:wildfly-clustering-ee-hotrod:%s"),
                        formatArtifact("org.jboss.eap:wildfly-clustering-ee-infinispan:%s"),
                        formatArtifact("org.jboss.eap:wildfly-clustering-ee-spi:%s"),
                        formatArtifact("org.jboss.eap:wildfly-clustering-infinispan-client-service:%s"),
                        formatArtifact("org.jboss.eap:wildfly-clustering-infinispan-embedded-service:%s"),
                        formatArtifact("org.jboss.eap:wildfly-clustering-marshalling-spi:%s"),
                        formatArtifact("org.jboss.eap:wildfly-clustering-service:%s"),
                        formatArtifact("org.jboss.eap:wildfly-clustering-web-container:%s"),
                        formatArtifact("org.jboss.eap:wildfly-clustering-web-hotrod:%s"),
                        formatArtifact("org.jboss.eap:wildfly-clustering-web-infinispan:%s"),
                        formatArtifact("org.jboss.eap:wildfly-clustering-web-service:%s"),
                        formatArtifact("org.jboss.eap:wildfly-clustering-web-spi:%s"),
                };
            default:
                throw new IllegalArgumentException();
        }
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
        PathAddress subsystemAddress = PathAddress.pathAddress(DistributableWebSubsystemResourceDefinitionRegistrar.REGISTRATION.getPathElement());

        if (DistributableWebSubsystemModel.VERSION_3_0_0.requiresTransformation(this.version)) {
            config.addFailedAttribute(subsystemAddress.append(PathElement.pathElement(SessionManagementResourceRegistration.INFINISPAN.getPathElement().getKey(), "protostream")), new FailedOperationTransformationConfig.NewAttributesConfig(SessionManagementResourceDefinitionRegistrar.MARSHALLER.getName()));
            config.addFailedAttribute(subsystemAddress.append(PathElement.pathElement(SessionManagementResourceRegistration.HOTROD.getPathElement().getKey(), "remote-protostream")), new FailedOperationTransformationConfig.NewAttributesConfig(SessionManagementResourceDefinitionRegistrar.MARSHALLER.getName()));
        }

        return config;
    }
}
