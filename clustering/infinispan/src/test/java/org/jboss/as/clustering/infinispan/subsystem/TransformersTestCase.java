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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;

import java.io.IOException;
import java.util.List;

import org.jboss.as.clustering.controller.OperationFactory;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.registry.Resource;
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

public class TransformersTestCase extends OperationTestCaseBase {

    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("infinispan-transformer.xml");
    }

    @Test
    public void testTransformer712() throws Exception {
        testTransformer_1_3_0(
                ModelTestControllerVersion.V7_1_2_FINAL,
                "org.jboss.as:jboss-as-clustering-infinispan:" + ModelTestControllerVersion.V7_1_2_FINAL.getMavenGavVersion(),
                "org.infinispan:infinispan-core:5.2.6.Final"
        );
    }

    @Test
    public void testTransformer713() throws Exception {
        testTransformer_1_3_0(
                ModelTestControllerVersion.V7_1_3_FINAL,
                "org.jboss.as:jboss-as-clustering-infinispan:" + ModelTestControllerVersion.V7_1_3_FINAL.getMavenGavVersion(),
                "org.infinispan:infinispan-core:5.2.6.Final"
        );
    }

    @Test
    public void testTransformer720() throws Exception {
        testTransformer_1_4_0(
                ModelTestControllerVersion.V7_2_0_FINAL,
                "org.jboss.as:jboss-as-clustering-infinispan:" + ModelTestControllerVersion.V7_2_0_FINAL.getMavenGavVersion(),
                "org.infinispan:infinispan-core:5.3.0.Final",
                "org.infinispan:infinispan-cachestore-jdbc:5.3.0.Final"
        );
    }

    @Test
    public void testTransformer800() throws Exception {
        testTransformer_2_0_0(
                ModelTestControllerVersion.WILDFLY_8_0_0_FINAL,
                "org.wildfly:wildfly-clustering-infinispan:" + ModelTestControllerVersion.WILDFLY_8_0_0_FINAL.getMavenGavVersion(),
                "org.infinispan:infinispan-core:6.0.1.Final",
                "org.infinispan:infinispan-commons:6.0.1.Final",
                "org.infinispan:infinispan-cachestore-jdbc:6.0.1.Final"
        );
    }

    @Test
    public void testTransformer600() throws Exception {
        testTransformer_1_3_0(
                ModelTestControllerVersion.EAP_6_0_0,
                "org.jboss.as:jboss-as-clustering-infinispan:" + ModelTestControllerVersion.EAP_6_0_0.getMavenGavVersion(),
                "org.infinispan:infinispan-core:5.1.4.FINAL-redhat-1"
        );
    }

    @Test
    public void testTransformer601() throws Exception {
        testTransformer_1_3_0(ModelTestControllerVersion.EAP_6_0_1,
                "org.jboss.as:jboss-as-clustering-infinispan:" + ModelTestControllerVersion.EAP_6_0_1.getMavenGavVersion(),
                "org.infinispan:infinispan-core:5.1.8.Final-redhat-1"
                );
    }

    @Test
    public void testTransformer610() throws Exception {
        testTransformer_1_4_1(
                ModelTestControllerVersion.EAP_6_1_0,
                "org.jboss.as:jboss-as-clustering-infinispan:" + ModelTestControllerVersion.EAP_6_1_0.getMavenGavVersion(),
                "org.infinispan:infinispan-core:5.2.6.Final-redhat-1",
                "org.infinispan:infinispan-cachestore-jdbc:5.2.6.Final-redhat-1"
                );
    }

    @Test
    public void testTransformer611() throws Exception {
        testTransformer_1_4_1(
                ModelTestControllerVersion.EAP_6_1_1,
                "org.jboss.as:jboss-as-clustering-infinispan:" + ModelTestControllerVersion.EAP_6_1_1.getMavenGavVersion(),
                "org.infinispan:infinispan-core:5.2.7.Final-redhat-1",
                "org.infinispan:infinispan-cachestore-jdbc:5.2.7.Final-redhat-1"
                );
    }

    private KernelServices buildKernelServices(ModelTestControllerVersion controllerVersion, ModelVersion version, String... mavenResourceURLs) throws Exception {
        return this.buildKernelServices(this.getSubsystemXml(), controllerVersion, version, mavenResourceURLs);
    }

    private KernelServices buildKernelServices(String xml, ModelTestControllerVersion controllerVersion, ModelVersion version, String... mavenResourceURLs) throws Exception {
        KernelServicesBuilder builder = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT)
                .setSubsystemXml(xml);

        builder.createLegacyKernelServicesBuilder(null, controllerVersion, version)
                .addMavenResourceURL(mavenResourceURLs)
                .skipReverseControllerCheck()
                .dontPersistXml();

        KernelServices services = builder.build();
        Assert.assertTrue(ModelTestControllerVersion.MASTER + " boot failed", services.isSuccessfulBoot());
        Assert.assertTrue(controllerVersion.getMavenGavVersion() + " boot failed", services.getLegacyServices(version).isSuccessfulBoot());
        return services;
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
    private void testTransformer_1_3_0(ModelTestControllerVersion controllerVersion, String ... mavenResourceURLs) throws Exception {

        ModelVersion version = InfinispanModel.VERSION_1_3_0.getVersion();
        KernelServices services = this.buildKernelServices(controllerVersion, version, mavenResourceURLs);

        // check that both versions of the legacy model are the same and valid
        checkSubsystemModelTransformation(services, version);

        // 1.3.0 API specific checks:
        // - check that segments is translated into virtual nodes
        // - check both segments and indexing properties are removed
        ModelNode model = services.readTransformedModel(version);
        ModelNode cache = model.get(InfinispanSubsystemResourceDefinition.PATH.getKeyValuePair()).get(CacheContainerResourceDefinition.pathElement("maximal").getKeyValuePair()).get(DistributedCacheResourceDefinition.pathElement("dist").getKeyValuePair());
        Assert.assertFalse(cache.has(ModelKeys.INDEXING_PROPERTIES));
        Assert.assertFalse(cache.has(ModelKeys.SEGMENTS));
        Assert.assertTrue(cache.get(ModelKeys.VIRTUAL_NODES).isDefined());
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
    public void testTransformer_1_4_0(ModelTestControllerVersion controllerVersion, String ... mavenResourceURLs) throws Exception {
        ModelVersion version = InfinispanModel.VERSION_1_4_0.getVersion();
        KernelServices services = this.buildKernelServices(controllerVersion, version, mavenResourceURLs);

        // check that both versions of the legacy model are the same and valid
        checkSubsystemModelTransformation(services, version, new VirtualNodesModelFixer());

        // 1.4.0 API specific checks
        // - check transformation of virtual nodes into segments
        // - check that statistics is discarded
        PathAddress address = PathAddress.pathAddress(InfinispanSubsystemResourceDefinition.PATH, CacheContainerResourceDefinition.pathElement("container"), DistributedCacheResourceDefinition.pathElement("cache"));
        ModelNode operation = Util.createAddOperation(address);
        operation.get(DistributedCacheResourceDefinition.VIRTUAL_NODES.getName()).set(4);
        operation.get(CacheResourceDefinition.STATISTICS_ENABLED.getName()).set(true);

        OperationTransformer.TransformedOperation transformedOperation = services.transformOperation(version, operation);
        Assert.assertFalse(transformedOperation.getTransformedOperation().has(DistributedCacheResourceDefinition.VIRTUAL_NODES.getName()));
        Assert.assertEquals(24, transformedOperation.getTransformedOperation().get(DistributedCacheResourceDefinition.SEGMENTS.getName()).asInt());

        ModelNode result = new ModelNode();
        result.get(OUTCOME).set(SUCCESS);
        result.get(RESULT);
        Assert.assertFalse(transformedOperation.rejectOperation(result));
        Assert.assertEquals(result, transformedOperation.transformResult(result));

        operation = OperationFactory.createWriteAttributeOperation(address, DistributedCacheResourceDefinition.VIRTUAL_NODES.getName(), new ModelNode(4));

        transformedOperation = services.transformOperation(version, operation);
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
    public void testTransformer_1_4_1(ModelTestControllerVersion controllerVersion, String ... mavenResourceURLs) throws Exception {
        ModelVersion version = InfinispanModel.VERSION_1_4_1.getVersion();
        KernelServices services = this.buildKernelServices(controllerVersion, version, mavenResourceURLs);

        // check that both versions of the legacy model are the same and valid
        checkSubsystemModelTransformation(services, version);

        // 1.4.1 API specific checks
        // - check transformation of virtual nodes into segments
        // - check that statistics is discarded
        PathAddress pa = PathAddress.pathAddress(PathElement.pathElement(SUBSYSTEM, InfinispanExtension.SUBSYSTEM_NAME),
                CacheContainerResourceDefinition.pathElement("container"),
                DistributedCacheResourceDefinition.pathElement("cache"));
        ModelNode addOp = Util.createAddOperation(pa);
        addOp.get(DistributedCacheResourceDefinition.VIRTUAL_NODES.getName()).set(1);
        addOp.get(CacheResourceDefinition.STATISTICS_ENABLED.getName()).set(true);

        // what happens to virtual nodes now? current - 1.4.1
        /*
        OperationTransformer.TransformedOperation transformedOperation = services.transformOperation(version, operation);
        Assert.assertFalse(transformedOperation.getTransformedOperation().has(DistributedCacheResourceDefinition.VIRTUAL_NODES.getName()));
        Assert.assertEquals(24, transformedOperation.getTransformedOperation().get(DistributedCacheResourceDefinition.SEGMENTS.getName()).asInt());

        ModelNode result = new ModelNode();
        result.get(OUTCOME).set(SUCCESS);
        result.get(RESULT);
        Assert.assertFalse(transformedOperation.rejectOperation(result));
        Assert.assertEquals(result, transformedOperation.transformResult(result));

        operation = OperationFactory.createWriteAttributeOperation(address, DistributedCacheResourceDefinition.VIRTUAL_NODES.getName(), new ModelNode(4));

        transformedOperation = services.transformOperation(version, operation);
        Assert.assertEquals(DistributedCacheResourceDefinition.SEGMENTS.getName(), transformedOperation.getTransformedOperation().get(NAME).asString());
        Assert.assertEquals(24, transformedOperation.getTransformedOperation().get(VALUE).asInt());
        Assert.assertFalse(transformedOperation.rejectOperation(result));
        Assert.assertEquals(result, transformedOperation.transformResult(result));
        */
    }

    /*
     * Check transformation from current model version to 2.0.0 model version.
     *
     * We do this by checking that;
     * - both the current kernel services and legacy kernel services boot correctly
     * - both versions of the transformed model (resource transformation vs operation transformation)
     * are the same and are valid according to the legacy subsystem description
     * - any specific requirements for discarding/renaming/converting have been performed
     */
    public void testTransformer_2_0_0(ModelTestControllerVersion controllerVersion, String ... mavenResourceURLs) throws Exception {
        ModelVersion version = InfinispanModel.VERSION_2_0_0.getVersion();
        KernelServices services = this.buildKernelServices(controllerVersion, version, mavenResourceURLs);

        // check that both versions of the legacy model are the same and valid
        checkSubsystemModelTransformation(services, version);

        // Verify that mode=BATCH is translated to mode=NONE, batching=true
        ModelNode model = services.readTransformedModel(version);
        ModelNode cache = model.get(InfinispanSubsystemResourceDefinition.PATH.getKeyValuePair()).get(CacheContainerResourceDefinition.pathElement("maximal").getKeyValuePair()).get(DistributedCacheResourceDefinition.pathElement("dist").getKeyValuePair());
        Assert.assertTrue(cache.hasDefined(CacheResourceDefinition.BATCHING.getName()));
        Assert.assertTrue(cache.get(CacheResourceDefinition.BATCHING.getName()).asBoolean());
        ModelNode transaction = model.get(TransactionResourceDefinition.PATH.getKeyValuePair());
        if (transaction.hasDefined(TransactionResourceDefinition.MODE.getName())) {
            Assert.assertEquals(TransactionMode.NONE.name(), transaction.get(TransactionResourceDefinition.MODE.getName()));
        }
    }

    @Test
    public void testRejections712() throws Exception {
        testRejections_1_3_0(
                ModelTestControllerVersion.V7_1_2_FINAL,
                "org.jboss.as:jboss-as-clustering-infinispan:" + ModelTestControllerVersion.V7_1_2_FINAL.getMavenGavVersion(),
                "org.infinispan:infinispan-core:5.2.6.Final"
                );
    }

    @Test
    public void testRejections713() throws Exception {
        testRejections_1_3_0(
                ModelTestControllerVersion.V7_1_3_FINAL,
                "org.jboss.as:jboss-as-clustering-infinispan:" + ModelTestControllerVersion.V7_1_3_FINAL.getMavenGavVersion(),
                "org.infinispan:infinispan-core:5.2.6.Final"
        );
    }

    @Test
    public void testRejections720() throws Exception {
        testRejections_1_4_0(
                ModelTestControllerVersion.V7_2_0_FINAL,
                "org.jboss.as:jboss-as-clustering-infinispan:" + ModelTestControllerVersion.V7_2_0_FINAL.getMavenGavVersion(),
                "org.infinispan:infinispan-core:5.3.0.Final",
                "org.infinispan:infinispan-cachestore-jdbc:5.3.0.Final"
                );
    }

    @Test
    public void testRejections600() throws Exception {
        testRejections_1_3_0(
                ModelTestControllerVersion.EAP_6_0_0,
                "org.jboss.as:jboss-as-clustering-infinispan:" + ModelTestControllerVersion.EAP_6_0_0.getMavenGavVersion(),
                "org.infinispan:infinispan-core:5.1.4.FINAL-redhat-1"
        );
    }

    @Test
    public void testRejections601() throws Exception {
        testRejections_1_3_0(ModelTestControllerVersion.EAP_6_0_1,
                "org.jboss.as:jboss-as-clustering-infinispan:" + ModelTestControllerVersion.EAP_6_0_1.getMavenGavVersion(),
                "org.infinispan:infinispan-core:5.1.8.Final-redhat-1"
                );
    }

    @Test
    public void testRejections610() throws Exception {
        testRejections_1_4_1(
                ModelTestControllerVersion.EAP_6_1_0,
                "org.jboss.as:jboss-as-clustering-infinispan:" + ModelTestControllerVersion.EAP_6_1_0.getMavenGavVersion(),
                "org.infinispan:infinispan-core:5.2.6.Final-redhat-1",
                "org.infinispan:infinispan-cachestore-jdbc:5.2.6.Final-redhat-1"
                );
    }

    @Test
    public void testRejections611() throws Exception {
        testRejections_1_4_1(
                ModelTestControllerVersion.EAP_6_1_1,
                "org.jboss.as:jboss-as-clustering-infinispan:" + ModelTestControllerVersion.EAP_6_1_1.getMavenGavVersion(),
                "org.infinispan:infinispan-core:5.2.7.Final-redhat-1",
                "org.infinispan:infinispan-cachestore-jdbc:5.2.7.Final-redhat-1"
                );
    }

    /*
     * Check expected rejections in transformation from current model to 1.3.0 model version.
     * In this case, we expect that:
     * - expressions used in specific attributes will be rejected
     * - elements backups and backup-for will be rejected as children of the cache element
     */
    public void testRejections_1_3_0(ModelTestControllerVersion controllerVersion, String ... mavenResourceURLs) throws Exception {
        ModelVersion version = InfinispanModel.VERSION_1_3_0.getVersion();

        // create builder for current subsystem version
        KernelServicesBuilder builderA = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT);

        // initialise the legacy services
        builderA.createLegacyKernelServicesBuilder(null, controllerVersion, version)
                .addMavenResourceURL(mavenResourceURLs)
                //TODO storing the model triggers the weirdness mentioned in SubsystemTestDelegate.LegacyKernelServiceInitializerImpl.install()
                //which is strange since it should be loading it all from the current jboss modules
                //Also this works in several other tests
                .dontPersistXml();

        KernelServices mainServicesA = builderA.build();
        KernelServices legacyServicesA = mainServicesA.getLegacyServices(version);
        Assert.assertNotNull(legacyServicesA);
        Assert.assertTrue("main services did not boot", mainServicesA.isSuccessfulBoot());
        Assert.assertTrue(legacyServicesA.isSuccessfulBoot());

        // test failed operations involving expressions
        List<ModelNode> xmlOps_expressions = builderA.parseXmlResource("infinispan-transformer-expressions.xml");
        ModelTestUtils.checkFailedTransformedBootOperations(mainServicesA, version, xmlOps_expressions, getFailedOperationConfig130());
        mainServicesA.shutdown();

        // create builder for current subsystem version
        KernelServicesBuilder builderB = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT);

        // initialize the legacy services
        builderB.createLegacyKernelServicesBuilder(null, controllerVersion, version)
                .addMavenResourceURL(mavenResourceURLs)
                //TODO storing the model triggers the weirdness mentioned in SubsystemTestDelegate.LegacyKernelServiceInitializerImpl.install()
                //which is strange since it should be loading it all from the current jboss modules
                //Also this works in several other tests
                .dontPersistXml();

        KernelServices mainServicesB = builderB.build();
        KernelServices legacyServicesB = mainServicesB.getLegacyServices(version);
        Assert.assertNotNull(legacyServicesB);
        Assert.assertTrue("main services did not boot", mainServicesB.isSuccessfulBoot());
        Assert.assertTrue(legacyServicesB.isSuccessfulBoot());

        // test failed operations involving expressions
        List<ModelNode> xmlOps_backup = builderB.parseXmlResource("infinispan-transformer-backup.xml");
        ModelTestUtils.checkFailedTransformedBootOperations(mainServicesB, version, xmlOps_backup, getFailedOperationConfig130());
        mainServicesB.shutdown();

    }

    /*
     * Check expected rejections in transformation from current model to 1.4.0 model version.
     * In this case, we expect that:
     * - elements backups and backup-for will be rejected as children of the cache element
     *
     */
    public void testRejections_1_4_0(ModelTestControllerVersion controllerVersion, String ... mavenResourceURLs) throws Exception {
        ModelVersion version = InfinispanModel.VERSION_1_4_0.getVersion();

        // create builder for current subsystem version
        KernelServicesBuilder builder = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT);

        // initialize the legacy services
        builder.createLegacyKernelServicesBuilder(null, controllerVersion, version)
                .addMavenResourceURL(mavenResourceURLs)
                //TODO storing the model triggers the weirdness mentioned in SubsystemTestDelegate.LegacyKernelServiceInitializerImpl.install()
                //which is strange since it should be loading it all from the current jboss modules
                //Also this works in several other tests
                .dontPersistXml();

        KernelServices mainServices = builder.build();
        KernelServices legacyServices = mainServices.getLegacyServices(version);
        Assert.assertNotNull(legacyServices);
        Assert.assertTrue("main services did not boot", mainServices.isSuccessfulBoot());
        Assert.assertTrue(legacyServices.isSuccessfulBoot());

        // test failed operations involving backups
        List<ModelNode> xmlOps = builder.parseXmlResource("infinispan-transformer-backup.xml");
        ModelTestUtils.checkFailedTransformedBootOperations(mainServices, version, xmlOps, getFailedOperationConfig140());
    }

    /*
     * Check expected rejections in transformation from current model to 1.4.0 model version.
     * In this case, we expect that:
     * - elements backups and backup-for will be rejected as children of the cache element
     *
     */
    public void testRejections_1_4_1(ModelTestControllerVersion controllerVersion, String ... mavenResourceURLs) throws Exception {
        ModelVersion version = InfinispanModel.VERSION_1_4_1.getVersion();

        // create builder for current subsystem version
        KernelServicesBuilder builder = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT);

        // initialise the legacy services
        builder.createLegacyKernelServicesBuilder(null, controllerVersion, version)
                .addMavenResourceURL(mavenResourceURLs)
                //TODO storing the model triggers the weirdness mentioned in SubsystemTestDelegate.LegacyKernelServiceInitializerImpl.install()
                //which is strange since it should be loading it all from the current jboss modules
                //Also this works in several other tests
                .dontPersistXml();

        KernelServices mainServices = builder.build();
        KernelServices legacyServices = mainServices.getLegacyServices(version);
        Assert.assertNotNull(legacyServices);
        Assert.assertTrue("main services did not boot", mainServices.isSuccessfulBoot());
        Assert.assertTrue(legacyServices.isSuccessfulBoot());

        // test failed operations involving backups
        List<ModelNode> xmlOps = builder.parseXmlResource("infinispan-transformer-backup.xml");
        ModelTestUtils.checkFailedTransformedBootOperations(mainServices, version, xmlOps, getFailedOperationConfig140());
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

        PathAddress containerAddress = subsystemAddress.append(CacheContainerResourceDefinition.WILDCARD_PATH);
        config.addFailedAttribute(containerAddress, new RejectExpressionsConfig(InfinispanRejectedExpressions_1_3.ACCEPT14_REJECT13_CONTAINER_ATTRIBUTES));

        PathAddress transportAddress = containerAddress.append(TransportResourceDefinition.PATH);
        config.addFailedAttribute(transportAddress, new RejectExpressionsConfig(InfinispanRejectedExpressions_1_3.ACCEPT14_REJECT13_TRANSPORT_ATTRIBUTES));

        PathElement[] cachePaths = {
                LocalCacheResourceDefinition.WILDCARD_PATH,
                InvalidationCacheResourceDefinition.WILDCARD_PATH,
                ReplicatedCacheResourceDefinition.WILDCARD_PATH,
                DistributedCacheResourceDefinition.WILDCARD_PATH
        };

        PathElement[] childPaths = {
                LockingResourceDefinition.PATH,
                TransactionResourceDefinition.PATH,
                ExpirationResourceDefinition.PATH,
                EvictionResourceDefinition.PATH,
                StateTransferResourceDefinition.PATH
        } ;

        PathElement[] storePaths = {
                CustomStoreResourceDefinition.PATH,
                FileStoreResourceDefinition.PATH,
                StringKeyedJDBCStoreResourceDefinition.PATH,
                BinaryKeyedJDBCStoreResourceDefinition.PATH,
                MixedKeyedJDBCStoreResourceDefinition.PATH,
                RemoteStoreResourceDefinition.PATH
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

            RejectExpressionsConfig keyedTableComplexChildConfig = new RejectExpressionsConfig(JDBCStoreResourceDefinition.TABLE_ATTRIBUTES);

            // cache store attributes
            for (PathElement storePath: storePaths) {
                PathAddress storeAddress = cacheAddress.append(storePath);
                // reject expressions on operations on stores and store properties
                config.addFailedAttribute(storeAddress, new RejectExpressionsConfig(InfinispanRejectedExpressions_1_3.ACCEPT14_REJECT13_STORE_ATTRIBUTES)
                        .configureComplexAttribute(ModelKeys.STRING_KEYED_TABLE, keyedTableComplexChildConfig)
                        .configureComplexAttribute(ModelKeys.BINARY_KEYED_TABLE, keyedTableComplexChildConfig)
                );

                PathAddress writeBehindAddress = storeAddress.append(StoreWriteBehindResourceDefinition.PATH);
                config.addFailedAttribute(writeBehindAddress, new RejectExpressionsConfig(InfinispanRejectedExpressions_1_3.ACCEPT14_REJECT13_STORE_ATTRIBUTES));

                PathAddress storePropertyAddress = storeAddress.append(StorePropertyResourceDefinition.WILDCARD_PATH);
                config.addFailedAttribute(storePropertyAddress, new RejectExpressionsConfig(InfinispanRejectedExpressions_1_3.ACCEPT14_REJECT13_STORE_ATTRIBUTES));
            }
        }

        // x-site related resources where we expect failure

        // replicated cache case
        PathAddress replicatedCacheAddress = containerAddress.append(ReplicatedCacheResourceDefinition.WILDCARD_PATH);
        config.addFailedAttribute(replicatedCacheAddress.append("backup"),FailedOperationTransformationConfig.REJECTED_RESOURCE);
        config.addFailedAttribute(replicatedCacheAddress.append("backup-for", "BACKUP_FOR"),FailedOperationTransformationConfig.REJECTED_RESOURCE);

        // distributed cache case
        PathAddress distributedCacheAddress = containerAddress.append(DistributedCacheResourceDefinition.WILDCARD_PATH);
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

        PathAddress containerAddress = subsystemAddress.append(CacheContainerResourceDefinition.WILDCARD_PATH);
        // replicated cache case
        PathAddress replicatedCacheAddress = containerAddress.append(ReplicatedCacheResourceDefinition.WILDCARD_PATH);
        config.addFailedAttribute(replicatedCacheAddress.append("backup"),FailedOperationTransformationConfig.REJECTED_RESOURCE);
        config.addFailedAttribute(replicatedCacheAddress.append("backup-for", "BACKUP_FOR"),FailedOperationTransformationConfig.REJECTED_RESOURCE);

        // distributed cache case
        PathAddress distributedCacheAddress = containerAddress.append(DistributedCacheResourceDefinition.WILDCARD_PATH);
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
     * Returns a copy of the model generated by booting legacy controller with legacy operations, but
     * with the following changes:
     * - virtual nodes attribute is removed
     *
     * This is required to address a problem with resource transformers: WFLY-2589
     */
    static class VirtualNodesModelFixer implements ModelFixer {
        @Override
        public ModelNode fixModel(ModelNode model) {
            ModelNode container = model.get(CacheContainerResourceDefinition.pathElement("maximal").getKeyValuePair());
            ModelNode cache = container.get(DistributedCacheResourceDefinition.pathElement("dist").getKeyValuePair());
            // remove the virtual-nodes attribute which was not marked as undefined
            cache.remove(DistributedCacheResourceDefinition.VIRTUAL_NODES.getName());
//            model.get(CacheContainerResourceDefinition.pathElement("maximal").getKeyValuePair()).get(LocalCacheResourceDefinition.pathElement("local").getKeyValuePair()).get(TransactionResourceDefinition.PATH.getKeyValuePair()).remove(TransactionResourceDefinition.MODE.getName());
            return model;
        }
    }
}
