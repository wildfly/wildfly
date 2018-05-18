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
import org.wildfly.clustering.singleton.SingletonCacheRequirement;
import org.wildfly.clustering.singleton.SingletonDefaultCacheRequirement;

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
            case EAP_7_1_0:
                return SingletonModel.VERSION_2_0_0;
            default:
                throw new IllegalArgumentException();
        }
    }

    private static String[] getDependencies(ModelTestControllerVersion version) {
        switch (version) {
            case EAP_7_0_0:
            case EAP_7_1_0:
                return new String[] {
                        formatEAP7SubsystemArtifact(version),
                        formatArtifact("org.jboss.eap:wildfly-clustering-singleton-api:%s", version),
                        formatArtifact("org.jboss.eap:wildfly-clustering-common:%s", version),
                };
            default:
                throw new IllegalArgumentException();
        }
    }

    @SuppressWarnings("deprecation")
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
    public void testTransformerEAP700() throws Exception {
        this.testTransformation(ModelTestControllerVersion.EAP_7_0_0);
    }

    @Test
    public void testTransformerEAP710() throws Exception {
        this.testTransformation(ModelTestControllerVersion.EAP_7_1_0);
    }

    private void testTransformation(final ModelTestControllerVersion controller) throws Exception {
        final ModelVersion version = getModelVersion(controller).getVersion();
        final String[] dependencies = getDependencies(controller);

        KernelServices services = this.buildKernelServices("subsystem-transform.xml", controller, version, dependencies);

        checkSubsystemModelTransformation(services, version, null, false);
    }

    @Test
    public void testRejectionsEAP700() throws Exception {
        this.testRejections(ModelTestControllerVersion.EAP_7_0_0);
    }

    @Test
    public void testRejectionsEAP710() throws Exception {
        this.testRejections(ModelTestControllerVersion.EAP_7_1_0);
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
