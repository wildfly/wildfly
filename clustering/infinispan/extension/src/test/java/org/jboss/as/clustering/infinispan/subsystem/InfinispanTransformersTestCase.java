/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.clustering.infinispan.subsystem;

import java.util.LinkedList;
import java.util.List;

import org.jboss.as.clustering.controller.CommonRequirement;
import org.jboss.as.clustering.controller.CommonUnaryRequirement;
import org.jboss.as.clustering.infinispan.subsystem.remote.RemoteCacheContainerResourceDefinition;
import org.jboss.as.clustering.jgroups.subsystem.JGroupsSubsystemInitialization;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.model.test.FailedOperationTransformationConfig;
import org.jboss.as.model.test.ModelTestControllerVersion;
import org.jboss.as.model.test.ModelTestUtils;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.KernelServicesBuilder;
import org.jboss.dmr.ModelNode;
import org.jgroups.conf.ClassConfigurator;
import org.junit.Assert;
import org.junit.Test;
import org.wildfly.clustering.jgroups.spi.JGroupsDefaultRequirement;
import org.wildfly.clustering.jgroups.spi.JGroupsRequirement;

/**
 * Test cases for transformers used in the Infinispan subsystem
 *
 * Here we perform two types of tests:
 * - testing transformation between the current model and legacy models, in the case
 * where no rejections are expected, but certain discards/conversions/renames are expected
 * - testing transformation between the current model and legacy models, in the case
 * where specific rejections are expected
 *
 * @author <a href="tomaz.cerar@redhat.com">Tomaz Cerar</a>
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 * @author Radoslav Husar
 */
public class InfinispanTransformersTestCase extends OperationTestCaseBase {

    private static String formatArtifact(String pattern, ModelTestControllerVersion version) {
        return String.format(pattern, version.getMavenGavVersion());
    }

    private static InfinispanModel getModelVersion(ModelTestControllerVersion controllerVersion) {
        switch (controllerVersion) {
            case EAP_7_4_0:
                return InfinispanModel.VERSION_14_0_0;
            default:
                throw new IllegalArgumentException();
        }
    }

    private static String[] getDependencies(ModelTestControllerVersion version) {
        switch (version) {
            case EAP_7_4_0:
                return new String[] {
                        formatArtifact("org.jboss.eap:wildfly-clustering-infinispan-extension:%s", version),
                        "org.infinispan:infinispan-commons:11.0.9.Final-redhat-00001",
                        "org.infinispan:infinispan-core:11.0.9.Final-redhat-00001",
                        "org.infinispan:infinispan-cachestore-jdbc:11.0.9.Final-redhat-00001",
                        "org.infinispan:infinispan-client-hotrod:11.0.9.Final-redhat-00001",
                        // Following are needed for InfinispanSubsystemInitialization
                        formatArtifact("org.jboss.eap:wildfly-clustering-api:%s", version),
                        formatArtifact("org.jboss.eap:wildfly-clustering-common:%s", version),
                        formatArtifact("org.jboss.eap:wildfly-clustering-infinispan-client:%s", version),
                        formatArtifact("org.jboss.eap:wildfly-clustering-infinispan-spi:%s", version),
                        formatArtifact("org.jboss.eap:wildfly-clustering-jgroups-extension:%s", version),
                        formatArtifact("org.jboss.eap:wildfly-clustering-jgroups-spi:%s", version),
                        formatArtifact("org.jboss.eap:wildfly-clustering-server:%s", version),
                        formatArtifact("org.jboss.eap:wildfly-clustering-service:%s", version),
                        formatArtifact("org.jboss.eap:wildfly-clustering-singleton-api:%s", version),
                        formatArtifact("org.jboss.eap:wildfly-clustering-spi:%s", version),
                        formatArtifact("org.jboss.eap:wildfly-connector:%s", version),
                };
            default: {
                throw new IllegalArgumentException();
            }
        }
    }

    @Override
    AdditionalInitialization createAdditionalInitialization() {
        return new InfinispanSubsystemInitialization()
                .require(CommonRequirement.PATH_MANAGER)
                .require(CommonUnaryRequirement.PATH, ServerEnvironment.SERVER_TEMP_DIR)
                .require(CommonUnaryRequirement.OUTBOUND_SOCKET_BINDING, "hotrod-server-1", "hotrod-server-2", "jdg1", "jdg2", "jdg3", "jdg4", "jdg5", "jdg6")
                .require(CommonUnaryRequirement.DATA_SOURCE, "ExampleDS")
                .require(CommonUnaryRequirement.SSL_CONTEXT, "hotrod-elytron")
                .require(JGroupsRequirement.CHANNEL_FACTORY, "maximal-channel")
                .require(JGroupsDefaultRequirement.CHANNEL_FACTORY)
                .require(CommonRequirement.LOCAL_TRANSACTION_PROVIDER)
                .require(TransactionResourceDefinition.TransactionRequirement.XA_RESOURCE_RECOVERY_REGISTRY)
                ;
    }

    @Test
    public void testTransformerEAP740() throws Exception {
        testTransformation(ModelTestControllerVersion.EAP_7_4_0);
    }

    private KernelServices buildKernelServices(String xml, ModelTestControllerVersion controllerVersion, ModelVersion version, String... mavenResourceURLs) throws Exception {
        KernelServicesBuilder builder = this.createKernelServicesBuilder().setSubsystemXml(xml);

        builder.createLegacyKernelServicesBuilder(createAdditionalInitialization(), controllerVersion, version)
                .addMavenResourceURL(mavenResourceURLs)
                .addSingleChildFirstClass(InfinispanSubsystemInitialization.class)
                .addSingleChildFirstClass(JGroupsSubsystemInitialization.class)
                .addSingleChildFirstClass(org.jboss.as.clustering.subsystem.AdditionalInitialization.class)
                .addSingleChildFirstClass(ClassConfigurator.class)
                .skipReverseControllerCheck()
                .dontPersistXml();

        KernelServices services = builder.build();
        Assert.assertTrue(ModelTestControllerVersion.MASTER + " boot failed", services.isSuccessfulBoot());
        Assert.assertTrue(controllerVersion.getMavenGavVersion() + " boot failed", services.getLegacyServices(version).isSuccessfulBoot());
        return services;
    }

    private void testTransformation(final ModelTestControllerVersion controller) throws Exception {
        final ModelVersion version = getModelVersion(controller).getVersion();
        final String[] dependencies = getDependencies(controller);
        final String subsystemXmlResource = String.format("subsystem-infinispan-transform-%d_%d_%d.xml", version.getMajor(), version.getMinor(), version.getMicro());

        KernelServices services = this.buildKernelServices(readResource(subsystemXmlResource), controller, version, dependencies);

        // check that both versions of the legacy model are the same and valid
        checkSubsystemModelTransformation(services, version, null, false);
    }

    @Test
    public void testRejectionsEAP740() throws Exception {
        testRejections(ModelTestControllerVersion.EAP_7_4_0);
    }

    private void testRejections(final ModelTestControllerVersion controller) throws Exception {
        final ModelVersion version = getModelVersion(controller).getVersion();
        final String[] dependencies = getDependencies(controller);

        // create builder for current subsystem version
        KernelServicesBuilder builder = this.createKernelServicesBuilder();

        // initialize the legacy services
        builder.createLegacyKernelServicesBuilder(createAdditionalInitialization(), controller, version)
                .addSingleChildFirstClass(InfinispanSubsystemInitialization.class)
                .addSingleChildFirstClass(JGroupsSubsystemInitialization.class)
                .addSingleChildFirstClass(org.jboss.as.clustering.subsystem.AdditionalInitialization.class)
                .addSingleChildFirstClass(ClassConfigurator.class)
                .addMavenResourceURL(dependencies)
                //TODO storing the model triggers the weirdness mentioned in SubsystemTestDelegate.LegacyKernelServiceInitializerImpl.install()
                //which is strange since it should be loading it all from the current jboss modules
                //Also this works in several other tests
                .dontPersistXml();

        KernelServices services = builder.build();
        KernelServices legacyServices = services.getLegacyServices(version);
        Assert.assertNotNull(legacyServices);
        Assert.assertTrue("main services did not boot", services.isSuccessfulBoot());
        Assert.assertTrue(legacyServices.isSuccessfulBoot());

        // test failed operations involving backups
        List<ModelNode> operations = builder.parseXmlResource("subsystem-infinispan-transformer-reject.xml");
        ModelTestUtils.checkFailedTransformedBootOperations(services, version, operations, createFailedOperationConfig(version));
    }

    private static FailedOperationTransformationConfig createFailedOperationConfig(ModelVersion version) {

        FailedOperationTransformationConfig config = new FailedOperationTransformationConfig();
        PathAddress subsystemAddress = PathAddress.pathAddress(InfinispanSubsystemResourceDefinition.PATH);
        PathAddress containerAddress = subsystemAddress.append(CacheContainerResourceDefinition.WILDCARD_PATH);
        PathAddress remoteContainerAddress = subsystemAddress.append(RemoteCacheContainerResourceDefinition.WILDCARD_PATH);
        List<String> rejectedRemoteContainerAttributes = new LinkedList<>();

        if (InfinispanModel.VERSION_15_0_0.requiresTransformation(version)) {
            config.addFailedAttribute(containerAddress, new FailedOperationTransformationConfig.NewAttributesConfig(CacheContainerResourceDefinition.Attribute.MARSHALLER.getDefinition()));
            rejectedRemoteContainerAttributes.add(RemoteCacheContainerResourceDefinition.Attribute.MARSHALLER.getName());
        }

        if (InfinispanModel.VERSION_14_0_0.requiresTransformation(version)) {
            rejectedRemoteContainerAttributes.add(RemoteCacheContainerResourceDefinition.ListAttribute.MODULES.getName());
        }

        if (!rejectedRemoteContainerAttributes.isEmpty()) {
            config.addFailedAttribute(remoteContainerAddress, new FailedOperationTransformationConfig.NewAttributesConfig(rejectedRemoteContainerAttributes.toArray(new String[rejectedRemoteContainerAttributes.size()])));
        }

        return config;
    }
}
