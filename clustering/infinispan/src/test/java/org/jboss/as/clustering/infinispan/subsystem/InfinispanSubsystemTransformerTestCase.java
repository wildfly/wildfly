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
import org.jboss.as.model.test.FailedOperationTransformationConfig.*;
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
 * - the basic subsystem model transformation to check resource and operation transformer
 * integrity on the 1.4 model alone
 * - rejection of expressions between the 1.4 model (which does accept expressions for attributes) and
 * the 1.3 model (which does not accept expressions for all but three attributes)
 *
 * NOTE: some code for adding Byteman testing of transformed values has been added  and commented out
 * due to current issues with Byteman. These will be reinstated when the issues are resolved.
 *
 * @author <a href="tomaz.cerar@redhat.com">Tomaz Cerar</a>
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */

//@RunWith(BMUnitRunner.class)
public class InfinispanSubsystemTransformerTestCase extends OperationTestCaseBase {

    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("infinispan-transformer_2_0.xml");
    }

    @Test
    public void testTransformer600() throws Exception {
        ignoreThisTestIfEAPRepositoryIsNotReachable();
        testTransformer_1_3_0(ModelTestControllerVersion.EAP_6_0_0);
    }

    @Test
    public void testTransformer601() throws Exception {
        ignoreThisTestIfEAPRepositoryIsNotReachable();
        testTransformer_1_3_0(ModelTestControllerVersion.EAP_6_0_1);
    }


    private void testTransformer_1_3_0(ModelTestControllerVersion controllerVersion) throws Exception {
        ModelVersion version = ModelVersion.create(1, 3);
        KernelServicesBuilder builder = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT)
                .setSubsystemXml(getSubsystemXml());
        builder.createLegacyKernelServicesBuilder(null, controllerVersion, version)
            .addMavenResourceURL("org.jboss.as:jboss-as-clustering-infinispan:" + controllerVersion.getMavenGavVersion())
            //TODO storing the model triggers the weirdness mentioned in SubsystemTestDelegate.LegacyKernelServiceInitializerImpl.install()
            //which is strange since it should be loading it all from the current jboss modules
            //Also this works in several other tests
            .dontPersistXml();

        KernelServices mainServices = builder.build();
        Assert.assertTrue("main services did not boot", mainServices.isSuccessfulBoot());
        Assert.assertTrue(mainServices.getLegacyServices(version).isSuccessfulBoot());

        checkSubsystemModelTransformation(mainServices, version);

        // check that segments is translated into virtual nodes and both segments and indexing properties are removed
        ModelNode model = mainServices.readTransformedModel(version);
        ModelNode distCache = model.get(SUBSYSTEM,"infinispan",ModelKeys.CACHE_CONTAINER, "maximal", ModelKeys.DISTRIBUTED_CACHE, "dist") ;
        Assert.assertFalse(distCache.has(ModelKeys.INDEXING_PROPERTIES));
        Assert.assertFalse(distCache.has(ModelKeys.SEGMENTS));
        Assert.assertTrue(distCache.get(ModelKeys.VIRTUAL_NODES).isDefined());
    }

    @Test
    public void testRejectExpressions600() throws Exception {
        ignoreThisTestIfEAPRepositoryIsNotReachable();
        testRejectExpressions_1_3_0(ModelTestControllerVersion.EAP_6_0_0);
    }

    @Test
    public void testRejectExpressions601() throws Exception {
        ignoreThisTestIfEAPRepositoryIsNotReachable();
        testRejectExpressions_1_3_0(ModelTestControllerVersion.EAP_6_0_1);
    }

    public void testRejectExpressions_1_3_0(ModelTestControllerVersion controllerVersion) throws Exception {
        // create builder for current subsystem version
        KernelServicesBuilder builder = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT);

        // create builder for legacy subsystem version
        ModelVersion version_1_3_0 = ModelVersion.create(1, 3, 0);
        builder.createLegacyKernelServicesBuilder(null, controllerVersion, version_1_3_0)
            .addMavenResourceURL("org.jboss.as:jboss-as-clustering-infinispan:" + controllerVersion.getMavenGavVersion())
            //TODO storing the model triggers the weirdness mentioned in SubsystemTestDelegate.LegacyKernelServiceInitializerImpl.install()
            //which is strange since it should be loading it all from the current jboss modules
            //Also this works in several other tests
            .dontPersistXml();

        KernelServices mainServices = builder.build();
        KernelServices legacyServices = mainServices.getLegacyServices(version_1_3_0);
        Assert.assertNotNull(legacyServices);
        Assert.assertTrue("main services did not boot", mainServices.isSuccessfulBoot()); ;
        Assert.assertTrue(legacyServices.isSuccessfulBoot());

        List<ModelNode> xmlOps = builder.parseXmlResource("infinispan-transformer_1_4-expressions.xml");

        ModelTestUtils.checkFailedTransformedBootOperations(mainServices, version_1_3_0, xmlOps, getConfig());
    }

    @Test
    public void testTransformer_1_4_0() throws Exception {
        ModelVersion version140 = ModelVersion.create(1, 4);
                // create builder for current subsystem version
                KernelServicesBuilder builder = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT);
                builder.createLegacyKernelServicesBuilder(null, ModelTestControllerVersion.MASTER, version140)
                        .addMavenResourceURL("org.jboss.as:jboss-as-clustering-infinispan:7.2.0.Final");


        KernelServices mainServices = builder.build();
        KernelServices legacyServices = mainServices.getLegacyServices(version140);
        Assert.assertNotNull(legacyServices);
        Assert.assertTrue("main services did not boot", mainServices.isSuccessfulBoot());
        Assert.assertTrue(legacyServices.isSuccessfulBoot());

        PathAddress pa = PathAddress.pathAddress(PathElement.pathElement(SUBSYSTEM, InfinispanExtension.SUBSYSTEM_NAME),
                PathElement.pathElement(CacheContainerResourceDefinition.CONTAINER_PATH.getKey(), "container"),
                PathElement.pathElement(DistributedCacheResourceDefinition.DISTRIBUTED_CACHE_PATH.getKey(), "cache"));
        ModelNode addOp = Util.createAddOperation(pa);
        addOp.get(DistributedCacheResourceDefinition.VIRTUAL_NODES.getName()).set(1);
        addOp.get(CacheResourceDefinition.STATISTICS.getName()).set(true);

        OperationTransformer.TransformedOperation transformedOperation = mainServices.transformOperation(version140, addOp);
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

        transformedOperation = mainServices.transformOperation(version140, writeOp);
        Assert.assertEquals(DistributedCacheResourceDefinition.SEGMENTS.getName(), transformedOperation.getTransformedOperation().get(NAME).asString());
        Assert.assertEquals(6, transformedOperation.getTransformedOperation().get(VALUE).asInt());
        Assert.assertFalse(transformedOperation.rejectOperation(result));
        Assert.assertEquals(result, transformedOperation.transformResult(result));
    }

    @Test
    public void testTransformer610() throws Exception {
        testTransformer_1_4_1(ModelTestControllerVersion.EAP_6_1_0);
    }

    @Test
    public void testTransformer611() throws Exception {
        testTransformer_1_4_1(ModelTestControllerVersion.EAP_6_1_1);
    }

    private void testTransformer_1_4_1(ModelTestControllerVersion controllerVersion) throws Exception {
        ModelVersion version = ModelVersion.create(1, 4, 1);
        KernelServicesBuilder builder = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT)
                .setSubsystemXml(getSubsystemXml());
        builder.createLegacyKernelServicesBuilder(null, controllerVersion, version)
            .addMavenResourceURL("org.jboss.as:jboss-as-clustering-infinispan:" + controllerVersion.getMavenGavVersion())
            //TODO storing the model triggers the weirdness mentioned in SubsystemTestDelegate.LegacyKernelServiceInitializerImpl.install()
            //which is strange since it should be loading it all from the current jboss modules
            //Also this works in several other tests
            .dontPersistXml();

        KernelServices mainServices = builder.build();
        Assert.assertTrue("main services did not boot", mainServices.isSuccessfulBoot());
        Assert.assertTrue(mainServices.getLegacyServices(version).isSuccessfulBoot());

        checkSubsystemModelTransformation(mainServices, version);
    }


    /**
     * Constructs a FailedOperationTransformationConfig which describes all attributes which should accept expressions
     * in 1.4.0 but not accept expressions in 1.3.0
     *
     * @return config
     */
    private FailedOperationTransformationConfig getConfig() {

        PathAddress subsystemAddress = PathAddress.pathAddress(PathElement.pathElement(SUBSYSTEM, getMainSubsystemName()));
        FailedOperationTransformationConfig config = new FailedOperationTransformationConfig() ;

        PathAddress containerAddress = subsystemAddress.append(CacheContainerResourceDefinition.CONTAINER_PATH);

        config.addFailedAttribute(containerAddress, new RejectExpressionsConfig(InfinispanRejectedExpressions_1_3.ACCEPT14_REJECT13_CONTAINER_ATTRIBUTES));

        PathAddress transportAddress = containerAddress.append(TransportResourceDefinition.TRANSPORT_PATH);
        // cache container-transport attributes
        config.addFailedAttribute(transportAddress, new RejectExpressionsConfig(InfinispanRejectedExpressions_1_3.ACCEPT14_REJECT13_TRANSPORT_ATTRIBUTES));

        PathElement[] cachePaths = {
                LocalCacheResourceDefinition.LOCAL_CACHE_PATH,
                InvalidationCacheResourceDefinition.INVALIDATION_CACHE_PATH,
                ReplicatedCacheResourceDefinition.REPLICATED_CACHE_PATH,
                DistributedCacheResourceDefinition.DISTRIBUTED_CACHE_PATH
        };

        PathElement[] childPaths = {
                LockingResource.LOCKING_PATH,
                TransactionResourceDefinition.TRANSACTION_PATH,
                ExpirationResourceDefinition.EXPIRATION_PATH,
                EvictionResourceDefinition.EVICTION_PATH,
                StateTransferResourceDefinition.STATE_TRANSFER_PATH
        } ;

        PathElement[] storePaths = {
                StoreResourceDefinition.STORE_PATH,
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

            RejectExpressionsConfig keyedTableComplexChildConfig = new RejectExpressionsConfig(BaseJDBCStoreResourceDefinition.COMMON_JDBC_STORE_TABLE_ATTRIBUTES);

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
}
