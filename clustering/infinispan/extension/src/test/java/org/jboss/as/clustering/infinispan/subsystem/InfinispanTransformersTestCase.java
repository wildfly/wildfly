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

import static org.jboss.as.clustering.controller.PropertiesTestUtil.checkMapModels;
import static org.jboss.as.clustering.controller.PropertiesTestUtil.checkMapResults;
import static org.jboss.as.clustering.controller.PropertiesTestUtil.executeOpInBothControllersWithAttachments;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.COMPOSITE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STEPS;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.jboss.as.clustering.controller.CommonRequirement;
import org.jboss.as.clustering.controller.CommonUnaryRequirement;
import org.jboss.as.clustering.infinispan.subsystem.remote.ConnectionPoolResourceDefinition;
import org.jboss.as.clustering.infinispan.subsystem.remote.RemoteCacheContainerResourceDefinition;
import org.jboss.as.clustering.infinispan.subsystem.remote.RemoteClusterResourceDefinition;
import org.jboss.as.clustering.infinispan.subsystem.remote.RemoteTransactionResourceDefinition;
import org.jboss.as.clustering.infinispan.subsystem.remote.SecurityResourceDefinition;
import org.jboss.as.clustering.jgroups.subsystem.JGroupsSubsystemInitialization;
import org.jboss.as.clustering.subsystem.RejectedValueConfig;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
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

    private static String formatEAP6SubsystemArtifact(ModelTestControllerVersion version) {
        return formatArtifact("org.jboss.as:jboss-as-clustering-infinispan:%s", version);
    }

    private static String formatEAP7SubsystemArtifact(ModelTestControllerVersion version) {
        return formatArtifact("org.jboss.eap:wildfly-clustering-infinispan-extension:%s", version);
    }

    private static String formatArtifact(String pattern, ModelTestControllerVersion version) {
        return String.format(pattern, version.getMavenGavVersion());
    }

    private static InfinispanModel getModelVersion(ModelTestControllerVersion controllerVersion) {
        switch (controllerVersion) {
            case EAP_6_4_0:
            case EAP_6_4_7:
                return InfinispanModel.VERSION_1_6_0;
            case EAP_7_0_0:
                return InfinispanModel.VERSION_4_0_0;
            case EAP_7_1_0:
                return InfinispanModel.VERSION_5_0_0;
            case EAP_7_2_0:
                return InfinispanModel.VERSION_8_0_0;
            default:
                throw new IllegalArgumentException();
        }
    }

    private static String[] getDependencies(ModelTestControllerVersion version) {
        switch (version) {
            case EAP_6_4_0:
            case EAP_6_4_7:
                return new String[] {
                        formatEAP6SubsystemArtifact(version),
                        "org.infinispan:infinispan-core:5.2.11.Final-redhat-2",
                        "org.infinispan:infinispan-cachestore-jdbc:5.2.11.Final-redhat-2",
                };
            case EAP_7_0_0:
            case EAP_7_1_0:
            case EAP_7_2_0:
                return new String[] {
                        formatEAP7SubsystemArtifact(version),
                        "org.infinispan:infinispan-commons:9.3.8.Final-redhat-00001",
                        "org.infinispan:infinispan-core:9.3.8.Final-redhat-00001",
                        "org.infinispan:infinispan-cachestore-jdbc:9.3.8.Final-redhat-00001",
                        "org.infinispan:infinispan-client-hotrod:9.3.8.Final-redhat-00001",
                        // Following are needed for InfinispanSubsystemInitialization
                        formatArtifact("org.jboss.eap:wildfly-clustering-common:%s", version),
                        formatArtifact("org.jboss.eap:wildfly-clustering-infinispan-spi:%s", version),
                        formatArtifact("org.jboss.eap:wildfly-clustering-jgroups-extension:%s", version),
                        formatArtifact("org.jboss.eap:wildfly-clustering-jgroups-spi:%s", version),
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
    public void testTransformerEAP640() throws Exception {
        testTransformation(ModelTestControllerVersion.EAP_6_4_0);
    }

    @Test
    public void testTransformerEAP700() throws Exception {
        testTransformation(ModelTestControllerVersion.EAP_7_0_0);
    }

    @Test
    public void testTransformerEAP710() throws Exception {
        testTransformation(ModelTestControllerVersion.EAP_7_1_0);
    }

    @Test
    public void testTransformerEAP720() throws Exception {
        testTransformation(ModelTestControllerVersion.EAP_7_2_0);
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

    @SuppressWarnings("deprecation")
    private void testTransformation(final ModelTestControllerVersion controller) throws Exception {
        final ModelVersion version = getModelVersion(controller).getVersion();
        final String[] dependencies = getDependencies(controller);
        final String subsystemXmlResource = String.format("subsystem-infinispan-transform-%d_%d_%d.xml", version.getMajor(), version.getMinor(), version.getMicro());

        KernelServices services = this.buildKernelServices(readResource(subsystemXmlResource), controller, version, dependencies);

        // check that both versions of the legacy model are the same and valid
        checkSubsystemModelTransformation(services, version, null, false);

        ModelNode transformed = services.readTransformedModel(version);

        if (InfinispanModel.VERSION_3_0_0.requiresTransformation(version)) {
            // Verify that mode=BATCH is translated to mode=NONE, batching=true
            ModelNode cache = transformed.get(InfinispanSubsystemResourceDefinition.PATH.getKeyValuePair()).get(CacheContainerResourceDefinition.pathElement("maximal").getKeyValuePair()).get(LocalCacheResourceDefinition.pathElement("local").getKeyValuePair());
            Assert.assertTrue(cache.hasDefined(CacheResourceDefinition.DeprecatedAttribute.BATCHING.getName()));
            Assert.assertTrue(cache.get(CacheResourceDefinition.DeprecatedAttribute.BATCHING.getName()).asBoolean());
            ModelNode transaction = cache.get(TransactionResourceDefinition.PATH.getKeyValuePair());
            if (transaction.hasDefined(TransactionResourceDefinition.Attribute.MODE.getName())) {
                Assert.assertEquals(TransactionMode.NONE.name(), transaction.get(TransactionResourceDefinition.Attribute.MODE.getName()).asString());
            }

            // Test properties operations
            propertiesMapOperationsTest(services, version);
        }
    }

    private static void propertiesMapOperationsTest(KernelServices services, ModelVersion version) throws Exception {
        final String cacheContainer = "maximal";

        final String testProperty1 = "testProperty1";
        final String testProperty2 = "testProperty2";
        final String testProperty3 = "testProperty3";
        final String testProperty4 = "testProperty4";

        final List<PathAddress> cacheStoreAddresses = new LinkedList<>();

        // Current addresses
        cacheStoreAddresses.add(getBinaryKeyedJDBCCacheStoreAddress(cacheContainer, ReplicatedCacheResourceDefinition.WILDCARD_PATH.getKey(), "cache-with-binary-keyed-store"));
        cacheStoreAddresses.add(getCustomCacheStoreAddress(cacheContainer, ReplicatedCacheResourceDefinition.WILDCARD_PATH.getKey(), "repl"));
        cacheStoreAddresses.add(getFileCacheStoreAddress(cacheContainer, LocalCacheResourceDefinition.WILDCARD_PATH.getKey(), "local"));
        cacheStoreAddresses.add(getMixedKeyedJDBCCacheStoreAddress(cacheContainer, DistributedCacheResourceDefinition.WILDCARD_PATH.getKey(), "dist"));
        cacheStoreAddresses.add(getRemoteCacheStoreAddress(cacheContainer, InvalidationCacheResourceDefinition.WILDCARD_PATH.getKey(), "invalid"));
        cacheStoreAddresses.add(getStringKeyedJDBCCacheStoreAddress(cacheContainer, ReplicatedCacheResourceDefinition.WILDCARD_PATH.getKey(), "cache-with-string-keyed-store"));

        // Legacy addresses
        cacheStoreAddresses.add(getBinaryKeyedJDBCCacheStoreLegacyAddress(cacheContainer, ReplicatedCacheResourceDefinition.WILDCARD_PATH.getKey(), "cache-with-binary-keyed-store"));
        cacheStoreAddresses.add(getCustomCacheStoreLegacyAddress(cacheContainer, ReplicatedCacheResourceDefinition.WILDCARD_PATH.getKey(), "repl"));
        cacheStoreAddresses.add(getFileCacheStoreLegacyAddress(cacheContainer, LocalCacheResourceDefinition.WILDCARD_PATH.getKey(), "local"));
        cacheStoreAddresses.add(getMixedKeyedJDBCCacheStoreLegacyAddress(cacheContainer, DistributedCacheResourceDefinition.WILDCARD_PATH.getKey(), "dist"));
        cacheStoreAddresses.add(getRemoteCacheStoreLegacyAddress(cacheContainer, InvalidationCacheResourceDefinition.WILDCARD_PATH.getKey(), "invalid"));
        cacheStoreAddresses.add(getStringKeyedJDBCCacheStoreLegacyAddress(cacheContainer, ReplicatedCacheResourceDefinition.WILDCARD_PATH.getKey(), "cache-with-string-keyed-store"));

        for (PathAddress storeAddress : cacheStoreAddresses) {

            // Check individual operations

            executeOpInBothControllersWithAttachments(services, version, getCacheStoreUndefinePropertiesOperation(storeAddress));
            checkMapModels(services, version, storeAddress);

            executeOpInBothControllersWithAttachments(services, version, getCacheStorePutPropertyOperation(storeAddress, testProperty1, "true"));
            checkMapResults(services, new ModelNode("true"), version, getCacheStoreGetPropertyOperation(storeAddress, testProperty1));
            checkMapModels(services, version, storeAddress, testProperty1, "true");

            executeOpInBothControllersWithAttachments(services, version, getCacheStorePutPropertyOperation(storeAddress, testProperty2, "false"));
            checkMapResults(services, new ModelNode("true"), version, getCacheStoreGetPropertyOperation(storeAddress, testProperty1));
            checkMapResults(services, new ModelNode("false"), version, getCacheStoreGetPropertyOperation(storeAddress, testProperty2));
            checkMapModels(services, version, storeAddress, testProperty1, "true", testProperty2, "false");

            executeOpInBothControllersWithAttachments(services, version, getCacheStorePutPropertyOperation(storeAddress, testProperty2, "true"));
            checkMapResults(services, new ModelNode("true"), version, getCacheStoreGetPropertyOperation(storeAddress, testProperty1));
            checkMapResults(services, new ModelNode("true"), version, getCacheStoreGetPropertyOperation(storeAddress, testProperty2));
            checkMapModels(services, version, storeAddress, testProperty1, "true", testProperty2, "true");

            executeOpInBothControllersWithAttachments(services, version, getCacheStoreRemovePropertyOperation(storeAddress, testProperty1));
            checkMapResults(services, new ModelNode(), version, getCacheStoreGetPropertyOperation(storeAddress, testProperty1));
            checkMapResults(services, new ModelNode("true"), version, getCacheStoreGetPropertyOperation(storeAddress, testProperty2));
            checkMapModels(services, version, storeAddress, testProperty2, "true");

            executeOpInBothControllersWithAttachments(services, version, getCacheStorePutPropertyOperation(storeAddress, testProperty1, "false"));
            checkMapResults(services, new ModelNode("false"), version, getCacheStoreGetPropertyOperation(storeAddress, testProperty1));
            checkMapResults(services, new ModelNode("true"), version, getCacheStoreGetPropertyOperation(storeAddress, testProperty2));
            checkMapModels(services, version, storeAddress, testProperty1, "false", testProperty2, "true");

            executeOpInBothControllersWithAttachments(services, version, getCacheStoreClearPropertiesOperation(storeAddress));
            checkMapResults(services, new ModelNode(), version, getCacheStoreGetPropertyOperation(storeAddress, testProperty1));
            checkMapResults(services, new ModelNode(), version, getCacheStoreGetPropertyOperation(storeAddress, testProperty2));
            checkMapModels(services, version, storeAddress);


            // Check composite operations
            ModelNode composite = new ModelNode();
            composite.get(OP).set(COMPOSITE);
            composite.get(OP_ADDR).setEmptyList();
            composite.get(STEPS).add(getCacheStorePutPropertyOperation(storeAddress, testProperty3, "false"));
            composite.get(STEPS).add(getCacheStorePutPropertyOperation(storeAddress, testProperty4, "true"));
            composite.get(STEPS).add(getCacheStorePutPropertyOperation(storeAddress, testProperty1, "true"));
            executeOpInBothControllersWithAttachments(services, version, composite);
            // Reread values back
            checkMapResults(services, new ModelNode("false"), version, getCacheStoreGetPropertyOperation(storeAddress, testProperty3));
            checkMapResults(services, new ModelNode("true"), version, getCacheStoreGetPropertyOperation(storeAddress, testProperty4));
            checkMapResults(services, new ModelNode("true"), version, getCacheStoreGetPropertyOperation(storeAddress, testProperty1));

            composite.get(STEPS).setEmptyList();
            composite.get(STEPS).add(getCacheStorePutPropertyOperation(storeAddress, testProperty3, "true"));
            composite.get(STEPS).add(getCacheStorePutPropertyOperation(storeAddress, testProperty4, "false"));
            composite.get(STEPS).add(getCacheStorePutPropertyOperation(storeAddress, testProperty1, "false"));
            executeOpInBothControllersWithAttachments(services, version, composite);
            // Reread values back
            checkMapResults(services, new ModelNode("true"), version, getCacheStoreGetPropertyOperation(storeAddress, testProperty3));
            checkMapResults(services, new ModelNode("false"), version, getCacheStoreGetPropertyOperation(storeAddress, testProperty4));
            checkMapResults(services, new ModelNode("false"), version, getCacheStoreGetPropertyOperation(storeAddress, testProperty1));
            checkMapModels(services, version, storeAddress, testProperty3, "true", testProperty4, "false", testProperty1, "false");

            composite.get(STEPS).setEmptyList();
            composite.get(STEPS).add(getCacheStoreRemovePropertyOperation(storeAddress, testProperty3));
            composite.get(STEPS).add(getCacheStoreRemovePropertyOperation(storeAddress, testProperty4));
            composite.get(STEPS).add(getCacheStorePutPropertyOperation(storeAddress, testProperty2, "false"));
            composite.get(STEPS).add(getCacheStoreRemovePropertyOperation(storeAddress, testProperty1));
            executeOpInBothControllersWithAttachments(services, version, composite);
            // Reread values back
            checkMapResults(services, new ModelNode(), version, getCacheStoreGetPropertyOperation(storeAddress, testProperty3));
            checkMapResults(services, new ModelNode(), version, getCacheStoreGetPropertyOperation(storeAddress, testProperty4));
            checkMapResults(services, new ModelNode(), version, getCacheStoreGetPropertyOperation(storeAddress, testProperty1));
            checkMapResults(services, new ModelNode("false"), version, getCacheStoreGetPropertyOperation(storeAddress, testProperty2));
            checkMapModels(services, version, storeAddress, testProperty2, "false");

            composite.get(STEPS).setEmptyList();
            composite.get(STEPS).add(getCacheStorePutPropertyOperation(storeAddress, testProperty3, "false"));
            composite.get(STEPS).add(getCacheStorePutPropertyOperation(storeAddress, testProperty4, "true"));
            composite.get(STEPS).add(getCacheStoreRemovePropertyOperation(storeAddress, testProperty2));
            executeOpInBothControllersWithAttachments(services, version, composite);
            checkMapResults(services, new ModelNode("false"), version, getCacheStoreGetPropertyOperation(storeAddress, testProperty3));
            checkMapResults(services, new ModelNode("true"), version, getCacheStoreGetPropertyOperation(storeAddress, testProperty4));
            checkMapResults(services, new ModelNode(), version, getCacheStoreGetPropertyOperation(storeAddress, testProperty2));
            checkMapModels(services, version, storeAddress, testProperty3, "false", testProperty4, "true");
        }
    }

    @Test
    public void testRejectionsEAP640() throws Exception {
        testRejections(ModelTestControllerVersion.EAP_6_4_0);
    }

    @Test
    public void testRejectionsEAP700() throws Exception {
        testRejections(ModelTestControllerVersion.EAP_7_0_0);
    }

    @Test
    public void testRejectionsEAP710() throws Exception {
        testRejections(ModelTestControllerVersion.EAP_7_1_0);
    }

    @Test
    public void testRejectionsEAP720() throws Exception {
        testRejections(ModelTestControllerVersion.EAP_7_2_0);
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

    @SuppressWarnings("deprecation")
    private static FailedOperationTransformationConfig createFailedOperationConfig(ModelVersion version) {

        FailedOperationTransformationConfig config = new FailedOperationTransformationConfig();
        PathAddress subsystemAddress = PathAddress.pathAddress(InfinispanSubsystemResourceDefinition.PATH);
        PathAddress containerAddress = subsystemAddress.append(CacheContainerResourceDefinition.WILDCARD_PATH);
        PathAddress remoteContainerAddress = subsystemAddress.append(RemoteCacheContainerResourceDefinition.WILDCARD_PATH);

        if (InfinispanModel.VERSION_12_0_0.requiresTransformation(version) && !InfinispanModel.VERSION_4_0_0.requiresTransformation(version)) {
            config.addFailedAttribute(containerAddress.append(LocalCacheResourceDefinition.WILDCARD_PATH, JDBCStoreResourceDefinition.PATH, StringTableResourceDefinition.PATH), FailedOperationTransformationConfig.REJECTED_RESOURCE);
        }

        if (InfinispanModel.VERSION_11_0_0.requiresTransformation(version)) {
            config.addFailedAttribute(remoteContainerAddress, new FailedOperationTransformationConfig.NewAttributesConfig(RemoteCacheContainerResourceDefinition.Attribute.STATISTICS_ENABLED.getName()));

            if (!InfinispanModel.VERSION_4_0_0.requiresTransformation(version)) {
                PathAddress storeAddress = containerAddress.append(DistributedCacheResourceDefinition.WILDCARD_PATH, MixedKeyedJDBCStoreResourceDefinition.PATH);
                config.addFailedAttribute(storeAddress.append(BinaryTableResourceDefinition.PATH), FailedOperationTransformationConfig.REJECTED_RESOURCE);
                config.addFailedAttribute(storeAddress.append(StringTableResourceDefinition.PATH), FailedOperationTransformationConfig.REJECTED_RESOURCE);
            }
        }

        if (InfinispanModel.VERSION_7_0_0.requiresTransformation(version)) {
            config.addFailedAttribute(containerAddress.append(ReplicatedCacheResourceDefinition.WILDCARD_PATH, StateTransferResourceDefinition.PATH), new RejectedValueConfig(StateTransferResourceDefinition.Attribute.TIMEOUT, value -> value.asLong() <= 0));
            config.addFailedAttribute(remoteContainerAddress, FailedOperationTransformationConfig.REJECTED_RESOURCE);
            for (PathElement path : Arrays.asList(ThreadPoolResourceDefinition.CLIENT.getPathElement(), ConnectionPoolResourceDefinition.PATH, org.jboss.as.clustering.infinispan.subsystem.remote.InvalidationNearCacheResourceDefinition.PATH, RemoteClusterResourceDefinition.WILDCARD_PATH, RemoteTransactionResourceDefinition.PATH, SecurityResourceDefinition.PATH) ) {
                config.addFailedAttribute(remoteContainerAddress.append(path), FailedOperationTransformationConfig.REJECTED_RESOURCE);
            }

            PathAddress cacheAddress = containerAddress.append(ScatteredCacheResourceDefinition.WILDCARD_PATH);
            config.addFailedAttribute(cacheAddress, FailedOperationTransformationConfig.REJECTED_RESOURCE);
            for (PathElement path : Arrays.asList(LockingResourceDefinition.PATH, TransactionResourceDefinition.PATH, HeapMemoryResourceDefinition.PATH, ExpirationResourceDefinition.PATH, StateTransferResourceDefinition.PATH, PartitionHandlingResourceDefinition.PATH)) {
                config.addFailedAttribute(cacheAddress.append(path), FailedOperationTransformationConfig.REJECTED_RESOURCE);
            }
        }

        if (InfinispanModel.VERSION_6_0_0.requiresTransformation(version)) {
            config.addFailedAttribute(containerAddress.append(ReplicatedCacheResourceDefinition.WILDCARD_PATH, OffHeapMemoryResourceDefinition.PATH), FailedOperationTransformationConfig.REJECTED_RESOURCE);
            config.addFailedAttribute(containerAddress.append(DistributedCacheResourceDefinition.WILDCARD_PATH, OffHeapMemoryResourceDefinition.PATH), FailedOperationTransformationConfig.REJECTED_RESOURCE);
        }

        if (InfinispanModel.VERSION_2_0_0.requiresTransformation(version)) {
            for (PathElement path : Arrays.asList(DistributedCacheResourceDefinition.WILDCARD_PATH, ReplicatedCacheResourceDefinition.WILDCARD_PATH)) {
                PathAddress cacheAddress = containerAddress.append(path);
                config.addFailedAttribute(cacheAddress.append(BackupsResourceDefinition.PATH), FailedOperationTransformationConfig.REJECTED_RESOURCE);
                config.addFailedAttribute(cacheAddress.append(BackupsResourceDefinition.PATH, BackupResourceDefinition.WILDCARD_PATH), FailedOperationTransformationConfig.REJECTED_RESOURCE);
                config.addFailedAttribute(cacheAddress.append(BackupForResourceDefinition.PATH), FailedOperationTransformationConfig.REJECTED_RESOURCE);
            }
        }

        return config;
    }
}
