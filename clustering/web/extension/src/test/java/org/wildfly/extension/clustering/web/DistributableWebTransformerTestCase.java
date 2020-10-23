/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.clustering.web;

import java.util.EnumSet;
import java.util.List;

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
import org.wildfly.clustering.infinispan.client.InfinispanClientRequirement;
import org.wildfly.clustering.infinispan.spi.InfinispanCacheRequirement;
import org.wildfly.clustering.infinispan.spi.InfinispanDefaultCacheRequirement;
import org.wildfly.clustering.infinispan.spi.InfinispanRequirement;

/**
 * Transformer tests for distributable-web subsystem.
 * @author Paul Ferraro
 */
@RunWith(value = Parameterized.class)
public class DistributableWebTransformerTestCase extends AbstractSubsystemTest {

    @Parameters
    public static Iterable<ModelTestControllerVersion> parameters() {
        return EnumSet.of(ModelTestControllerVersion.EAP_7_3_0);
    }

    private final ModelTestControllerVersion controller;
    private final org.jboss.as.subsystem.test.AdditionalInitialization additionalInitialization;
    private final ModelVersion version;

    public DistributableWebTransformerTestCase(ModelTestControllerVersion controller) {
        super(DistributableWebExtension.SUBSYSTEM_NAME, new DistributableWebExtension());
        this.controller = controller;
        this.version = this.getModelVersion().getVersion();
        this.additionalInitialization = new AdditionalInitialization()
                .require(InfinispanRequirement.CONTAINER.resolve("foo"))
                .require(InfinispanDefaultCacheRequirement.CACHE.resolve("foo"))
                .require(InfinispanDefaultCacheRequirement.CONFIGURATION.resolve("foo"))
                .require(InfinispanCacheRequirement.CACHE.resolve("foo", "bar"))
                .require(InfinispanCacheRequirement.CONFIGURATION.resolve("foo", "bar"))
                .require(InfinispanClientRequirement.REMOTE_CONTAINER.resolve("foo"))
                .require(InfinispanCacheRequirement.CACHE.resolve("foo", "routing"))
                .require(InfinispanCacheRequirement.CONFIGURATION.resolve("foo", "routing"))
                ;
    }

    private String formatSubsystemArtifact() {
        return this.formatArtifact("org.jboss.eap:wildfly-clustering-web-extension:%s");
    }

    private String formatArtifact(String pattern) {
        return String.format(pattern, this.controller.getMavenGavVersion());
    }

    private DistributableWebModel getModelVersion() {
        switch (this.controller) {
            // Subsystem does not predate EAP 7.3.0
            case EAP_7_3_0:
                return DistributableWebModel.VERSION_2_0_0;
            default:
                throw new IllegalArgumentException();
        }
    }

    private String[] getDependencies() {
        switch (this.controller) {
            case EAP_7_3_0:
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
            default:
                throw new IllegalArgumentException();
        }
    }

    /**
     * Tests transformation of model from current version into specified version.
     */
    @Test
    public void testTransformation() throws Exception {
        String subsystemXmlResource = String.format("wildfly-distributable-web-transform-%d_%d_%d.xml", this.version.getMajor(), this.version.getMinor(), this.version.getMicro());

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

        List<ModelNode> operations = builder.parseXmlResource("wildfly-distributable-web-transform-reject.xml");
        ModelTestUtils.checkFailedTransformedBootOperations(services, this.version, operations, new FailedOperationTransformationConfig());
    }
}
