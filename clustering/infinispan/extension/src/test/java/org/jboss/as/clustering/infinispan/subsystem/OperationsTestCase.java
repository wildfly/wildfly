package org.jboss.as.clustering.infinispan.subsystem;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;

import java.util.Collections;

import org.jboss.as.clustering.controller.Attribute;
import org.jboss.as.clustering.controller.Operations;
import org.jboss.as.clustering.controller.SimpleAttribute;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
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
    @SuppressWarnings("deprecation")
    @Test
    public void testDistributedCacheJDBCStoreReadWriteOperation() throws Exception {

        ModelNode stringKeyedTable = createStringKeyedTable();

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

         // read the string-keyed-table attribute
        result = servicesA.executeOperation(getJDBCCacheStoreReadOperation("maximal", DistributedCacheResourceDefinition.WILDCARD_PATH.getKey(), "dist", MixedKeyedJDBCStoreResourceDefinition.DeprecatedAttribute.STRING_TABLE));
        Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());
        Assert.assertEquals(stringKeyedTable.asPropertyList().size(), result.get(RESULT).asPropertyList().size());
        for (Property property : stringKeyedTable.asPropertyList()) {
            Assert.assertTrue(result.get(RESULT).hasDefined(property.getName()));
            Assert.assertEquals(property.getValue(), result.get(RESULT).get(property.getName()));
        }
    }

    @SuppressWarnings("deprecation")
    private static ModelNode createStringKeyedTable() {

        // create a string-keyed-table complex attribute
        ModelNode stringKeyedTable = new ModelNode().setEmptyObject();
        stringKeyedTable.get(StringTableResourceDefinition.Attribute.PREFIX.getName()).set("ispn_bucket");
        stringKeyedTable.get(TableResourceDefinition.DeprecatedAttribute.BATCH_SIZE.getName()).set(100);
        stringKeyedTable.get(TableResourceDefinition.Attribute.FETCH_SIZE.getName()).set(100);

        ModelNode idColumn = stringKeyedTable.get(TableResourceDefinition.ColumnAttribute.ID.getName()).setEmptyObject();
        idColumn.get(TableResourceDefinition.ColumnAttribute.ID.getColumnName().getName()).set("id");
        idColumn.get(TableResourceDefinition.ColumnAttribute.ID.getColumnType().getName()).set("VARCHAR");

        ModelNode dataColumn = stringKeyedTable.get(TableResourceDefinition.ColumnAttribute.DATA.getName()).setEmptyObject();
        dataColumn.get(TableResourceDefinition.ColumnAttribute.DATA.getColumnName().getName()).set("datum");
        dataColumn.get(TableResourceDefinition.ColumnAttribute.DATA.getColumnType().getName()).set("BINARY");

        ModelNode timestampColumn = stringKeyedTable.get(TableResourceDefinition.ColumnAttribute.TIMESTAMP.getName()).setEmptyObject();
        timestampColumn.get(TableResourceDefinition.ColumnAttribute.TIMESTAMP.getColumnName().getName()).set("version");
        timestampColumn.get(TableResourceDefinition.ColumnAttribute.TIMESTAMP.getColumnType().getName()).set("BIGINT");

        return stringKeyedTable;
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testStoreProperties() throws Exception {
        KernelServices services = this.createKernelServicesBuilder().setSubsystemXml(this.getSubsystemXml()).build();

        PathAddress address = getRemoteCacheStoreAddress("maximal", InvalidationCacheResourceDefinition.WILDCARD_PATH.getKey(), "invalid");
        String key = "infinispan.client.hotrod.ping_on_startup";
        String value = "true";

        ModelNode operation = Operations.createMapPutOperation(address, StoreResourceDefinition.Attribute.PROPERTIES, key, value);
        ModelNode result = services.executeOperation(operation);
        Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());
        Assert.assertFalse(result.get(RESULT).isDefined());

        operation = Operations.createMapGetOperation(address, StoreResourceDefinition.Attribute.PROPERTIES, key);
        result = services.executeOperation(operation);
        Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());
        Assert.assertEquals(value, result.get(RESULT).asString());

        operation = Operations.createMapRemoveOperation(address, StoreResourceDefinition.Attribute.PROPERTIES, key);
        result = services.executeOperation(operation);
        Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());
        Assert.assertFalse(result.get(RESULT).isDefined());

        operation = Operations.createMapGetOperation(address, StoreResourceDefinition.Attribute.PROPERTIES, key);
        result = services.executeOperation(operation);
        Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());
        Assert.assertFalse(result.get(RESULT).isDefined());

        // Validate that properties can still be added/removed/updated via child property resources
        PathAddress propertyAddress = address.append(StorePropertyResourceDefinition.pathElement(key));
        operation = Operations.createAddOperation(propertyAddress, Collections.<Attribute, ModelNode>singletonMap(new SimpleAttribute(StorePropertyResourceDefinition.VALUE), new ModelNode(value)));
        result = services.executeOperation(operation);
        Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());
        Assert.assertFalse(result.get(RESULT).isDefined());

        operation = Operations.createMapGetOperation(address, StoreResourceDefinition.Attribute.PROPERTIES, key);
        result = services.executeOperation(operation);
        Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());
        Assert.assertEquals(value, result.get(RESULT).asString());

        value = "false";
        operation = Operations.createWriteAttributeOperation(propertyAddress, new SimpleAttribute(StorePropertyResourceDefinition.VALUE), new ModelNode(value));
        result = services.executeOperation(operation);
        Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());
        Assert.assertFalse(result.get(RESULT).isDefined());

        operation = Operations.createMapGetOperation(address, StoreResourceDefinition.Attribute.PROPERTIES, key);
        result = services.executeOperation(operation);
        Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());
        Assert.assertEquals(value, result.get(RESULT).asString());

        operation = Operations.createReadAttributeOperation(propertyAddress, new SimpleAttribute(StorePropertyResourceDefinition.VALUE));
        result = services.executeOperation(operation);
        Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());
        Assert.assertEquals(value, result.get(RESULT).asString());

        operation = Util.createRemoveOperation(propertyAddress);
        result = services.executeOperation(operation);
        Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());
        Assert.assertFalse(result.get(RESULT).isDefined());

        operation = Operations.createMapGetOperation(address, StoreResourceDefinition.Attribute.PROPERTIES, key);
        result = services.executeOperation(operation);
        Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());
        Assert.assertFalse(result.get(RESULT).isDefined());
    }

    @Test
    public void testAliases() throws Exception {
        KernelServices services = this.createKernelServicesBuilder().setSubsystemXml(this.getSubsystemXml()).build();

        PathAddress address = getCacheContainerAddress("minimal");
        String alias = "alias0";

        ModelNode operation = Operations.createListAddOperation(address, CacheContainerResourceDefinition.Attribute.ALIASES, alias);
        ModelNode result = services.executeOperation(operation);
        Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());
        Assert.assertFalse(result.get(RESULT).isDefined());

        operation = Operations.createListGetOperation(address, CacheContainerResourceDefinition.Attribute.ALIASES, 0);
        result = services.executeOperation(operation);
        Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());
        Assert.assertEquals(new ModelNode(alias), result.get(RESULT));

        operation = Operations.createListRemoveOperation(address, CacheContainerResourceDefinition.Attribute.ALIASES, 0);
        result = services.executeOperation(operation);
        Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());
        Assert.assertFalse(result.get(RESULT).isDefined());

        operation = Operations.createListGetOperation(address, CacheContainerResourceDefinition.Attribute.ALIASES, 0);
        result = services.executeOperation(operation);
        Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());
        Assert.assertFalse(result.get(RESULT).isDefined());

        // Validate that aliases can still be added/removed via legacy operations
        operation = Util.createOperation("add-alias", address);
        operation.get(ModelDescriptionConstants.NAME).set(alias);
        result = services.executeOperation(operation);
        Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());
        Assert.assertFalse(result.get(RESULT).isDefined());

        operation = Operations.createListGetOperation(address, CacheContainerResourceDefinition.Attribute.ALIASES, 0);
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

        operation = Operations.createListGetOperation(address, CacheContainerResourceDefinition.Attribute.ALIASES, 0);
        result = services.executeOperation(operation);
        Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());
        Assert.assertFalse(result.get(RESULT).isDefined());
    }
}