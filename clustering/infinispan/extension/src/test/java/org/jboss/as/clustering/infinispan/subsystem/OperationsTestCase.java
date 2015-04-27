package org.jboss.as.clustering.infinispan.subsystem;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;

import org.jboss.as.clustering.controller.Operations;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
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
        ModelNode result = servicesA.executeOperation(getCacheContainerReadOperation("maximal", "default-cache"));
        Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());
        Assert.assertEquals("local", result.get(RESULT).asString());

        // write the default cache attribute
        result = servicesA.executeOperation(getCacheContainerWriteOperation("maximal", "default-cache", "new-default-cache"));
        Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());

        // re-read the default cache attribute
        result = servicesA.executeOperation(getCacheContainerReadOperation("maximal", "default-cache"));
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

        ModelNode readOperation = getCacheReadOperation("maximal", ModelKeys.LOCAL_CACHE, "local", CacheResourceDefinition.STATISTICS_ENABLED.getName());

        // read the cache container batching attribute
        ModelNode result = servicesA.executeOperation(readOperation);
        Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());
        Assert.assertTrue(result.get(RESULT).asBoolean());

        ModelNode writeOperation = getCacheWriteOperation("maximal", ModelKeys.LOCAL_CACHE, "local", CacheResourceDefinition.STATISTICS_ENABLED.getName(), "false");

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
    public void testDistributedCacheMixedJDBCStoreReadWriteOperation() throws Exception {

        ModelNode stringKeyedTable = createStringKeyedTable();

        // Parse and install the XML into the controller
        String subsystemXml = getSubsystemXml();
        KernelServices servicesA = this.createKernelServicesBuilder().setSubsystemXml(subsystemXml).build();

        // read the distributed cache mixed-keyed-jdbc-store datasource attribute
        ModelNode result = servicesA.executeOperation(getMixedKeyedJDBCCacheStoreReadOperation("maximal", ModelKeys.DISTRIBUTED_CACHE, "dist", "datasource"));
        Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());
        Assert.assertEquals("java:jboss/jdbc/store", result.get(RESULT).asString());

        // write the batching attribute
        result = servicesA.executeOperation(getMixedKeyedJDBCCacheStoreWriteOperation("maximal", ModelKeys.DISTRIBUTED_CACHE, "dist", "datasource", "new-datasource"));
        Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());

        // re-read the batching attribute
        result = servicesA.executeOperation(getMixedKeyedJDBCCacheStoreReadOperation("maximal", ModelKeys.DISTRIBUTED_CACHE, "dist", "datasource"));
        Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());
        Assert.assertEquals("new-datasource", result.get(RESULT).asString());

         // read the string-keyed-table attribute
        result = servicesA.executeOperation(getMixedKeyedJDBCCacheStoreReadOperation("maximal", ModelKeys.DISTRIBUTED_CACHE, "dist", "string-keyed-table"));
        Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());
        Assert.assertEquals(stringKeyedTable.asString(), result.get(RESULT).asString());
    }

    private static ModelNode createStringKeyedTable() {

        // create a string-keyed-table complex attribute
        ModelNode stringKeyedTable = new ModelNode().setEmptyObject();
        stringKeyedTable.get(ModelKeys.PREFIX).set("ispn_bucket");
        stringKeyedTable.get(ModelKeys.BATCH_SIZE).set(100);
        stringKeyedTable.get(ModelKeys.FETCH_SIZE).set(100);

        ModelNode idColumn = stringKeyedTable.get(ModelKeys.ID_COLUMN).setEmptyObject();
        idColumn.get(ModelKeys.NAME).set("id");
        idColumn.get(ModelKeys.TYPE).set("VARCHAR");

        ModelNode dataColumn = stringKeyedTable.get(ModelKeys.DATA_COLUMN).setEmptyObject();
        dataColumn.get(ModelKeys.NAME).set("datum");
        dataColumn.get(ModelKeys.TYPE).set("BINARY");

        ModelNode timestampColumn = stringKeyedTable.get(ModelKeys.TIMESTAMP_COLUMN).setEmptyObject();
        timestampColumn.get(ModelKeys.NAME).set("version");
        timestampColumn.get(ModelKeys.TYPE).set("BIGINT");

        return stringKeyedTable;
    }

    @Test
    public void testStoreProperties() throws Exception {
        KernelServices services = this.createKernelServicesBuilder().setSubsystemXml(this.getSubsystemXml()).build();

        PathAddress address = getRemoteCacheStoreAddress("maximal", ModelKeys.INVALIDATION_CACHE, "invalid");
        String key = "infinispan.client.hotrod.ping_on_startup";
        String value = "true";

        ModelNode operation = Operations.createMapPutOperation(address, StoreResourceDefinition.PROPERTIES.getName(), key, value);
        ModelNode result = services.executeOperation(operation);
        Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());
        Assert.assertFalse(result.get(RESULT).isDefined());

        operation = Operations.createMapGetOperation(address, StoreResourceDefinition.PROPERTIES.getName(), key);
        result = services.executeOperation(operation);
        Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());
        Assert.assertEquals(value, result.get(RESULT).asString());

        operation = Operations.createMapRemoveOperation(address, StoreResourceDefinition.PROPERTIES.getName(), key);
        result = services.executeOperation(operation);
        Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());
        Assert.assertFalse(result.get(RESULT).isDefined());

        operation = Operations.createMapGetOperation(address, StoreResourceDefinition.PROPERTIES.getName(), key);
        result = services.executeOperation(operation);
        Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());
        Assert.assertFalse(result.get(RESULT).isDefined());

        // Validate that properties can still be added/removed/updated via child property resources
        PathAddress propertyAddress = address.append(StorePropertyResourceDefinition.pathElement(key));
        operation = Util.createAddOperation(propertyAddress);
        operation.get(StorePropertyResourceDefinition.VALUE.getName()).set(value);
        result = services.executeOperation(operation);
        Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());
        Assert.assertFalse(result.get(RESULT).isDefined());

        operation = Operations.createMapGetOperation(address, StoreResourceDefinition.PROPERTIES.getName(), key);
        result = services.executeOperation(operation);
        Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());
        Assert.assertEquals(value, result.get(RESULT).asString());

        value = "false";
        operation = Operations.createWriteAttributeOperation(propertyAddress, StorePropertyResourceDefinition.VALUE.getName(), new ModelNode(value));
        result = services.executeOperation(operation);
        Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());
        Assert.assertFalse(result.get(RESULT).isDefined());

        operation = Operations.createMapGetOperation(address, StoreResourceDefinition.PROPERTIES.getName(), key);
        result = services.executeOperation(operation);
        Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());
        Assert.assertEquals(value, result.get(RESULT).asString());

        operation = Operations.createReadAttributeOperation(propertyAddress, StorePropertyResourceDefinition.VALUE.getName());
        result = services.executeOperation(operation);
        Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());
        Assert.assertEquals(value, result.get(RESULT).asString());

        operation = Util.createRemoveOperation(propertyAddress);
        result = services.executeOperation(operation);
        Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());
        Assert.assertFalse(result.get(RESULT).isDefined());

        operation = Operations.createMapGetOperation(address, StoreResourceDefinition.PROPERTIES.getName(), key);
        result = services.executeOperation(operation);
        Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());
        Assert.assertFalse(result.get(RESULT).isDefined());
    }

    @Test
    public void testAliases() throws Exception {
        KernelServices services = this.createKernelServicesBuilder().setSubsystemXml(this.getSubsystemXml()).build();

        PathAddress address = getCacheContainerAddress("minimal");
        String alias = "alias0";

        ModelNode operation = Operations.createListAddOperation(address, CacheContainerResourceDefinition.ALIASES.getName(), alias);
        ModelNode result = services.executeOperation(operation);
        Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());
        Assert.assertFalse(result.get(RESULT).isDefined());

        operation = Operations.createListGetOperation(address, CacheContainerResourceDefinition.ALIASES.getName(), 0);
        result = services.executeOperation(operation);
        Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());
        Assert.assertEquals(new ModelNode(alias), result.get(RESULT));

        operation = Operations.createListRemoveOperation(address, CacheContainerResourceDefinition.ALIASES.getName(), 0);
        result = services.executeOperation(operation);
        Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());
        Assert.assertFalse(result.get(RESULT).isDefined());

        operation = Operations.createListGetOperation(address, CacheContainerResourceDefinition.ALIASES.getName(), 0);
        result = services.executeOperation(operation);
        Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());
        Assert.assertFalse(result.get(RESULT).isDefined());

        // Validate that aliases can still be added/removed via legacy operations
        operation = Util.createOperation("add-alias", address);
        operation.get(ModelDescriptionConstants.NAME).set(alias);
        result = services.executeOperation(operation);
        Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());
        Assert.assertFalse(result.get(RESULT).isDefined());

        operation = Operations.createListGetOperation(address, CacheContainerResourceDefinition.ALIASES.getName(), 0);
        result = services.executeOperation(operation);
        Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());
        /* This currently fails due to WFCORE-626, requires wildfly-core-1.0.0.Beta4
        Assert.assertEquals(new ModelNode(alias), result.get(RESULT));
        */
        operation = Util.createOperation("remove-alias", address);
        operation.get(ModelDescriptionConstants.NAME).set(alias);
        result = services.executeOperation(operation);
        Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());
        Assert.assertFalse(result.get(RESULT).isDefined());

        operation = Operations.createListGetOperation(address, CacheContainerResourceDefinition.ALIASES.getName(), 0);
        result = services.executeOperation(operation);
        Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());
        Assert.assertFalse(result.get(RESULT).isDefined());
    }

    @Test
    public void testIndexingProperties() throws Exception {
        KernelServices services = this.createKernelServicesBuilder().setSubsystemXml(this.getSubsystemXml()).build();

        PathAddress address = getCacheAddress("capedwarf", ModelKeys.DISTRIBUTED_CACHE, "tasks");
        String key = "hibernate.test";
        String value = "true";

        ModelNode operation = Operations.createMapPutOperation(address, CacheResourceDefinition.INDEXING_PROPERTIES.getName(), key, value);
        ModelNode result = services.executeOperation(operation);
        Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());
        Assert.assertFalse(result.get(RESULT).isDefined());

        operation = Operations.createMapGetOperation(address, CacheResourceDefinition.INDEXING_PROPERTIES.getName(), key);
        result = services.executeOperation(operation);
        Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());
        Assert.assertEquals(value, result.get(RESULT).asString());

        operation = Operations.createMapRemoveOperation(address, CacheResourceDefinition.INDEXING_PROPERTIES.getName(), key);
        result = services.executeOperation(operation);
        Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());
        Assert.assertFalse(result.get(RESULT).isDefined());

        operation = Operations.createMapGetOperation(address, CacheResourceDefinition.INDEXING_PROPERTIES.getName(), key);
        result = services.executeOperation(operation);
        Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());
        Assert.assertFalse(result.get(RESULT).isDefined());
    }
}