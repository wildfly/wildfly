package org.jboss.as.clustering.infinispan.subsystem;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;

import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Test;

/**
 *  Test case for testing individual management operations on runtime metrics.
 *
 *  These test cases are based on the XML config in subsystem-infinispan-test,
 *  a non-exhaustive subsystem configuration.
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
*/
public class RuntimeMetricsTestCase extends OperationTestCaseBase {

    // cache container test operations
    static final ModelNode readCacheContainerCacheManagerStatusOp = getCacheContainerReadOperation("maximal", MetricKeys.CACHE_MANAGER_STATUS);

    // cache test operations
    static final ModelNode readLocalCacheStatusOp = getCacheReadOperation("maximal", ModelKeys.LOCAL_CACHE, "local", MetricKeys.CACHE_STATUS);

    /*
     * Tests access to cache container attributes
     */
    @Test
    public void testCacheContainerReadOnlyMetrics() throws Exception {

        // Parse and install the XML into the controller
        String subsystemXml = getSubsystemXml() ;
        KernelServices servicesA = createKernelServicesBuilder(null).setSubsystemXml(subsystemXml).build();

        // read the cache container default cache attribute
        ModelNode result = servicesA.executeOperation(readCacheContainerCacheManagerStatusOp);
        Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());
        System.out.println("cache manager status = " + result.get(RESULT).toString());
        // Assert.assertEquals("local", result.get(RESULT).asString());
    }

    /*
     * Tests access to local cache attributes
     */
    @Test
    public void testLocalCacheReadOnlyMetrics() throws Exception {

        // Parse and install the XML into the controller
        String subsystemXml = getSubsystemXml() ;
        KernelServices servicesA = createKernelServicesBuilder(null).setSubsystemXml(subsystemXml).build();

        // read the cache container batching attribute
        ModelNode result = servicesA.executeOperation(readLocalCacheStatusOp);
        Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());
        System.out.println("cache status = " + result.get(RESULT).toString());
        // Assert.assertEquals("true", result.get(RESULT).asString());
    }

}