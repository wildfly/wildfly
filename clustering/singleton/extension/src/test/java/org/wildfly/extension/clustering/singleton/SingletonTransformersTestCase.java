/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.wildfly.extension.clustering.singleton;

import java.util.List;

import org.jboss.as.clustering.controller.CommonUnaryRequirement;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.model.test.FailedOperationTransformationConfig;
import org.jboss.as.model.test.ModelTestControllerVersion;
import org.jboss.as.model.test.ModelTestUtils;
import org.jboss.as.subsystem.test.AbstractSubsystemTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.KernelServicesBuilder;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

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

    private static SingletonModel getModelVersion(ModelTestControllerVersion controllerVersion) {
        switch (controllerVersion) {
            case EAP_7_0_0:
                return SingletonModel.VERSION_1_0_0;
            default:
                throw new IllegalArgumentException();
        }
    }

    private static String[] getDependencies(ModelTestControllerVersion version) {
        switch (version) {
            case EAP_7_0_0:
                return new String[] {
                        formatArtifact("org.jboss.eap:wildfly-clustering-singleton-api:%s", version),
                        formatArtifact("org.jboss.eap:wildfly-clustering-singleton-extension:%s", version),
                        formatArtifact("org.jboss.eap:wildfly-clustering-common:%s", version),
                };
            default:
                throw new IllegalArgumentException();
        }
    }

    private AdditionalInitialization createAdditionalInitialization() {
        return new org.jboss.as.clustering.subsystem.AdditionalInitialization()
                .require(CommonUnaryRequirement.SOCKET_BINDING, "binding0", "binding1")
                ;
    }

    @Test
    public void testTransformerEAP700() throws Exception {
        testTransformation(ModelTestControllerVersion.EAP_7_0_0);
    }

    private void testTransformation(final ModelTestControllerVersion controller) throws Exception {
        final ModelVersion version = getModelVersion(controller).getVersion();
        final String subsystemXmlResource = String.format("subsystem-transform-%d_%d_%d.xml", version.getMajor(), version.getMinor(), version.getMicro());
        final String[] dependencies = getDependencies(controller);

        KernelServices services = this.buildKernelServices(subsystemXmlResource, controller, version, dependencies);

        checkSubsystemModelTransformation(services, version, null, false);
    }

    @Ignore("https://issues.jboss.org/browse/WFCORE-1892")
    @Test
    public void testRejectionsEAP700() throws Exception {
        testRejections(ModelTestControllerVersion.EAP_7_0_0);
    }

    private void testRejections(final ModelTestControllerVersion controller) throws Exception {
        final ModelVersion version = getModelVersion(controller).getVersion();
        final String subsystemXmlResource = String.format("subsystem-reject-%d_%d_%d.xml", version.getMajor(), version.getMinor(), version.getMicro());
        final String[] dependencies = getDependencies(controller);

        // create builder for current subsystem version
        KernelServicesBuilder builder = createKernelServicesBuilder();

        // initialize the legacy services
        builder.createLegacyKernelServicesBuilder(createAdditionalInitialization(), controller, version)
                .addSingleChildFirstClass(org.jboss.as.clustering.subsystem.AdditionalInitialization.class)
                .addMavenResourceURL(dependencies)
                .dontPersistXml();

        KernelServices services = builder.build();
        KernelServices legacyServices = services.getLegacyServices(version);
        Assert.assertNotNull(legacyServices);
        Assert.assertTrue(services.isSuccessfulBoot());
        Assert.assertTrue(legacyServices.isSuccessfulBoot());

        // test failed operations involving backups
        List<ModelNode> xmlOps = builder.parseXmlResource(subsystemXmlResource);
        ModelTestUtils.checkFailedTransformedBootOperations(services, version, xmlOps, createFailedOperationConfig(version));
    }

    private static FailedOperationTransformationConfig createFailedOperationConfig(ModelVersion version) {

        FailedOperationTransformationConfig config = new FailedOperationTransformationConfig();
        PathAddress subsystemAddress = PathAddress.pathAddress(SingletonResourceDefinition.PATH);

        if (SingletonModel.VERSION_2_0_0.requiresTransformation(version)) {
            PathAddress singletonPolicyAddress = subsystemAddress.append(SingletonPolicyResourceDefinition.WILDCARD_PATH);

            config.addFailedAttribute(singletonPolicyAddress, new FailedOperationTransformationConfig.RejectExpressionsConfig(SingletonPolicyResourceDefinition.Attribute.CACHE.getName(), SingletonPolicyResourceDefinition.Attribute.CACHE_CONTAINER.getName()));
        }

        return config;
    }

    private KernelServicesBuilder createKernelServicesBuilder() {
        return this.createKernelServicesBuilder(createAdditionalInitialization());
    }

    private KernelServices buildKernelServices(String subsystemXml, ModelTestControllerVersion controllerVersion, ModelVersion version, String... mavenResourceURLs) throws Exception {
        KernelServicesBuilder builder = this.createKernelServicesBuilder().setSubsystemXmlResource(subsystemXml);

        builder.createLegacyKernelServicesBuilder(null, controllerVersion, version)
                .addMavenResourceURL(mavenResourceURLs)
                .skipReverseControllerCheck()
                .dontPersistXml();

        KernelServices services = builder.build();
        Assert.assertTrue(ModelTestControllerVersion.MASTER + " boot failed", services.isSuccessfulBoot());
        Assert.assertTrue(controllerVersion.getMavenGavVersion() + " boot failed", services.getLegacyServices(version).isSuccessfulBoot());
        return services;
    }
}
