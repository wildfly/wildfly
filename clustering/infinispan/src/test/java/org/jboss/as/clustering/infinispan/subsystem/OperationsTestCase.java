package org.jboss.as.clustering.infinispan.subsystem;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;

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

    // subsystem test operations
    // TODO

    // cache container test operations
    static final ModelNode cacheContainerAddOp = getCacheContainerAddOperation("maximal2");
    static final ModelNode cacheContainerRemovekOp = getCacheContainerRemoveOperation("maximal2");
    static final ModelNode readCacheContainerDefaultCacheOp = getCacheContainerReadOperation("maximal", "default-cache");
    static final ModelNode writeCacheContainerDefaultCacheOp = getCacheContainerWriteOperation("maximal", "default-cache", "new-default-cache");

    // cache transport test operations
    // TODO

    // cache test operations
    static final ModelNode localCacheAddOp = getCacheAddOperation("maximal2", ModelKeys.LOCAL_CACHE, "new-cache");
    static final ModelNode localCacheRemovekOp = getCacheRemoveOperation("maximal2", ModelKeys.LOCAL_CACHE, "new-cache");

    // cache locking test operations
    // TODO

    // cache store test operations
    static final ModelNode readDistCacheMixedJDBCStoreDatastoreOp = getMixedKeyedJDBCCacheStoreReadOperation("maximal", ModelKeys.DISTRIBUTED_CACHE, "dist", "datasource");
    static final ModelNode writeDistCacheFileStoreDatastoreOp = getMixedKeyedJDBCCacheStoreWriteOperation("maximal", ModelKeys.DISTRIBUTED_CACHE, "dist", "datasource", "new-datasource");
    static final ModelNode readDistCacheMixedJDBCStoreStringKeyedTableOp = getMixedKeyedJDBCCacheStoreReadOperation("maximal", ModelKeys.DISTRIBUTED_CACHE, "dist", "string-keyed-table");
    // static final ModelNode writeDistCacheFileStoreStringKeyedTableOp = getMixedKeyedJDBCCacheStoreWriteOperation("maximal", ModelKeys.DISTRIBUTED_CACHE, "dist", "string-keyed-table", "new-datasource");

    /*
     * Tests access to cache container attributes
     */
    @Test
    public void testCacheContainerReadWriteOperation() throws Exception {

        // Parse and install the XML into the controller
        String subsystemXml = getSubsystemXml() ;
        KernelServices servicesA = createKernelServicesBuilder(null).setSubsystemXml(subsystemXml).build();

        // read the cache container default cache attribute
        ModelNode result = servicesA.executeOperation(readCacheContainerDefaultCacheOp);
        Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());
        Assert.assertEquals("local", result.get(RESULT).asString());

        // write the default cache attribute
        result = servicesA.executeOperation(writeCacheContainerDefaultCacheOp);
        Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());

        // re-read the default cache attribute
        result = servicesA.executeOperation(readCacheContainerDefaultCacheOp);
        Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());
        Assert.assertEquals("new-default-cache", result.get(RESULT).asString());
    }

    /*
     * Tests access to local cache attributes
     */
    @Test
    public void testLocalCacheReadWriteOperation() throws Exception {

        // Parse and install the XML into the controller
        String subsystemXml = getSubsystemXml() ;
        KernelServices servicesA = createKernelServicesBuilder(null).setSubsystemXml(subsystemXml).build();

        String initialValue = "EAGER";
        ModelNode readOperation = getCacheReadOperation("maximal", ModelKeys.LOCAL_CACHE, "local", ModelKeys.START);

        // read the cache container batching attribute
        ModelNode result = servicesA.executeOperation(readOperation);
        Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());
        Assert.assertEquals(initialValue, result.get(RESULT).asString());

        String newValue = "LAZY";
        ModelNode writeOperation = getCacheWriteOperation("maximal", ModelKeys.LOCAL_CACHE, "local", ModelKeys.START, newValue);

        // write the batching attribute
        result = servicesA.executeOperation(writeOperation);
        Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());

        // re-read the batching attribute
        result = servicesA.executeOperation(readOperation);
        Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());
        Assert.assertEquals(newValue, result.get(RESULT).asString());
    }

    /*
     * Tests access to local cache attributes
     */
    @Test
    public void testDistributedCacheMixedJDBCStoreReadWriteOperation() throws Exception {

        ModelNode stringKeyedTable = createStringKeyedTable() ;

        // Parse and install the XML into the controller
        String subsystemXml = getSubsystemXml() ;
        KernelServices servicesA = createKernelServicesBuilder(null).setSubsystemXml(subsystemXml).build();

        // read the distributed cache mixed-keyed-jdbc-store datasource attribute
        ModelNode result = servicesA.executeOperation(readDistCacheMixedJDBCStoreDatastoreOp);
        Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());
        Assert.assertEquals("java:jboss/jdbc/store", result.get(RESULT).asString());

        // write the batching attribute
        result = servicesA.executeOperation(writeDistCacheFileStoreDatastoreOp);
        Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());

        // re-read the batching attribute
        result = servicesA.executeOperation(readDistCacheMixedJDBCStoreDatastoreOp);
        Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());
        Assert.assertEquals("new-datasource", result.get(RESULT).asString());

         // read the string-keyed-table attribute
        result = servicesA.executeOperation(readDistCacheMixedJDBCStoreStringKeyedTableOp);
        Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());
        Assert.assertEquals(stringKeyedTable.asString(), result.get(RESULT).asString());
    }

    private ModelNode createStringKeyedTable() {

        // create a string-keyed-table complex attribute
        ModelNode stringKeyedTable = new ModelNode().setEmptyObject() ;
        stringKeyedTable.get(ModelKeys.PREFIX).set("ispn_bucket");
        stringKeyedTable.get(ModelKeys.BATCH_SIZE).set(100);
        stringKeyedTable.get(ModelKeys.FETCH_SIZE).set(100);

        ModelNode idColumn = stringKeyedTable.get(ModelKeys.ID_COLUMN).setEmptyObject();
        idColumn.get(ModelKeys.NAME).set("id") ;
        idColumn.get(ModelKeys.TYPE).set("VARCHAR") ;

        ModelNode dataColumn = stringKeyedTable.get(ModelKeys.DATA_COLUMN).setEmptyObject();
        dataColumn.get(ModelKeys.NAME).set("datum") ;
        dataColumn.get(ModelKeys.TYPE).set("BINARY") ;

        ModelNode timestampColumn = stringKeyedTable.get(ModelKeys.TIMESTAMP_COLUMN).setEmptyObject();
        timestampColumn.get(ModelKeys.NAME).set("version") ;
        timestampColumn.get(ModelKeys.TYPE).set("BIGINT") ;

        return stringKeyedTable ;
    }

    /*
     * Tests adding dist cache with deprecated virtual nodes attribute
     */
    @Test
    public void testDistCacheAddOperationWithVirtualNodes() throws Exception {

        // Parse and install the XML into the controller
        String subsystemXml = getSubsystemXml() ;
        KernelServices servicesA = createKernelServicesBuilder(null).setSubsystemXml(subsystemXml).build();

        // add the distributed cache with virtual-nodes = 6, should lead to segments value of 36 (6*6)
        ModelNode distCacheAddOp = getCacheAddOperation("maximal", ModelKeys.DISTRIBUTED_CACHE, "new-dist-cache");
        distCacheAddOp.get(ModelKeys.MODE).set("SYNC");
        distCacheAddOp.get(ModelKeys.VIRTUAL_NODES).set(6);
        ModelNode result = servicesA.executeOperation(distCacheAddOp);
        Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());

        // read the segments attribute
        ModelNode readDistCacheSegmentsOp = getCacheReadOperation("maximal", ModelKeys.DISTRIBUTED_CACHE, "new-dist-cache", "segments");
        result = servicesA.executeOperation(readDistCacheSegmentsOp);
        Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());
        Assert.assertEquals("36", result.get(RESULT).asString());
    }

}