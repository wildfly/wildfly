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

import static org.jboss.as.clustering.infinispan.subsystem.ModelKeys.CACHE_CONTAINER;
import static org.jboss.as.clustering.infinispan.subsystem.ModelKeys.DISTRIBUTED_CACHE;
import static org.jboss.as.clustering.infinispan.subsystem.ModelKeys.INVALIDATION_CACHE;
import static org.jboss.as.clustering.infinispan.subsystem.ModelKeys.LOCAL_CACHE;
import static org.jboss.as.clustering.infinispan.subsystem.ModelKeys.REPLICATED_CACHE;
import static org.jboss.as.clustering.infinispan.subsystem.ModelKeys.STATISTICS;
import static org.jboss.as.clustering.infinispan.subsystem.ModelKeys.VIRTUAL_NODES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;

import java.io.IOException;
import java.util.List;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.transform.OperationTransformer;
import org.jboss.as.model.test.FailedOperationTransformationConfig;
import org.jboss.as.model.test.FailedOperationTransformationConfig.ChainedConfig;
import org.jboss.as.model.test.FailedOperationTransformationConfig.RejectExpressionsConfig;
import org.jboss.as.model.test.ModelFixer;
import org.jboss.as.model.test.ModelTestControllerVersion;
import org.jboss.as.model.test.ModelTestUtils;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.KernelServicesBuilder;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Test;

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
 */

//@RunWith(BMUnitRunner.class)
public class TransformersTestCase extends OperationTestCaseBase {

    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("infinispan-transformer-2_0.xml");
    }

    @Test
    public void testTransformer712() throws Exception {
        testTransformer_1_3_0(ModelTestControllerVersion.V7_1_2_FINAL);
    }

    @Test
    public void testTransformer713() throws Exception {
        testTransformer_1_3_0(ModelTestControllerVersion.V7_1_3_FINAL);
    }

    @Test
    public void testTransformer720() throws Exception {
        testTransformer_1_4_0(ModelTestControllerVersion.V7_2_0_FINAL);
    }

    @Test
    public void testTransformer600() throws Exception {
        testTransformer_1_3_0(ModelTestControllerVersion.EAP_6_0_0);
    }

    @Test
    public void testTransformer601() throws Exception {
        testTransformer_1_3_0(ModelTestControllerVersion.EAP_6_0_1);
    }

    @Test
    public void testTransformer610() throws Exception {
        testTransformer_1_4_1(ModelTestControllerVersion.EAP_6_1_0);
    }

    @Test
    public void testTransformer611() throws Exception {
        testTransformer_1_4_1(ModelTestControllerVersion.EAP_6_1_1);
    }

    /*
     * Check transformation from current model version to 1.3.0 model version,
     * when no rejections are involved.
     *
     * We do this by checking that;
     * - both the current kernel services and legacy kernel services boot correctly
     * - both versions of the transformed model (resource transformation vs operation transformation)
     * are the same and are valid according to the legacy subsystem description
     * - any specific requirements for discarding/renaming/converting have been performed
     */
    private void testTransformer_1_3_0(ModelTestControllerVersion controllerVersion) throws Exception {
        ModelVersion version130 = ModelVersion.create(1, 3);
        KernelServicesBuilder builder = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT)
                .setSubsystemXml(getSubsystemXml());
        builder.createLegacyKernelServicesBuilder(null, controllerVersion, version130)
            .addMavenResourceURL("org.jboss.as:jboss-as-clustering-infinispan:" + controllerVersion.getMavenGavVersion())
            .addMavenResourceURL("org.infinispan:infinispan-core:5.2.6.Final")
            .configureReverseControllerCheck(null, new FixReverseControllerModel130())
            //TODO storing the model triggers the weirdness mentioned in SubsystemTestDelegate.LegacyKernelServiceInitializerImpl.install()
            //which is strange since it should be loading it all from the current jboss modules
            //Also this works in several other tests
            .dontPersistXml();

        KernelServices mainServices = builder.build();
        Assert.assertTrue("main services did not boot", mainServices.isSuccessfulBoot());
        Assert.assertTrue(mainServices.getLegacyServices(version130).isSuccessfulBoot());

        // check that both versions of the legacy model are the same and valid
        checkSubsystemModelTransformation(mainServices, version130);

        // 1.3.0 API specific checks:
        // - check that segments is translated into virtual nodes
        // - check both segments and indexing properties are removed
        ModelNode model = mainServices.readTransformedModel(version130);
        ModelNode distCache = model.get(SUBSYSTEM,"infinispan",ModelKeys.CACHE_CONTAINER, "maximal", ModelKeys.DISTRIBUTED_CACHE, "dist") ;
        Assert.assertFalse(distCache.has(ModelKeys.INDEXING_PROPERTIES));
        Assert.assertFalse(distCache.has(ModelKeys.SEGMENTS));
        Assert.assertTrue(distCache.get(ModelKeys.VIRTUAL_NODES).isDefined());
    }

    /*
     * Check transformation from current model version to 1.4.0 model version.
     *
     * We do this by checking that;
     * - both the current kernel services and legacy kernel services boot correctly
     * - both versions of the transformed model (resource transformation vs operation transformation)
     * are the same and are valid according to the legacy subsystem description
     * - any specific requirements for discarding/renaming/converting have been performed
     */
    public void testTransformer_1_4_0(ModelTestControllerVersion controllerVersion) throws Exception {
        ModelVersion version140 = ModelVersion.create(1, 4);
        // create builder for current subsystem version
        KernelServicesBuilder builder = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT)
                .setSubsystemXml(getSubsystemXml());
        builder.createLegacyKernelServicesBuilder(null,controllerVersion, version140)
                .addMavenResourceURL("org.jboss.as:jboss-as-clustering-infinispan:7.2.0.Final")
                .addMavenResourceURL("org.infinispan:infinispan-core:5.3.0.Final")
                .addMavenResourceURL("org.infinispan:infinispan-cachestore-jdbc:5.3.0.Final")
                .configureReverseControllerCheck(null, new FixReverseControllerModel140());

        KernelServices mainServices = builder.build();
        KernelServices legacyServices = mainServices.getLegacyServices(version140);
        Assert.assertNotNull(legacyServices);
        Assert.assertTrue("main services did not boot", mainServices.isSuccessfulBoot());
        Assert.assertTrue(legacyServices.isSuccessfulBoot());

        // check that both versions of the legacy model are the same and valid
        // TODO: need a model fixer to make this work
        checkSubsystemModelTransformation(mainServices, version140, new VirtualNodesTransformedModelProblem());

        // 1.4.0 API specific checks
        // - check transformation of virtual nodes into segments
        // - check that statistics is discarded
        PathAddress pa = PathAddress.pathAddress(PathElement.pathElement(SUBSYSTEM, InfinispanExtension.SUBSYSTEM_NAME),
                PathElement.pathElement(CacheContainerResourceDefinition.CONTAINER_PATH.getKey(), "container"),
                PathElement.pathElement(DistributedCacheResourceDefinition.DISTRIBUTED_CACHE_PATH.getKey(), "cache"));
        ModelNode addOp = Util.createAddOperation(pa);
        addOp.get(DistributedCacheResourceDefinition.VIRTUAL_NODES.getName()).set(4);
        addOp.get(CacheResourceDefinition.STATISTICS.getName()).set(true);

        OperationTransformer.TransformedOperation transformedOperation = mainServices.transformOperation(version140, addOp);
        Assert.assertFalse(transformedOperation.getTransformedOperation().has(DistributedCacheResourceDefinition.VIRTUAL_NODES.getName()));
        Assert.assertEquals(24, transformedOperation.getTransformedOperation().get(DistributedCacheResourceDefinition.SEGMENTS.getName()).asInt());

        ModelNode result = new ModelNode();
        result.get(OUTCOME).set(SUCCESS);
        result.get(RESULT);
        Assert.assertFalse(transformedOperation.rejectOperation(result));
        Assert.assertEquals(result, transformedOperation.transformResult(result));

        ModelNode writeOp = Util.createEmptyOperation(WRITE_ATTRIBUTE_OPERATION, pa);
        writeOp.get(NAME).set(DistributedCacheResourceDefinition.VIRTUAL_NODES.getName());
        writeOp.get(VALUE).set(4);

        transformedOperation = mainServices.transformOperation(version140, writeOp);
        Assert.assertEquals(DistributedCacheResourceDefinition.SEGMENTS.getName(), transformedOperation.getTransformedOperation().get(NAME).asString());
        Assert.assertEquals(24, transformedOperation.getTransformedOperation().get(VALUE).asInt());
        Assert.assertFalse(transformedOperation.rejectOperation(result));
        Assert.assertEquals(result, transformedOperation.transformResult(result));
    }

    /*
     * Check transformation from current model version to 1.4.0 model version.
     *
     * We do this by checking that;
     * - both the current kernel services and legacy kernel services boot correctly
     * - both versions of the transformed model (resource transformation vs operation transformation)
     * are the same and are valid according to the legacy subsystem description
     * - any specific requirements for discarding/renaming/converting have been performed
     */
    public void testTransformer_1_4_1(ModelTestControllerVersion controllerVersion) throws Exception {
        ModelVersion version141 = ModelVersion.create(1, 4, 1);
        // create builder for current subsystem version
        KernelServicesBuilder builder = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT)
                .setSubsystemXml(getSubsystemXml());
        builder.createLegacyKernelServicesBuilder(null,controllerVersion, version141)
                .addMavenResourceURL("org.jboss.as:jboss-as-clustering-infinispan:7.2.0.Final")
                .addMavenResourceURL("org.infinispan:infinispan-core:5.3.0.Final")
                .addMavenResourceURL("org.infinispan:infinispan-cachestore-jdbc:5.3.0.Final")
                .configureReverseControllerCheck(null, new FixReverseControllerModel141());

        KernelServices mainServices = builder.build();
        KernelServices legacyServices = mainServices.getLegacyServices(version141);
        Assert.assertNotNull(legacyServices);
        Assert.assertTrue("main services did not boot", mainServices.isSuccessfulBoot());
        Assert.assertTrue(legacyServices.isSuccessfulBoot());

        // check that both versions of the legacy model are the same and valid
        checkSubsystemModelTransformation(mainServices, version141);

        // 1.4.0 API specific checks
        // - check transformation of virtual nodes into segments
        // - check that statistics is discarded
        PathAddress pa = PathAddress.pathAddress(PathElement.pathElement(SUBSYSTEM, InfinispanExtension.SUBSYSTEM_NAME),
                PathElement.pathElement(CacheContainerResourceDefinition.CONTAINER_PATH.getKey(), "container"),
                PathElement.pathElement(DistributedCacheResourceDefinition.DISTRIBUTED_CACHE_PATH.getKey(), "cache"));
        ModelNode addOp = Util.createAddOperation(pa);
        addOp.get(DistributedCacheResourceDefinition.VIRTUAL_NODES.getName()).set(1);
        addOp.get(CacheResourceDefinition.STATISTICS.getName()).set(true);

        OperationTransformer.TransformedOperation transformedOperation = mainServices.transformOperation(version141, addOp);
        Assert.assertFalse(transformedOperation.getTransformedOperation().has(DistributedCacheResourceDefinition.VIRTUAL_NODES.getName()));
        Assert.assertEquals(6, transformedOperation.getTransformedOperation().get(DistributedCacheResourceDefinition.SEGMENTS.getName()).asInt());

        ModelNode result = new ModelNode();
        result.get(OUTCOME).set(SUCCESS);
        result.get(RESULT);
        Assert.assertFalse(transformedOperation.rejectOperation(result));
        Assert.assertEquals(result, transformedOperation.transformResult(result));

        ModelNode writeOp = Util.createEmptyOperation(WRITE_ATTRIBUTE_OPERATION, pa);
        writeOp.get(NAME).set(DistributedCacheResourceDefinition.VIRTUAL_NODES.getName());
        writeOp.get(VALUE).set(1);

        transformedOperation = mainServices.transformOperation(version141, writeOp);
        Assert.assertEquals(DistributedCacheResourceDefinition.SEGMENTS.getName(), transformedOperation.getTransformedOperation().get(NAME).asString());
        Assert.assertEquals(6, transformedOperation.getTransformedOperation().get(VALUE).asInt());
        Assert.assertFalse(transformedOperation.rejectOperation(result));
        Assert.assertEquals(result, transformedOperation.transformResult(result));

    }

    @Test
    public void testRejections712() throws Exception {
        testRejections_1_3_0(ModelTestControllerVersion.V7_1_2_FINAL);
    }

    @Test
    public void testRejections713() throws Exception {
        testRejections_1_3_0(ModelTestControllerVersion.V7_1_3_FINAL);
    }

    @Test
    public void testRejections720() throws Exception {
        testRejections_1_4_0(ModelTestControllerVersion.V7_2_0_FINAL);
    }

    @Test
    public void testRejections600() throws Exception {
        testRejections_1_3_0(ModelTestControllerVersion.EAP_6_0_0);
    }

   @Test
    public void testRejections601() throws Exception {
        testRejections_1_3_0(ModelTestControllerVersion.EAP_6_0_1);
    }

    @Test
    public void testRejections610() throws Exception {
        testRejections_1_4_1(ModelTestControllerVersion.EAP_6_1_0);
    }

    @Test
    public void testRejections611() throws Exception {
        testRejections_1_4_1(ModelTestControllerVersion.EAP_6_1_1);
    }

    /*
     * Check expected rejections in transformation from current model to 1.3.0 model version.
     * In this case, we expect that:
     * - expressions used in specific attributes will be rejected
     * - elements backups and backup-for will be rejected as children of the cache element
     */
    public void testRejections_1_3_0(ModelTestControllerVersion controllerVersion) throws Exception {

        ModelVersion version_1_3_0 = ModelVersion.create(1, 3, 0);

        // create builder for current subsystem version
        KernelServicesBuilder builderA = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT);

        // create builder for legacy subsystem version
        builderA.createLegacyKernelServicesBuilder(null, controllerVersion, version_1_3_0)
            .addMavenResourceURL("org.jboss.as:jboss-as-clustering-infinispan:" + controllerVersion.getMavenGavVersion())
            .addMavenResourceURL("org.infinispan:infinispan-core:5.2.6.Final")
            //TODO storing the model triggers the weirdness mentioned in SubsystemTestDelegate.LegacyKernelServiceInitializerImpl.install()
            //which is strange since it should be loading it all from the current jboss modules
            //Also this works in several other tests
            .dontPersistXml();

        KernelServices mainServicesA = builderA.build();
        KernelServices legacyServicesA = mainServicesA.getLegacyServices(version_1_3_0);
        Assert.assertNotNull(legacyServicesA);
        Assert.assertTrue("main services did not boot", mainServicesA.isSuccessfulBoot());
        Assert.assertTrue(legacyServicesA.isSuccessfulBoot());

        // test failed operations involving expressions
        List<ModelNode> xmlOps_expressions = builderA.parseXmlResource("infinispan-transformer-2_0-expressions.xml");
        ModelTestUtils.checkFailedTransformedBootOperations(mainServicesA, version_1_3_0, xmlOps_expressions, getFailedOperationConfig130());
        mainServicesA.shutdown();

        // create builder for current subsystem version
        KernelServicesBuilder builderB = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT);

        // create builder for legacy subsystem version
        builderB.createLegacyKernelServicesBuilder(null, controllerVersion, version_1_3_0)
            .addMavenResourceURL("org.jboss.as:jboss-as-clustering-infinispan:" + controllerVersion.getMavenGavVersion())
            .addMavenResourceURL("org.infinispan:infinispan-core:5.2.6.Final")
            //TODO storing the model triggers the weirdness mentioned in SubsystemTestDelegate.LegacyKernelServiceInitializerImpl.install()
            //which is strange since it should be loading it all from the current jboss modules
            //Also this works in several other tests
            .dontPersistXml();

        KernelServices mainServicesB = builderB.build();
        KernelServices legacyServicesB = mainServicesB.getLegacyServices(version_1_3_0);
        Assert.assertNotNull(legacyServicesB);
        Assert.assertTrue("main services did not boot", mainServicesB.isSuccessfulBoot());
        Assert.assertTrue(legacyServicesB.isSuccessfulBoot());

        // test failed operations involving expressions
        List<ModelNode> xmlOps_backup = builderB.parseXmlResource("infinispan-transformer-2_0-backup.xml");
        ModelTestUtils.checkFailedTransformedBootOperations(mainServicesB, version_1_3_0, xmlOps_backup, getFailedOperationConfig130());
        mainServicesB.shutdown();

    }

    /*
     * Check expected rejections in transformation from current model to 1.4.0 model version.
     * In this case, we expect that:
     * - elements backups and backup-for will be rejected as children of the cache element
     *
     */
    public void testRejections_1_4_0(ModelTestControllerVersion controllerVersion) throws Exception {
        // create builder for current subsystem version
        KernelServicesBuilder builder = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT);

        // create builder for legacy subsystem version
        ModelVersion version_1_4_0 = ModelVersion.create(1, 4, 0);
        builder.createLegacyKernelServicesBuilder(null, controllerVersion, version_1_4_0)
            .addMavenResourceURL("org.jboss.as:jboss-as-clustering-infinispan:7.2.0.Final")
            .addMavenResourceURL("org.infinispan:infinispan-core:5.3.0.Final")
            .addMavenResourceURL("org.infinispan:infinispan-cachestore-jdbc:5.3.0.Final")
            //TODO storing the model triggers the weirdness mentioned in SubsystemTestDelegate.LegacyKernelServiceInitializerImpl.install()
            //which is strange since it should be loading it all from the current jboss modules
            //Also this works in several other tests
            .dontPersistXml();

        KernelServices mainServices = builder.build();
        KernelServices legacyServices = mainServices.getLegacyServices(version_1_4_0);
        Assert.assertNotNull(legacyServices);
        Assert.assertTrue("main services did not boot", mainServices.isSuccessfulBoot());
        Assert.assertTrue(legacyServices.isSuccessfulBoot());

        // test failed operations involving backups
        List<ModelNode> xmlOps = builder.parseXmlResource("infinispan-transformer-2_0-backup.xml");
        ModelTestUtils.checkFailedTransformedBootOperations(mainServices, version_1_4_0, xmlOps, getFailedOperationConfig140());
    }

    /*
     * Check expected rejections in transformation from current model to 1.4.0 model version.
     * In this case, we expect that:
     * - elements backups and backup-for will be rejected as children of the cache element
     *
     */
    public void testRejections_1_4_1(ModelTestControllerVersion controllerVersion) throws Exception {
        // create builder for current subsystem version
        KernelServicesBuilder builder = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT);

        // create builder for legacy subsystem version
        ModelVersion version_1_4_1 = ModelVersion.create(1, 4, 1);
        builder.createLegacyKernelServicesBuilder(null, controllerVersion, version_1_4_1)
            .addMavenResourceURL("org.jboss.as:jboss-as-clustering-infinispan:7.2.0.Final")
            .addMavenResourceURL("org.infinispan:infinispan-core:5.3.0.Final")
            .addMavenResourceURL("org.infinispan:infinispan-cachestore-jdbc:5.3.0.Final")
            //TODO storing the model triggers the weirdness mentioned in SubsystemTestDelegate.LegacyKernelServiceInitializerImpl.install()
            //which is strange since it should be loading it all from the current jboss modules
            //Also this works in several other tests
            .dontPersistXml();

        KernelServices mainServices = builder.build();
        KernelServices legacyServices = mainServices.getLegacyServices(version_1_4_1);
        Assert.assertNotNull(legacyServices);
        Assert.assertTrue("main services did not boot", mainServices.isSuccessfulBoot());
        Assert.assertTrue(legacyServices.isSuccessfulBoot());

        // test failed operations involving backups
        List<ModelNode> xmlOps = builder.parseXmlResource("infinispan-transformer-2_0-backup.xml");
        ModelTestUtils.checkFailedTransformedBootOperations(mainServices, version_1_4_1, xmlOps, getFailedOperationConfig140());
    }


    /**
     * Constructs a FailedOperationTransformationConfig which describes:
     * - all attributes which should accept expressions in current but not accept expressions in 1.3.0
     * - the cache child elements backups and backup-for
     *
     * @return config
     */
    private FailedOperationTransformationConfig getFailedOperationConfig130() {

        PathAddress subsystemAddress = PathAddress.pathAddress(PathElement.pathElement(SUBSYSTEM, getMainSubsystemName()));
        FailedOperationTransformationConfig config = new FailedOperationTransformationConfig() ;

        PathAddress containerAddress = subsystemAddress.append(CacheContainerResourceDefinition.CONTAINER_PATH);
        config.addFailedAttribute(containerAddress, new RejectExpressionsConfig(InfinispanRejectedExpressions_1_3.ACCEPT14_REJECT13_CONTAINER_ATTRIBUTES));

        PathAddress transportAddress = containerAddress.append(TransportResourceDefinition.TRANSPORT_PATH);
        config.addFailedAttribute(transportAddress, new RejectExpressionsConfig(InfinispanRejectedExpressions_1_3.ACCEPT14_REJECT13_TRANSPORT_ATTRIBUTES));

        PathElement[] cachePaths = {
                LocalCacheResourceDefinition.LOCAL_CACHE_PATH,
                InvalidationCacheResourceDefinition.INVALIDATION_CACHE_PATH,
                ReplicatedCacheResourceDefinition.REPLICATED_CACHE_PATH,
                DistributedCacheResourceDefinition.DISTRIBUTED_CACHE_PATH
        };

        PathElement[] childPaths = {
                LockingResourceDefinition.LOCKING_PATH,
                TransactionResourceDefinition.TRANSACTION_PATH,
                ExpirationResourceDefinition.EXPIRATION_PATH,
                EvictionResourceDefinition.EVICTION_PATH,
                StateTransferResourceDefinition.STATE_TRANSFER_PATH
        } ;

        PathElement[] storePaths = {
                CustomStoreResourceDefinition.STORE_PATH,
                FileStoreResourceDefinition.FILE_STORE_PATH,
                StringKeyedJDBCStoreResourceDefinition.STRING_KEYED_JDBC_STORE_PATH,
                BinaryKeyedJDBCStoreResourceDefinition.BINARY_KEYED_JDBC_STORE_PATH,
                MixedKeyedJDBCStoreResourceDefinition.MIXED_KEYED_JDBC_STORE_PATH,
                RemoteStoreResourceDefinition.REMOTE_STORE_PATH
        } ;

        // cache attributes
        for (PathElement cachePath: cachePaths) {
            PathAddress cacheAddress = containerAddress.append(cachePath);
            FailedOperationTransformationConfig.ChainedConfig.Builder builder = ChainedConfig.createBuilder(InfinispanRejectedExpressions_1_3.ACCEPT14_REJECT13_CACHE_ATTRIBUTES);
            builder.addConfig(new RejectExpressionsConfig(InfinispanRejectedExpressions_1_3.ACCEPT14_REJECT13_CACHE_ATTRIBUTES));
            builder.addConfig(new RemoveResolvedIndexingPropertiesConfig(CacheResourceDefinition.INDEXING_PROPERTIES));
            config.addFailedAttribute(cacheAddress, builder.build());

            // cache child attributes
            for (PathElement childPath: childPaths) {
                // reject expressions on operations in children
                config.addFailedAttribute(cacheAddress.append(childPath), new RejectExpressionsConfig(InfinispanRejectedExpressions_1_3.ACCEPT14_REJECT13_CHILD_ATTRIBUTES));
            }

            RejectExpressionsConfig keyedTableComplexChildConfig = new RejectExpressionsConfig(JDBCStoreResourceDefinition.COMMON_JDBC_STORE_TABLE_ATTRIBUTES);

            // cache store attributes
            for (PathElement storePath: storePaths) {
                PathAddress storeAddress = cacheAddress.append(storePath);
                // reject expressions on operations on stores and store properties
                config.addFailedAttribute(storeAddress, new RejectExpressionsConfig(InfinispanRejectedExpressions_1_3.ACCEPT14_REJECT13_STORE_ATTRIBUTES)
                        .configureComplexAttribute(ModelKeys.STRING_KEYED_TABLE, keyedTableComplexChildConfig)
                        .configureComplexAttribute(ModelKeys.BINARY_KEYED_TABLE, keyedTableComplexChildConfig)
                );

                PathAddress writeBehindAddress = storeAddress.append(StoreWriteBehindResourceDefinition.STORE_WRITE_BEHIND_PATH);
                config.addFailedAttribute(writeBehindAddress, new RejectExpressionsConfig(InfinispanRejectedExpressions_1_3.ACCEPT14_REJECT13_STORE_ATTRIBUTES));

                PathAddress storePropertyAddress = storeAddress.append(StorePropertyResourceDefinition.STORE_PROPERTY_PATH);
                config.addFailedAttribute(storePropertyAddress, new RejectExpressionsConfig(InfinispanRejectedExpressions_1_3.ACCEPT14_REJECT13_STORE_ATTRIBUTES));
            }
        }

        // x-site related resources where we expect failure

        // replicated cache case
        PathAddress replicatedCacheAddress = containerAddress.append(ReplicatedCacheResourceDefinition.REPLICATED_CACHE_PATH);
        config.addFailedAttribute(replicatedCacheAddress.append("backup"),FailedOperationTransformationConfig.REJECTED_RESOURCE);
        config.addFailedAttribute(replicatedCacheAddress.append("backup-for", "BACKUP_FOR"),FailedOperationTransformationConfig.REJECTED_RESOURCE);

        // distributed cache case
        PathAddress distributedCacheAddress = containerAddress.append(DistributedCacheResourceDefinition.DISTRIBUTED_CACHE_PATH);
        config.addFailedAttribute(distributedCacheAddress.append("backup"),FailedOperationTransformationConfig.REJECTED_RESOURCE);
        config.addFailedAttribute(distributedCacheAddress.append("backup-for", "BACKUP_FOR"),FailedOperationTransformationConfig.REJECTED_RESOURCE);

        return config ;
    }

    /**
     * Constructs a FailedOperationTransformationConfig which describes:
     * - the cache child elements backups and backup-for
     *
     * @return config
     */
    private FailedOperationTransformationConfig getFailedOperationConfig140() {

        PathAddress subsystemAddress = PathAddress.pathAddress(PathElement.pathElement(SUBSYSTEM, getMainSubsystemName()));
        FailedOperationTransformationConfig config = new FailedOperationTransformationConfig() ;

        PathAddress containerAddress = subsystemAddress.append(CacheContainerResourceDefinition.CONTAINER_PATH);
        // replicated cache case
        PathAddress replicatedCacheAddress = containerAddress.append(ReplicatedCacheResourceDefinition.REPLICATED_CACHE_PATH);
        config.addFailedAttribute(replicatedCacheAddress.append("backup"),FailedOperationTransformationConfig.REJECTED_RESOURCE);
        config.addFailedAttribute(replicatedCacheAddress.append("backup-for", "BACKUP_FOR"),FailedOperationTransformationConfig.REJECTED_RESOURCE);

        // distributed cache case
        PathAddress distributedCacheAddress = containerAddress.append(DistributedCacheResourceDefinition.DISTRIBUTED_CACHE_PATH);
        config.addFailedAttribute(distributedCacheAddress.append("backup"),FailedOperationTransformationConfig.REJECTED_RESOURCE);
        config.addFailedAttribute(distributedCacheAddress.append("backup-for", "BACKUP_FOR"),FailedOperationTransformationConfig.REJECTED_RESOURCE);

        return config ;
    }

    /**
     * Constructs a FailedOperationTransformationConfig which describes:
     * - the cache child elements backups and backup-for
     *
     * @return config
     */
    private FailedOperationTransformationConfig getFailedOperationConfig141() {

        PathAddress subsystemAddress = PathAddress.pathAddress(PathElement.pathElement(SUBSYSTEM, getMainSubsystemName()));
        FailedOperationTransformationConfig config = new FailedOperationTransformationConfig() ;

        PathAddress containerAddress = subsystemAddress.append(CacheContainerResourceDefinition.CONTAINER_PATH);
        // replicated cache case
        PathAddress replicatedCacheAddress = containerAddress.append(ReplicatedCacheResourceDefinition.REPLICATED_CACHE_PATH);
        config.addFailedAttribute(replicatedCacheAddress.append("backup"),FailedOperationTransformationConfig.REJECTED_RESOURCE);
        config.addFailedAttribute(replicatedCacheAddress.append("backup-for", "BACKUP_FOR"),FailedOperationTransformationConfig.REJECTED_RESOURCE);

        // distributed cache case
        PathAddress distributedCacheAddress = containerAddress.append(DistributedCacheResourceDefinition.DISTRIBUTED_CACHE_PATH);
        config.addFailedAttribute(distributedCacheAddress.append("backup"),FailedOperationTransformationConfig.REJECTED_RESOURCE);
        config.addFailedAttribute(distributedCacheAddress.append("backup-for", "BACKUP_FOR"),FailedOperationTransformationConfig.REJECTED_RESOURCE);

        return config ;
    }

    private static class RemoveResolvedIndexingPropertiesConfig extends FailedOperationTransformationConfig.AttributesPathAddressConfig<RemoveResolvedIndexingPropertiesConfig>{

        protected RemoveResolvedIndexingPropertiesConfig(AttributeDefinition...attributes) {
            super(convert(attributes));
        }
        @Override
        protected boolean isAttributeWritable(String attributeName) {
            return true;
        }

        @Override
        protected boolean checkValue(String attrName, ModelNode attribute, boolean isWriteAttribute) {
            //The add does not currently reject the defined indexing-properties
            if (attribute.isDefined() && attrName.equals(CacheResourceDefinition.INDEXING_PROPERTIES.getName())) {
                ModelNode resolved = attribute.resolve();
                return resolved.equals(attribute);
            }
            return false;
        }

        @Override
        protected ModelNode correctValue(ModelNode toResolve, boolean isWriteAttribute) {
            return new ModelNode();
        }
    }

    /*
     * Returns a copy of the model generated by booting current controller with legacy operations, but
     * with the following changes:
     * - all instances of "statistics" attribute set to true.
     *
     * This is required when comparing current model with the current controller booted with legacy operations,
     * as the statistics attribute of cache-container and cache add operations are discarded.
     */
    private static class FixReverseControllerModel130 implements ModelFixer {
        @Override
        public ModelNode fixModel(ModelNode modelNode) {
            // ModelNode fixedModelNode = modelNode.clone();
            ModelNode fixedModelNode = modelNode;
            // set statistics to true for cache container and caches in the model
            // we are assuming the model specified in infinispan-transformer-2_0.xml
            fixedModelNode.get(CACHE_CONTAINER, "minimal").get(STATISTICS).set(true);
            fixedModelNode.get(CACHE_CONTAINER, "minimal", LOCAL_CACHE, "local").get(STATISTICS).set(true);

            fixedModelNode.get(CACHE_CONTAINER, "maximal").get(STATISTICS).set(true);
            fixedModelNode.get(CACHE_CONTAINER, "maximal", LOCAL_CACHE, "local").get(STATISTICS).set(true);
            fixedModelNode.get(CACHE_CONTAINER, "maximal", INVALIDATION_CACHE, "invalid").get(STATISTICS).set(true);
            fixedModelNode.get(CACHE_CONTAINER, "maximal", REPLICATED_CACHE, "repl").get(STATISTICS).set(true);
            fixedModelNode.get(CACHE_CONTAINER, "maximal", DISTRIBUTED_CACHE, "dist").get(STATISTICS).set(true);
            fixedModelNode.get(CACHE_CONTAINER, "maximal", DISTRIBUTED_CACHE, "dist").get(STATISTICS).set(true);

            return fixedModelNode;
        }
    }

    /*
     * Returns a copy of the model generated by booting current controller with legacy operations, but
     * with the following changes:
     * - all instances of "statistics" attribute set to true
     * - virtual nodes attribute
     *
     * This is required when comparing current model with the current controller booted with legacy operations,
     * as the statistics attribute of ache-container and cache add operations are discarded.
     */
    private static class FixReverseControllerModel140 implements ModelFixer {
        @Override
        public ModelNode fixModel(ModelNode modelNode) {
            // ModelNode fixedModelNode = modelNode.clone();
            ModelNode fixedModelNode = modelNode;
            // set statistics to true for cache container and caches in the model
            // we are assuming the model specified in infinispan-transformer-2_0.xml
            fixedModelNode.get(CACHE_CONTAINER, "minimal").get(STATISTICS).set(true);
            fixedModelNode.get(CACHE_CONTAINER, "minimal", LOCAL_CACHE, "local").get(STATISTICS).set(true);

            fixedModelNode.get(CACHE_CONTAINER, "maximal").get(STATISTICS).set(true);
            fixedModelNode.get(CACHE_CONTAINER, "maximal", LOCAL_CACHE, "local").get(STATISTICS).set(true);
            fixedModelNode.get(CACHE_CONTAINER, "maximal", INVALIDATION_CACHE, "invalid").get(STATISTICS).set(true);
            fixedModelNode.get(CACHE_CONTAINER, "maximal", REPLICATED_CACHE, "repl").get(STATISTICS).set(true);
            fixedModelNode.get(CACHE_CONTAINER, "maximal", DISTRIBUTED_CACHE, "dist").get(STATISTICS).set(true);

            return fixedModelNode;
        }
    }

    /*
     * Returns a copy of the model generated by booting current controller with legacy operations, but
     * with the following changes:
     * - all instances of "statistics" attribute set to true
     * - virtual nodes attribute
     *
     * This is required when comparing current model with the current controller booted with legacy operations,
     * as the statistics attribute of ache-container and cache add operations are discarded.
     */
    private static class FixReverseControllerModel141 implements ModelFixer {
        @Override
        public ModelNode fixModel(ModelNode modelNode) {
            // ModelNode fixedModelNode = modelNode.clone();
            ModelNode fixedModelNode = modelNode;
            // set statistics to true for cache container and caches in the model
            // we are assuming the model specified in infinispan-transformer-2_0.xml
            fixedModelNode.get(CACHE_CONTAINER, "minimal").get(STATISTICS).set(true);
            fixedModelNode.get(CACHE_CONTAINER, "minimal", LOCAL_CACHE, "local").get(STATISTICS).set(true);

            fixedModelNode.get(CACHE_CONTAINER, "maximal").get(STATISTICS).set(true);
            fixedModelNode.get(CACHE_CONTAINER, "maximal", LOCAL_CACHE, "local").get(STATISTICS).set(true);
            fixedModelNode.get(CACHE_CONTAINER, "maximal", INVALIDATION_CACHE, "invalid").get(STATISTICS).set(true);
            fixedModelNode.get(CACHE_CONTAINER, "maximal", REPLICATED_CACHE, "repl").get(STATISTICS).set(true);
            fixedModelNode.get(CACHE_CONTAINER, "maximal", DISTRIBUTED_CACHE, "dist").get(STATISTICS).set(true);

            return fixedModelNode;
        }
    }

    /*
     * Returns a copy of the model generated by booting legacy controller with legacy operations, but
     * with the following changes:
     * - virtual nodes attribute is removed
     *
     * This is required to address a problem with resource transformers: WFLY-2589
     */
    private static class VirtualNodesTransformedModelProblem implements ModelFixer {
        @Override
        public ModelNode fixModel(ModelNode modelNode) {
            // ModelNode fixedModelNode = modelNode.clone();
            ModelNode fixedModelNode = modelNode;
            // remove the virtual-nodes attribute which was not marked as undefined
            fixedModelNode.get(CACHE_CONTAINER, "maximal", DISTRIBUTED_CACHE, "dist").remove(VIRTUAL_NODES);

            return fixedModelNode;
        }
    }
}
