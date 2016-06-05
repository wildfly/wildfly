package org.jboss.as.clustering.infinispan.subsystem;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.*;

import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.byteman.contrib.bmunit.BMRule;
import org.jboss.byteman.contrib.bmunit.BMUnitRunner;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceName;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
* Test case for testing sequences of management operations.
*
* @author Richard Achmatowicz (c) 2011 Red Hat Inc.
*/
@RunWith(BMUnitRunner.class)
public class OperationSequencesTestCase extends OperationTestCaseBase {

    @Test
    public void testCacheContainerAddRemoveAddSequence() throws Exception {

        // Parse and install the XML into the controller
        String subsystemXml = getSubsystemXml() ;
        KernelServices servicesA = this.createKernelServicesBuilder().setSubsystemXml(subsystemXml).build();

        ModelNode addContainerOp = getCacheContainerAddOperation("maximal2");
        ModelNode removeContainerOp = getCacheContainerRemoveOperation("maximal2");
        ModelNode addCacheOp = getCacheAddOperation("maximal2",  LocalCacheResourceDefinition.WILDCARD_PATH.getKey(), "fred");

        // add a cache container
        ModelNode result = servicesA.executeOperation(addContainerOp);
        Assert.assertEquals(result.get(FAILURE_DESCRIPTION).asString(), SUCCESS, result.get(OUTCOME).asString());

        // add a local cache
        result = servicesA.executeOperation(addCacheOp);
        Assert.assertEquals(result.get(FAILURE_DESCRIPTION).asString(), SUCCESS, result.get(OUTCOME).asString());

        // remove the cache container
        result = servicesA.executeOperation(removeContainerOp);
        Assert.assertEquals(result.get(FAILURE_DESCRIPTION).asString(), SUCCESS, result.get(OUTCOME).asString());

        // add the same cache container
        result = servicesA.executeOperation(addContainerOp);
        Assert.assertEquals(result.get(FAILURE_DESCRIPTION).asString(), SUCCESS, result.get(OUTCOME).asString());

        // add the same local cache
        result = servicesA.executeOperation(addCacheOp);
        Assert.assertEquals(result.get(FAILURE_DESCRIPTION).asString(), SUCCESS, result.get(OUTCOME).asString());
    }

    @Test
    public void testCacheContainerRemoveRemoveSequence() throws Exception {

        // Parse and install the XML into the controller
        String subsystemXml = getSubsystemXml() ;
        KernelServices servicesA = this.createKernelServicesBuilder().setSubsystemXml(subsystemXml).build();

        ModelNode addContainerOp = getCacheContainerAddOperation("maximal2");
        ModelNode removeContainerOp = getCacheContainerRemoveOperation("maximal2");
        ModelNode addCacheOp = getCacheAddOperation("maximal2", LocalCacheResourceDefinition.WILDCARD_PATH.getKey(), "fred");

        // add a cache container
        ModelNode result = servicesA.executeOperation(addContainerOp);
        Assert.assertEquals(result.get(FAILURE_DESCRIPTION).asString(), SUCCESS, result.get(OUTCOME).asString());

        // add a local cache
        result = servicesA.executeOperation(addCacheOp);
        Assert.assertEquals(result.get(FAILURE_DESCRIPTION).asString(), SUCCESS, result.get(OUTCOME).asString());

        // remove the cache container
        result = servicesA.executeOperation(removeContainerOp);
        Assert.assertEquals(result.get(FAILURE_DESCRIPTION).asString(), SUCCESS, result.get(OUTCOME).asString());

        // remove the cache container again
        result = servicesA.executeOperation(removeContainerOp);
        Assert.assertEquals(result.toString(), FAILED, result.get(OUTCOME).asString());
    }

    @Ignore("This requires NORMAL mode, but we lack the requisite runtime capabilities")
    @Test
    @BMRule(name="Test remove rollback operation",
            targetClass="org.jboss.as.clustering.infinispan.subsystem.CacheContainerServiceHandler",
            targetMethod="removeServices",
            targetLocation="AT ENTRY",
            action="$1.setRollbackOnly()")
    public void testCacheContainerRemoveRollback() throws Exception {
        // Parse and install the XML into the controller
        String subsystemXml = getSubsystemXml() ;
        KernelServices servicesA = this.createKernelServicesBuilder().setSubsystemXml(subsystemXml).build();

        ModelNode addContainerOp = getCacheContainerAddOperation("maximal2");
        ModelNode removeContainerOp = getCacheContainerRemoveOperation("maximal2");
        ModelNode addCacheOp = getCacheAddOperation("maximal2", LocalCacheResourceDefinition.WILDCARD_PATH.getKey(), "fred");

        // add a cache container
        ModelNode result = servicesA.executeOperation(addContainerOp);
        Assert.assertEquals(result.get(FAILURE_DESCRIPTION).asString(), SUCCESS, result.get(OUTCOME).asString());

        // add a local cache
        result = servicesA.executeOperation(addCacheOp);
        Assert.assertEquals(result.get(FAILURE_DESCRIPTION).asString(), SUCCESS, result.get(OUTCOME).asString());

        // remove the cache container
        // the remove has OperationContext.setRollbackOnly() injected
        // and so is expected to fail
        result = servicesA.executeOperation(removeContainerOp);
        Assert.assertEquals(result.toString(), FAILED, result.get(OUTCOME).asString());

        // need to check that all services are correctly re-installed
        ServiceName containerServiceName = CacheContainerResourceDefinition.Capability.CONTAINER.getServiceName(getCacheContainerAddress("maximal2"));

        ServiceName cacheConfigurationServiceName = CacheResourceDefinition.Capability.CONFIGURATION.getServiceName(getCacheAddress("maximal2", LocalCacheResourceDefinition.WILDCARD_PATH.getKey(), "fred"));
        ServiceName cacheServiceName = CacheResourceDefinition.Capability.CACHE.getServiceName(getCacheAddress("maximal2", LocalCacheResourceDefinition.WILDCARD_PATH.getKey(), "fred"));

        Assert.assertNotNull("cache container service not installed", servicesA.getContainer().getService(containerServiceName));
        Assert.assertNotNull("cache configuration service not installed", servicesA.getContainer().getService(cacheConfigurationServiceName));
        Assert.assertNotNull("cache service not installed", servicesA.getContainer().getService(cacheServiceName));
    }

    @Test
    public void testLocalCacheAddRemoveAddSequence() throws Exception {

        // Parse and install the XML into the controller
        String subsystemXml = getSubsystemXml() ;
        KernelServices servicesA = this.createKernelServicesBuilder().setSubsystemXml(subsystemXml).build();

        ModelNode addOp = getCacheAddOperation("maximal", LocalCacheResourceDefinition.WILDCARD_PATH.getKey(), "fred");
        ModelNode removeOp = getCacheRemoveOperation("maximal", LocalCacheResourceDefinition.WILDCARD_PATH.getKey(), "fred");

        // add a local cache
        ModelNode result = servicesA.executeOperation(addOp);
        Assert.assertEquals(result.get(FAILURE_DESCRIPTION).asString(), SUCCESS, result.get(OUTCOME).asString());

        // remove the local cache
        result = servicesA.executeOperation(removeOp);
        Assert.assertEquals(result.get(FAILURE_DESCRIPTION).asString(), SUCCESS, result.get(OUTCOME).asString());

        // add the same local cache
        result = servicesA.executeOperation(addOp);
        Assert.assertEquals(result.get(FAILURE_DESCRIPTION).asString(), SUCCESS, result.get(OUTCOME).asString());
    }

    @Test
    public void testLocalCacheRemoveRemoveSequence() throws Exception {

        // Parse and install the XML into the controller
        String subsystemXml = getSubsystemXml() ;
        KernelServices servicesA = this.createKernelServicesBuilder().setSubsystemXml(subsystemXml).build();

        ModelNode addOp = getCacheAddOperation("maximal", LocalCacheResourceDefinition.WILDCARD_PATH.getKey(), "fred");
        ModelNode removeOp = getCacheRemoveOperation("maximal", LocalCacheResourceDefinition.WILDCARD_PATH.getKey(), "fred");

        // add a local cache
        ModelNode result = servicesA.executeOperation(addOp);
        Assert.assertEquals(result.get(FAILURE_DESCRIPTION).asString(), SUCCESS, result.get(OUTCOME).asString());

        // remove the local cache
        result = servicesA.executeOperation(removeOp);
        Assert.assertEquals(result.get(FAILURE_DESCRIPTION).asString(), SUCCESS, result.get(OUTCOME).asString());

        // remove the same local cache
        result = servicesA.executeOperation(removeOp);
        Assert.assertEquals(result.toString(), FAILED, result.get(OUTCOME).asString());
    }
}
