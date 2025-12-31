/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.ejb;

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
import org.wildfly.clustering.infinispan.service.InfinispanServiceDescriptor;

/**
 * Transform/reject tests for singleton subsystem.
 *
 * @author Radoslav Husar
 */
@RunWith(value = Parameterized.class)
public class DistributableEjbTransformersTestCase extends AbstractSubsystemTest {

    @Parameters
    public static Iterable<ModelTestControllerVersion> parameters() {
        return EnumSet.of(
                ModelTestControllerVersion.EAP_8_0_0,
                ModelTestControllerVersion.EAP_8_1_0
        );
    }

    private final ModelTestControllerVersion controllerVersion;
    private final ModelVersion version;

    public DistributableEjbTransformersTestCase(ModelTestControllerVersion controllerVersion) {
        super(DistributableEjbSubsystemResourceDefinitionRegistrar.REGISTRATION.getName(), new DistributableEjbExtension());

        this.controllerVersion = controllerVersion;
        this.version = this.getModelVersion().getVersion();
    }

    private DistributableEjbSubsystemModel getModelVersion() {
        return switch (this.controllerVersion) {
            case EAP_8_0_0 -> DistributableEjbSubsystemModel.VERSION_1_0_0;
            case EAP_8_1_0 -> DistributableEjbSubsystemModel.VERSION_1_0_0;
            default -> throw new IllegalArgumentException();
        };
    }

    private String[] getDependencies() {
        return switch (this.controllerVersion) {
            case EAP_8_0_0 -> new String[] {
                    this.controllerVersion.createGAV("wildfly-clustering-ejb-extension"),
                    this.controllerVersion.createGAV("wildfly-clustering-common"),
                    this.controllerVersion.createGAV("wildfly-clustering-ee-infinispan"),
                    this.controllerVersion.createGAV("wildfly-clustering-ejb-infinispan"),
                    this.controllerVersion.createGAV("wildfly-clustering-ejb-spi"),
                    this.controllerVersion.createGAV("wildfly-clustering-infinispan-embedded-service"),
                    this.controllerVersion.createGAV("wildfly-clustering-marshalling-spi"),
                    this.controllerVersion.createGAV("wildfly-clustering-service"),
            };
            case EAP_8_1_0 -> new String[] {
                    this.controllerVersion.createGAV("wildfly-clustering-ejb-extension"),
                    this.controllerVersion.createGAV("wildfly-clustering-common"),
                    this.controllerVersion.createGAV("wildfly-clustering-ejb-spi"),
                    this.controllerVersion.createGAV("wildfly-clustering-infinispan-embedded-service"),
                    this.controllerVersion.createGAV("wildfly-clustering-server-service"),
                    this.controllerVersion.createCoreGAV("wildfly-subsystem"),
            };
            default -> throw new IllegalArgumentException();
        };
    }

    protected org.jboss.as.subsystem.test.AdditionalInitialization createAdditionalInitialization() {
        return new AdditionalInitialization()
                .require(InfinispanServiceDescriptor.DEFAULT_CACHE, "foo")
                .require(InfinispanServiceDescriptor.DEFAULT_CACHE_CONFIGURATION, "foo")
                .require(InfinispanServiceDescriptor.CACHE_CONFIGURATION, "foo", "bar")
                ;
    }

    /**
     * Tests transformation of a subsystem XML file between a current server with current model version and
     * a legacy server with a specified controller version and model version.
     *
     * The subsystem XML file used to initialise the current server needs to be backward-compatible: it should not
     * trigger rejections when being transformed to the legacy server.
     *
     * The test requires one XML file "distributable-ejb-transform-<version>.xml per legacy ModelVersion tested.
     *
     * @throws Exception
     */
    @Test
    public void testTransformation() throws Exception {
        String subsystemXmlResource = String.format("distributable-ejb-transform-%s.xml", this.version);

        // create KernelServices instances for both current and the legacy version
        KernelServices services = this.buildKernelServices(subsystemXmlResource, this.controllerVersion, this.version, this.getDependencies());

        // validate that the transformations between current model and legacy model are (1) consistent and (2) valid
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

        // test that rejections which are expected during transformation do occur
        String rejectionsXmlResource = String.format("distributable-ejb-reject.xml");
        List<ModelNode> xmlOps = builder.parseXmlResource(rejectionsXmlResource);
        ModelTestUtils.checkFailedTransformedBootOperations(services, version, xmlOps, createFailedOperationConfig(version));
    }

    private static FailedOperationTransformationConfig createFailedOperationConfig(ModelVersion version) {
        FailedOperationTransformationConfig config = new FailedOperationTransformationConfig();
        PathAddress subsystemAddress = PathAddress.pathAddress(DistributableEjbSubsystemResourceDefinitionRegistrar.REGISTRATION.getPathElement());

        if (DistributableEjbSubsystemModel.VERSION_2_0_0.requiresTransformation(version)) {
            config.addFailedAttribute(subsystemAddress.append(PathElement.pathElement(BeanManagementResourceRegistration.INFINISPAN.getPathElement().getKey(), "default")), new FailedOperationTransformationConfig.NewAttributesConfig(BeanManagementResourceDefinitionRegistrar.IDLE_THRESHOLD));
            config.addFailedAttribute(subsystemAddress.append(PathElement.pathElement(InfinispanTimerManagementResourceDefinitionRegistrar.REGISTRATION.getPathElement().getKey(), "distributed")), new FailedOperationTransformationConfig.NewAttributesConfig(InfinispanTimerManagementResourceDefinitionRegistrar.IDLE_THRESHOLD));
        }
        //
        PathAddress ejbClientServicesAddress = subsystemAddress.append(PathElement.pathElement("ejb-client-services", "local"));

        if (DistributableEjbSubsystemModel.VERSION_1_0_0.requiresTransformation(version)) {
            // reject the use of ejb-client-services child resource as it does not exist in 1_0_0
            config.addFailedAttribute(ejbClientServicesAddress, FailedOperationTransformationConfig.REJECTED_RESOURCE);
        }
        return config;
    }

    private KernelServicesBuilder createKernelServicesBuilder() {
        return this.createKernelServicesBuilder(this.createAdditionalInitialization());
    }

    /**
     * Builds an instance of KernelServices representing current as well as an instance of legacy KernelServices
     * representing a specific server version and model version.
     *
     * @param subsystemXml  the XML used to initialise the subsystem
     * @param controllerVersion the server version used to initialise the legacy KernelServices
     * @param version the model version used to initialise the legacy services
     * @param mavenResourceURLs additional server modules required to initialize the legacy KernelServices
     * @return a configured KernelServices instance
     * @throws Exception
     */
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
