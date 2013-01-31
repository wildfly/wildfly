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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import java.io.IOException;
import java.util.List;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
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
    public void testTransformer712() throws Exception {
        testTransformer_1_3_0("7.1.2.Final");
    }

    @Test
    public void testTransformer713() throws Exception {
        testTransformer_1_3_0("7.1.3.Final");
    }

    private void testTransformer_1_3_0(String asVersion) throws Exception {
        ModelVersion version = ModelVersion.create(1, 3);
        KernelServicesBuilder builder = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT)
                .setSubsystemXml(getSubsystemXml());
        builder.createLegacyKernelServicesBuilder(null, version)
            .addMavenResourceURL("org.jboss.as:jboss-as-clustering-infinispan:" + asVersion);

        KernelServices mainServices = builder.build();
        Assert.assertTrue("main services did not boot", mainServices.isSuccessfulBoot()); ;

        checkSubsystemModelTransformation(mainServices, version);

        // check that segments is translated into virtual nodes and both segments and indexing properties are removed
        ModelNode model = mainServices.readTransformedModel(version);
        ModelNode distCache = model.get(SUBSYSTEM,"infinispan",ModelKeys.CACHE_CONTAINER, "maximal", ModelKeys.DISTRIBUTED_CACHE, "dist") ;
        Assert.assertFalse(distCache.has(ModelKeys.INDEXING_PROPERTIES));
        Assert.assertFalse(distCache.has(ModelKeys.SEGMENTS));
        Assert.assertTrue(distCache.get(ModelKeys.VIRTUAL_NODES).isDefined());
    }

    @Test
    public void testRejectExpressions712() throws Exception {
        testRejectExpressions_1_3_0("7.1.2.Final");
    }

    @Test
    public void testRejectExpressions713() throws Exception {
        testRejectExpressions_1_3_0("7.1.3.Final");
    }

    public void testRejectExpressions_1_3_0(String asVersion) throws Exception {
        // create builder for current subsystem version
        KernelServicesBuilder builder = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT);

        // create builder for legacy subsystem version
        ModelVersion version_1_3_0 = ModelVersion.create(1, 3, 0);
        builder.createLegacyKernelServicesBuilder(null, version_1_3_0)
            .addMavenResourceURL("org.jboss.as:jboss-as-clustering-infinispan:" + asVersion);

        KernelServices mainServices = builder.build();
        KernelServices legacyServices = mainServices.getLegacyServices(version_1_3_0);
        Assert.assertNotNull(legacyServices);
        Assert.assertTrue("main services did not boot", mainServices.isSuccessfulBoot()); ;
        Assert.assertTrue(legacyServices.isSuccessfulBoot());

        List<ModelNode> xmlOps = builder.parseXmlResource("infinispan-transformer_1_4-expressions.xml");

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
                    FailedOperationTransformationConfig.ChainedConfig.createBuilder(InfinispanRejectedExpressions_1_3.ACCEPT14_REJECT13_CACHE_ATTRIBUTES)
                    .addConfig(new FailedOperationTransformationConfig.RejectExpressionsConfig(InfinispanRejectedExpressions_1_3.ACCEPT14_REJECT13_CACHE_ATTRIBUTES))
                    .addConfig(new RemoveResolvedIndexingPropertiesConfig(CacheResource.INDEXING_PROPERTIES)).build()/*.setNotExpectedWriteFailure(ModelKeys.SEGMENTS)*/);

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


            FailedOperationTransformationConfig.RejectExpressionsConfig keyedTableComplexChildConfig =
                    new FailedOperationTransformationConfig.RejectExpressionsConfig(BaseJDBCStoreResource.COMMON_JDBC_STORE_TABLE_ATTRIBUTES);


            // cache store attributes
            for (int k=0; k < storePaths.length; k++) {
                // reject expressions on operations on stores and store properties
                config.addFailedAttribute(subsystemAddress.append(CacheContainerResource.CONTAINER_PATH).append(cachePaths[i]).append(storePaths[k]),
                        new FailedOperationTransformationConfig.RejectExpressionsConfig(InfinispanRejectedExpressions_1_3.ACCEPT14_REJECT13_STORE_ATTRIBUTES)
                            .configureComplexAttribute(ModelKeys.STRING_KEYED_TABLE, keyedTableComplexChildConfig)
                            .configureComplexAttribute(ModelKeys.BINARY_KEYED_TABLE, keyedTableComplexChildConfig));

                config.addFailedAttribute(subsystemAddress.append(CacheContainerResource.CONTAINER_PATH).append(cachePaths[i]).append(storePaths[k]).append(StoreWriteBehindResource.STORE_WRITE_BEHIND_PATH),
                        new FailedOperationTransformationConfig.RejectExpressionsConfig(InfinispanRejectedExpressions_1_3.ACCEPT14_REJECT13_STORE_ATTRIBUTES));

                config.addFailedAttribute(subsystemAddress.append(CacheContainerResource.CONTAINER_PATH).append(cachePaths[i]).append(storePaths[k]).append(StorePropertyResource.STORE_PROPERTY_PATH),
                        new FailedOperationTransformationConfig.RejectExpressionsConfig(InfinispanRejectedExpressions_1_3.ACCEPT14_REJECT13_STORE_ATTRIBUTES));
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
            if (attribute.isDefined() && attrName.equals(CacheResource.INDEXING_PROPERTIES.getName())) {
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
