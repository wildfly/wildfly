package org.jboss.as.clustering.infinispan.subsystem;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Test;

/**
 *  Test case for testing individual management operations.
 *
 *  These test cases are based on the XML config in subsystem-infinispan-test,
 *  a non-exhaustive subsystem configuration.
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
*/
public class OperationsTestCase extends OperationTestCaseBase {

    /*
     * Tests access to cache container attributes
     */
    @Test
    public void testCacheContainerReadWriteOperation() throws Exception {

        // Parse and install the XML into the controller
        String subsystemXml = getSubsystemXml();
        KernelServices servicesA = this.createKernelServicesBuilder().setSubsystemXml(subsystemXml).build();

        // read the cache container default cache attribute
        ModelNode result = servicesA.executeOperation(getCacheContainerReadOperation("maximal", CacheContainerResourceDefinition.Attribute.DEFAULT_CACHE));
        Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());
        Assert.assertEquals("local", result.get(RESULT).asString());

        // write the default cache attribute
        result = servicesA.executeOperation(getCacheContainerWriteOperation("maximal", CacheContainerResourceDefinition.Attribute.DEFAULT_CACHE, "new-default-cache"));
        Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());

        // re-read the default cache attribute
        result = servicesA.executeOperation(getCacheContainerReadOperation("maximal", CacheContainerResourceDefinition.Attribute.DEFAULT_CACHE));
        Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());
        Assert.assertEquals("new-default-cache", result.get(RESULT).asString());
    }

    /*
     * Tests access to local cache attributes
     */
    @Test
    public void testLocalCacheReadWriteOperation() throws Exception {

        // Parse and install the XML into the controller
        String subsystemXml = getSubsystemXml();
        KernelServices servicesA = this.createKernelServicesBuilder().setSubsystemXml(subsystemXml).build();

        ModelNode readOperation = getCacheReadOperation("maximal", LocalCacheResourceDefinition.WILDCARD_PATH.getKey(), "local", CacheResourceDefinition.Attribute.STATISTICS_ENABLED);

        // read the cache container batching attribute
        ModelNode result = servicesA.executeOperation(readOperation);
        Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());
        Assert.assertTrue(result.get(RESULT).asBoolean());

        ModelNode writeOperation = getCacheWriteOperation("maximal", LocalCacheResourceDefinition.WILDCARD_PATH.getKey(), "local", CacheResourceDefinition.Attribute.STATISTICS_ENABLED, "false");

        // write the batching attribute
        result = servicesA.executeOperation(writeOperation);
        Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());

        // re-read the batching attribute
        result = servicesA.executeOperation(readOperation);
        Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());
        Assert.assertFalse(result.get(RESULT).asBoolean());
    }

    /*
     * Tests access to local cache attributes
     */
    @Test
    public void testDistributedCacheJDBCStoreReadWriteOperation() throws Exception {

        // Parse and install the XML into the controller
        String subsystemXml = getSubsystemXml();
        KernelServices servicesA = this.createKernelServicesBuilder().setSubsystemXml(subsystemXml).build();

        // read the distributed cache mixed-keyed-jdbc-store datasource attribute
        ModelNode result = servicesA.executeOperation(getJDBCCacheStoreReadOperation("maximal", DistributedCacheResourceDefinition.WILDCARD_PATH.getKey(), "dist", JDBCStoreResourceDefinition.Attribute.DATA_SOURCE));
        Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());
        Assert.assertEquals("ExampleDS", result.get(RESULT).asString());

        // write the batching attribute
        result = servicesA.executeOperation(getJDBCCacheStoreWriteOperation("maximal", DistributedCacheResourceDefinition.WILDCARD_PATH.getKey(), "dist", JDBCStoreResourceDefinition.Attribute.DATA_SOURCE, "new-datasource"));
        Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());

        // re-read the batching attribute
        result = servicesA.executeOperation(getJDBCCacheStoreReadOperation("maximal", DistributedCacheResourceDefinition.WILDCARD_PATH.getKey(), "dist", JDBCStoreResourceDefinition.Attribute.DATA_SOURCE));
        Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());
        Assert.assertEquals("new-datasource", result.get(RESULT).asString());
    }

    @Test
    public void testStoreProperties() throws Exception {
        KernelServices services = this.createKernelServicesBuilder().setSubsystemXml(this.getSubsystemXml()).build();

        PathAddress address = getRemoteCacheStoreAddress("maximal", InvalidationCacheResourceDefinition.WILDCARD_PATH.getKey(), "invalid");
        String key = "infinispan.client.hotrod.ping_on_startup";
        String value = "true";

        ModelNode operation = Util.createMapPutOperation(address, StoreResourceDefinition.Attribute.PROPERTIES.getName(), key, value);
        ModelNode result = services.executeOperation(operation);
        Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());
        Assert.assertFalse(result.get(RESULT).isDefined());

        operation = Util.createMapGetOperation(address, StoreResourceDefinition.Attribute.PROPERTIES.getName(), key);
        result = services.executeOperation(operation);
        Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());
        Assert.assertEquals(value, result.get(RESULT).asString());

        operation = Util.createMapRemoveOperation(address, StoreResourceDefinition.Attribute.PROPERTIES.getName(), key);
        result = services.executeOperation(operation);
        Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());
        Assert.assertFalse(result.get(RESULT).isDefined());

        operation = Util.createMapGetOperation(address, StoreResourceDefinition.Attribute.PROPERTIES.getName(), key);
        result = services.executeOperation(operation);
        Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());
        Assert.assertFalse(result.get(RESULT).isDefined());
    }

    @Test
    public void testAliases() throws Exception {
        KernelServices services = this.createKernelServicesBuilder().setSubsystemXml(this.getSubsystemXml()).build();

        PathAddress address = getCacheContainerAddress("minimal");
        String alias = "alias0";

        ModelNode operation = Util.createListAddOperation(address, CacheContainerResourceDefinition.ListAttribute.ALIASES.getName(), alias);
        ModelNode result = services.executeOperation(operation);
        Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());
        Assert.assertFalse(result.get(RESULT).isDefined());

        operation = Util.createListGetOperation(address, CacheContainerResourceDefinition.ListAttribute.ALIASES.getName(), 0);
        result = services.executeOperation(operation);
        Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());
        Assert.assertEquals(new ModelNode(alias), result.get(RESULT));

        operation = Util.createListRemoveOperation(address, CacheContainerResourceDefinition.ListAttribute.ALIASES.getName(), 0);
        result = services.executeOperation(operation);
        Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());
        Assert.assertFalse(result.get(RESULT).isDefined());

        operation = Util.createListGetOperation(address, CacheContainerResourceDefinition.ListAttribute.ALIASES.getName(), 0);
        result = services.executeOperation(operation);
        Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());
        Assert.assertFalse(result.get(RESULT).isDefined());
    }
}