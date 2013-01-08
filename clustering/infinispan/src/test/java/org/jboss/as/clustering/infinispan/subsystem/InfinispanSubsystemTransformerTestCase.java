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

import static org.jboss.as.clustering.infinispan.subsystem.ModelKeys.DEFAULT_CACHE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.List;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.transform.OperationTransformer;
import org.jboss.as.model.test.FailedOperationTransformationConfig;
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
        return readResource("infinispan-transformer_1_4.xml");
    }

    @Test
//    @BMRule(name="Debugging support",
//            targetClass="^org.jboss.as.subsystem.test.SubsystemTestDelegate",
//            targetMethod="checkSubsystemModelTransformation",
//            targetLocation="AT INVOKE ModelTestUtils.compare",
//            binding="legacy:ModelNode = $1; transformed:ModelNode = $2",
//            condition="TRUE",
//            action="traceln(\"legacy = \" + legacy.toString() + \" transformed = \" + transformed.toString()")
    public void testTransformer_1_3_0() throws Exception {
        ModelVersion version = ModelVersion.create(1, 3);
        KernelServicesBuilder builder = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT)
                .setSubsystemXml(getSubsystemXml());
        builder.createLegacyKernelServicesBuilder(null, version)
            .addMavenResourceURL("org.jboss.as:jboss-as-clustering-infinispan:7.1.2.Final");

        KernelServices mainServices = builder.build();
        Assert.assertTrue("main services did not boot", mainServices.isSuccessfulBoot()); ;

        checkSubsystemModelTransformation(mainServices, version);
    }

    @Test
    public void testRejectExpressions_1_3_0_Kabir() throws Exception {
        // create builder for current subsystem version
        KernelServicesBuilder builder = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT);

        // create builder for legacy subsystem version
        ModelVersion version_1_3_0 = ModelVersion.create(1, 3, 0);
        builder.createLegacyKernelServicesBuilder(null, version_1_3_0)
            .addMavenResourceURL("org.jboss.as:jboss-as-clustering-infinispan:7.1.2.Final");

        KernelServices mainServices = builder.build();
        KernelServices legacyServices = mainServices.getLegacyServices(version_1_3_0);
        Assert.assertNotNull(legacyServices);
        Assert.assertTrue("main services did not boot", mainServices.isSuccessfulBoot()); ;
        Assert.assertTrue(legacyServices.isSuccessfulBoot());

        List<ModelNode> xmlOps = builder.parseXmlResource("infinispan-transformer_1_4-expressions.xml");

        FailedOperationTransformationConfig config = getConfig() ;


        ModelTestUtils.checkFailedTransformedBootOperations(mainServices, version_1_3_0, xmlOps, getConfig());
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

        // cache-container attributes
        config.addFailedAttribute(subsystemAddress.append(CacheContainerResource.CONTAINER_PATH),
                new FailedOperationTransformationConfig.RejectExpressionsConfig(
                        InfinispanRejectedExpressions_1_3.ACCEPT14_REJECT13_CONTAINER_ATTRIBUTES));

        // cache container-transport attributes
        config.addFailedAttribute(subsystemAddress.append(CacheContainerResource.CONTAINER_PATH).append(TransportResource.TRANSPORT_PATH),
                // cluster allowed expressions in 1.3.0
                new FailedOperationTransformationConfig.RejectExpressionsConfig(
                        InfinispanRejectedExpressions_1_3.ACCEPT14_REJECT13_TRANSPORT_ATTRIBUTES)) ;

        PathElement[] cachePaths = {
                LocalCacheResource.LOCAL_CACHE_PATH,
                InvalidationCacheResource.INVALIDATION_CACHE_PATH,
                ReplicatedCacheResource.REPLICATED_CACHE_PATH,
                DistributedCacheResource.DISTRIBUTED_CACHE_PATH};

        // cache attributes
        for (int i=0; i < cachePaths.length; i++) {
            config.addFailedAttribute(subsystemAddress.append(CacheContainerResource.CONTAINER_PATH).append(cachePaths[i]),
                    new FailedOperationTransformationConfig.RejectExpressionsConfig(
                            InfinispanRejectedExpressions_1_3.ACCEPT14_REJECT13_CACHE_ATTRIBUTES));

            PathElement[] childPaths = {
                    LockingResource.LOCKING_PATH,
                    TransactionResource.TRANSACTION_PATH,
                    ExpirationResource.EXPIRATION_PATH,
                    EvictionResource.EVICTION_PATH,
                    StateTransferResource.STATE_TRANSFER_PATH
            } ;

            // cache child attributes
            for (int j=0; j < childPaths.length; j++) {
                // reject expressions on operations in children
                config.addFailedAttribute(subsystemAddress.append(CacheContainerResource.CONTAINER_PATH).append(cachePaths[i]).append(childPaths[j]),
                        new FailedOperationTransformationConfig.RejectExpressionsConfig(
                                InfinispanRejectedExpressions_1_3.ACCEPT14_REJECT13_CHILD_ATTRIBUTES));
            }

            PathElement[] storePaths = {
                    StoreResource.STORE_PATH,
                    FileStoreResource.FILE_STORE_PATH,
                    StringKeyedJDBCStoreResource.STRING_KEYED_JDBC_STORE_PATH,
                    BinaryKeyedJDBCStoreResource.BINARY_KEYED_JDBC_STORE_PATH,
                    MixedKeyedJDBCStoreResource.MIXED_KEYED_JDBC_STORE_PATH,
                    RemoteStoreResource.REMOTE_STORE_PATH
            } ;

            // cache store attributes
            for (int k=0; k < storePaths.length; k++) {
                // reject expressions on operations on stores and store properties
                config.addFailedAttribute(subsystemAddress.append(CacheContainerResource.CONTAINER_PATH).append(cachePaths[i]).append(storePaths[k]),
                        new FailedOperationTransformationConfig.RejectExpressionsConfig(InfinispanRejectedExpressions_1_3.ACCEPT14_REJECT13_STORE_ATTRIBUTES));

                config.addFailedAttribute(subsystemAddress.append(CacheContainerResource.CONTAINER_PATH).append(cachePaths[i]).append(storePaths[k]).append(StoreWriteBehindResource.STORE_WRITE_BEHIND_PATH),
                        new FailedOperationTransformationConfig.RejectExpressionsConfig(InfinispanRejectedExpressions_1_3.ACCEPT14_REJECT13_STORE_ATTRIBUTES));

                config.addFailedAttribute(subsystemAddress.append(CacheContainerResource.CONTAINER_PATH).append(cachePaths[i]).append(storePaths[k]).append(StorePropertyResource.STORE_PROPERTY_PATH),
                        new FailedOperationTransformationConfig.RejectExpressionsConfig(InfinispanRejectedExpressions_1_3.ACCEPT14_REJECT13_STORE_ATTRIBUTES));
            }
        }
        return config ;
    }

    @Test
//    @BMRule(name="Debugging support",
//            targetClass="^org.jboss.as.subsystem.test.SubsystemTestDelegate",
//            targetMethod="checkSubsystemModelTransformation",
//            targetLocation="AT INVOKE ModelTestUtils.compare",
//            binding="legacy:ModelNode = $1; transformed:ModelNode = $2",
//            condition="TRUE",
//            action="traceln(\"legacy = \" + legacy.toString() + \" transformed = \" + transformed.toString()")
    public void testRejectExpressions_1_3_0() throws Exception {
        // create builder for current subsystem version
        KernelServicesBuilder builder = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT)
                .setSubsystemXml(getSubsystemXml());

        // create builder for legacy subsystem version
        ModelVersion version_1_3_0 = ModelVersion.create(1, 3, 0);
        builder.createLegacyKernelServicesBuilder(null, version_1_3_0)
            .addMavenResourceURL("org.jboss.as:jboss-as-clustering-infinispan:7.1.2.Final");

        KernelServices mainServices = builder.build();
        KernelServices legacyServices = mainServices.getLegacyServices(version_1_3_0);
        junit.framework.Assert.assertNotNull(legacyServices);

        // A number of cases to test:
        // 1. cache store property ADD and write operations, to check expressions are rejected for 1.4 attributes
        // 2. DEFAULT_CACHE ADD and write operation, to check that expressions are not rejected for 1.3 attributes which accept expressions
        // 3. cache store table ADD and write operations, to check that expressions are rejected for primitive values in complex attributes

        // 1. cache store property should reject
        ModelNode cacheStorePropertyAddOp = getCacheStorePropertyAddOperation("maximal", "repl", ModelKeys.REPLICATED_CACHE, "some_property", "${some_property_value:new}");
        testRejectExpressionsForOperation(mainServices, version_1_3_0, cacheStorePropertyAddOp, true);

        ModelNode cacheStorePropertyWriteOp = getCacheStorePropertyWriteOperation("maximal", "repl", ModelKeys.REPLICATED_CACHE, "some_property", "${some_property_value:new}");
        testRejectExpressionsForOperation(mainServices, version_1_3_0, cacheStorePropertyWriteOp, true);

        // 2. cache container should accept expressions for DEFAULT_CACHE
        ModelNode cacheContainerAddOp = Util.createAddOperation(getCacheContainerAddress("somecontainer"));
        cacheContainerAddOp.get(DEFAULT_CACHE).set("${some_default_cache:default}");
        testRejectExpressionsForOperation(mainServices, version_1_3_0, cacheContainerAddOp, false);

        ModelNode cacheContainerWriteOp = getCacheContainerWriteOperation("maximal", ModelKeys.DEFAULT_CACHE, "${some_default_cache:local2}");
        testRejectExpressionsForOperation(mainServices, version_1_3_0, cacheContainerWriteOp, false);

        // 3. cache store table operations should reject expressions on non-complex attributes
        ModelNode stringKeyedTable = createStringKeyedTable("ispn_bucket", 100, 100,
                new String[] {"id", "VARCHAR"}, new String[] {"datun", "BINARY"}, new String[] {"version", "${someversion:BIGINT}"}) ;
        ModelNode stringKeyedTableWriteOp = getMixedKeyedJDBCCacheStoreWriteOperation("maximal", ModelKeys.DISTRIBUTED_CACHE, "dist", "string-keyed-table", stringKeyedTable);
        testRejectExpressionsForOperation(mainServices, version_1_3_0, stringKeyedTableWriteOp, true);
    }

    private void testRejectExpressionsForOperation(KernelServices services, ModelVersion version, ModelNode operation, boolean reject)
            throws OperationFailedException {

        // perform operation on the 1.4.0 model
        ModelNode mainResult = services.executeOperation(operation);
        assertEquals(mainResult.toJSONString(true), SUCCESS, mainResult.get(OUTCOME).asString());

        // perform transformed operation on the 1.3.0 model - expect rejection
        OperationTransformer.TransformedOperation transformedOperation = services.transformOperation(version, operation);
        final ModelNode addResult = services.executeOperation(version, transformedOperation);

        if (reject)
            junit.framework.Assert.assertEquals("should reject the expression", FAILED, addResult.get(OUTCOME).asString());
        else
            junit.framework.Assert.assertEquals("should not reject the expression", SUCCESS, addResult.get(OUTCOME).asString());
    }

    private ModelNode createStringKeyedTable(String prefix, int batchSize, int fetchSize, String[] idCol, String[] dataCol, String[] timestampCol) {

        // create a string-keyed-table complex attribute
        ModelNode stringKeyedTable = new ModelNode().setEmptyObject() ;
        stringKeyedTable.get(ModelKeys.PREFIX).set(prefix);
        stringKeyedTable.get(ModelKeys.BATCH_SIZE).set(batchSize);
        stringKeyedTable.get(ModelKeys.FETCH_SIZE).set(fetchSize);

        ModelNode idColumn = stringKeyedTable.get(ModelKeys.ID_COLUMN).setEmptyObject();
        idColumn.get(ModelKeys.NAME).set(idCol[0]) ;
        idColumn.get(ModelKeys.TYPE).set(idCol[1]) ;

        ModelNode dataColumn = stringKeyedTable.get(ModelKeys.DATA_COLUMN).setEmptyObject();
        dataColumn.get(ModelKeys.NAME).set(dataCol[0]) ;
        dataColumn.get(ModelKeys.TYPE).set(dataCol[1]) ;

        ModelNode timestampColumn = stringKeyedTable.get(ModelKeys.TIMESTAMP_COLUMN).setEmptyObject();
        timestampColumn.get(ModelKeys.NAME).set(timestampCol[0]) ;
        timestampColumn.get(ModelKeys.TYPE).set(timestampCol[1]) ;

        return stringKeyedTable ;
    }
}
