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
    // cache container test operations
    static final ModelNode cacheContainerAddOp = getCacheContainerAddOperation("maximal2");
    static final ModelNode cacheContainerRemovekOp = getCacheContainerRemoveOperation("maximal2");
    static final ModelNode readCacheContainerDefaultCacheOp = getCacheContainerReadOperation("maximal", "default-cache");
    static final ModelNode writeCacheContainerDefaultCacheOp = getCacheContainerWriteOperation("maximal", "default-cache", "new-default-cache");
    // cache transport test operations
    // cache test operations
    static final ModelNode localCacheAddOp = getCacheAddOperation("maximal2", "new-cache", ModelKeys.LOCAL_CACHE);
    static final ModelNode localCacheRemovekOp = getCacheRemoveOperation("maximal2", "new-cache", ModelKeys.LOCAL_CACHE);
    static final ModelNode readLocalCacheBatchingOp = getCacheReadOperation("maximal", "local", ModelKeys.LOCAL_CACHE, "batching");
    static final ModelNode writeLocalCacheBatchingOp = getCacheWriteOperation("maximal", "local", ModelKeys.LOCAL_CACHE, "batching", "false");

    // cache locking test operations
    // cache store test operations

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

        // read the cache container batching attribute
        ModelNode result = servicesA.executeOperation(readLocalCacheBatchingOp);
        Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());
        Assert.assertEquals("true", result.get(RESULT).asString());

        // write the batching attribute
        result = servicesA.executeOperation(writeLocalCacheBatchingOp);
        Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());

        // re-read the batching attribute
        result = servicesA.executeOperation(readLocalCacheBatchingOp);
        Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());
        Assert.assertEquals("false", result.get(RESULT).asString());
    }

}