/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.singleton;

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
import org.wildfly.clustering.singleton.service.SingletonServiceTargetFactory;

/**
 * @author Radoslav Husar
 */
public class SingletonTransformersTestCase extends AbstractSubsystemTest {

    public SingletonTransformersTestCase() {
        super(SingletonExtension.SUBSYSTEM_NAME, new SingletonExtension());
    }

    private static String formatArtifact(String pattern, ModelTestControllerVersion version) {
        return String.format(pattern, version.getMavenGavVersion());
    }

    private static String formatEAP7SubsystemArtifact(ModelTestControllerVersion version) {
        return formatArtifact("org.jboss.eap:wildfly-clustering-singleton-extension:%s", version);
    }

    private static SingletonSubsystemModel getModelVersion(ModelTestControllerVersion controllerVersion) {
        switch (controllerVersion) {
            case EAP_7_4_0:
                return SingletonSubsystemModel.VERSION_3_0_0;
            default:
                throw new IllegalArgumentException();
        }
    }

    private static String[] getDependencies(ModelTestControllerVersion version) {
        switch (version) {
            case EAP_7_4_0:
                return new String[] {
                        formatEAP7SubsystemArtifact(version),
                        formatArtifact("org.jboss.eap:wildfly-clustering-api:%s", version),
                        formatArtifact("org.jboss.eap:wildfly-clustering-common:%s", version),
                        formatArtifact("org.jboss.eap:wildfly-clustering-server:%s", version),
                        formatArtifact("org.jboss.eap:wildfly-clustering-service:%s", version),
                        formatArtifact("org.jboss.eap:wildfly-clustering-singleton-api:%s", version),
                        formatArtifact("org.jboss.eap:wildfly-clustering-spi:%s", version),
                };
            default:
                throw new IllegalArgumentException();
        }
    }

    @SuppressWarnings("deprecation")
    protected org.jboss.as.subsystem.test.AdditionalInitialization createAdditionalInitialization() {
        return new AdditionalInitialization()
                .require(OutboundSocketBinding.SERVICE_DESCRIPTOR, "binding0")
                .require(OutboundSocketBinding.SERVICE_DESCRIPTOR, "binding1")
                .require(SingletonServiceTargetFactory.DEFAULT_SERVICE_DESCRIPTOR, "singleton-container")
                .require(org.wildfly.clustering.singleton.service.SingletonServiceConfiguratorFactory.DEFAULT_SERVICE_DESCRIPTOR, "singleton-container")
                .require(SingletonServiceTargetFactory.SERVICE_DESCRIPTOR, "singleton-container", "singleton-cache")
                .require(org.wildfly.clustering.singleton.service.SingletonServiceConfiguratorFactory.SERVICE_DESCRIPTOR, "singleton-container", "singleton-cache")
                ;
    }

    @Test
    public void testTransformerEAP740() throws Exception {
        this.testTransformation(ModelTestControllerVersion.EAP_7_4_0);
    }

    private void testTransformation(final ModelTestControllerVersion controller) throws Exception {
        final ModelVersion version = getModelVersion(controller).getVersion();
        final String[] dependencies = getDependencies(controller);

        KernelServices services = this.buildKernelServices("subsystem-transform.xml", controller, version, dependencies);

        checkSubsystemModelTransformation(services, version, null, false);
    }

    @Test
    public void testRejectionsEAP740() throws Exception {
        this.testRejections(ModelTestControllerVersion.EAP_7_4_0);
    }

    private void testRejections(final ModelTestControllerVersion controller) throws Exception {
        ModelVersion version = getModelVersion(controller).getVersion();
        String[] dependencies = getDependencies(controller);

        // create builder for current subsystem version
        KernelServicesBuilder builder = createKernelServicesBuilder();

        // initialize the legacy services
        builder.createLegacyKernelServicesBuilder(this.createAdditionalInitialization(), controller, version)
                .addSingleChildFirstClass(AdditionalInitialization.class)
                .addMavenResourceURL(dependencies)
        ;

        KernelServices services = builder.build();
        KernelServices legacyServices = services.getLegacyServices(version);
        Assert.assertNotNull(legacyServices);
        Assert.assertTrue(services.isSuccessfulBoot());
        Assert.assertTrue(legacyServices.isSuccessfulBoot());

        // test failed operations involving backups
        List<ModelNode> xmlOps = builder.parseXmlResource("subsystem-reject.xml");
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
