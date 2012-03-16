package org.jboss.as.clustering.infinispan.subsystem;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;

import static org.jboss.as.clustering.infinispan.subsystem.ModelKeys.DEFAULT_CACHE;
import static org.jboss.as.clustering.infinispan.subsystem.ModelKeys.JNDI_NAME;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.net.URL;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.subsystem.test.AbstractSubsystemTest;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Test;

/**
* Test case for testing sequences of management operations.
*
* @author Richard Achmatowicz (c) 2011 Red Hat Inc.
*/
public class OperationSequencesTestCase extends AbstractSubsystemTest {

    static final String SUBSYSTEM_XML_FILE = "subsystem-infinispan_1_2.xml" ;

    public OperationSequencesTestCase() {
        super(InfinispanExtension.SUBSYSTEM_NAME, new InfinispanExtension());
    }

    @Test
    public void testCacheContainerAddRemoveAddSequence() throws Exception {

        // Parse and install the XML into the controller
        String subsystemXml = getSubsystemXml() ;
        KernelServices servicesA = super.installInController(subsystemXml) ;

        ModelNode addContainerOp = getCacheContainerAddOperation("maximal2");
        ModelNode removeContainerOp = getCacheContainerRemoveOperation("maximal2");
        ModelNode addCacheOp = getLocalCacheAddOperation("maximal2", "fred");
        ModelNode removeCacheOp = getLocalCacheRemoveOperation("maximal2", "fred");

        // add a cache container
        ModelNode result = servicesA.executeOperation(addContainerOp);
        Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());

        // add a local cache
        result = servicesA.executeOperation(addCacheOp);
        Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());

        // remove the cache container
        result = servicesA.executeOperation(removeContainerOp);
        Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());

        // add the same cache container
        result = servicesA.executeOperation(addContainerOp);
        Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());

        // add the same local cache
        result = servicesA.executeOperation(addCacheOp);
        Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());
    }

    @Test
    public void testCacheContainerRemoveRemoveSequence() throws Exception {

        // Parse and install the XML into the controller
        String subsystemXml = getSubsystemXml() ;
        KernelServices servicesA = super.installInController(subsystemXml) ;

        ModelNode addContainerOp = getCacheContainerAddOperation("maximal2");
        ModelNode removeContainerOp = getCacheContainerRemoveOperation("maximal2");
        ModelNode addCacheOp = getLocalCacheAddOperation("maximal2", "fred");
        ModelNode removeCacheOp = getLocalCacheRemoveOperation("maximal2", "fred");

        // add a cache container
        ModelNode result = servicesA.executeOperation(addContainerOp);
        Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());

        // add a local cache
        result = servicesA.executeOperation(addCacheOp);
        Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());

        // remove the cache container
        result = servicesA.executeOperation(removeContainerOp);
        Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());

        // remove the cache container again
        result = servicesA.executeOperation(removeContainerOp);
        Assert.assertEquals(FAILED, result.get(OUTCOME).asString());
    }

    @Test
    public void testLocalCacheAddRemoveAddSequence() throws Exception {

        // Parse and install the XML into the controller
        String subsystemXml = getSubsystemXml() ;
        KernelServices servicesA = super.installInController(subsystemXml) ;

        ModelNode addOp = getLocalCacheAddOperation("maximal", "fred");
        ModelNode removeOp = getLocalCacheRemoveOperation("maximal", "fred");

        // add a local cache
        ModelNode result = servicesA.executeOperation(addOp);
        Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());

        // remove the local cache
        result = servicesA.executeOperation(removeOp);
        Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());

        // add the same local cache
        result = servicesA.executeOperation(addOp);
        Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());
    }

    @Test
    public void testLocalCacheRemoveRemoveSequence() throws Exception {

        // Parse and install the XML into the controller
        String subsystemXml = getSubsystemXml() ;
        KernelServices servicesA = super.installInController(subsystemXml) ;

        ModelNode addOp = getLocalCacheAddOperation("maximal", "fred");
        ModelNode removeOp = getLocalCacheRemoveOperation("maximal", "fred");

        // add a local cache
        ModelNode result = servicesA.executeOperation(addOp);
        Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());

        // remove the local cache
        result = servicesA.executeOperation(removeOp);
        Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());

        // remove the same local cache
        result = servicesA.executeOperation(removeOp);
        Assert.assertEquals(FAILED, result.get(OUTCOME).asString());
    }


    private ModelNode getCacheContainerAddOperation(String containerName) {
        // create the address of the cache
        PathAddress containerAddr = getCacheContainerAddress(containerName);
        ModelNode addOp = new ModelNode() ;
        addOp.get(OP).set(ADD);
        addOp.get(OP_ADDR).set(containerAddr.toModelNode());
        // required attributes
        addOp.get(DEFAULT_CACHE).set("default");

        return addOp ;
    }

    private ModelNode getCacheContainerRemoveOperation(String containerName) {
        // create the address of the cache
        PathAddress containerAddr = getCacheContainerAddress(containerName);
        ModelNode removeOp = new ModelNode() ;
        removeOp.get(OP).set(REMOVE);
        removeOp.get(OP_ADDR).set(containerAddr.toModelNode());

        return removeOp ;
    }


    private ModelNode getLocalCacheAddOperation(String containerName, String cacheName) {
        // create the address of the cache
        PathAddress localCacheAddr = getCacheAddress(containerName, cacheName, ModelKeys.LOCAL_CACHE);
        ModelNode addOp = new ModelNode() ;
        addOp.get(OP).set(ADD);
        addOp.get(OP_ADDR).set(localCacheAddr.toModelNode());
        // required attributes
        addOp.get(JNDI_NAME).set("java:/fred/was/here");

        return addOp ;
    }

    private ModelNode getLocalCacheRemoveOperation(String containerName, String cacheName) {
        // create the address of the cache
        PathAddress localCacheAddr = getCacheAddress(containerName, cacheName, ModelKeys.LOCAL_CACHE);
        ModelNode removeOp = new ModelNode() ;
        removeOp.get(OP).set(REMOVE);
        removeOp.get(OP_ADDR).set(localCacheAddr.toModelNode());

        return removeOp ;
    }

    private PathAddress getCacheContainerAddress(String containerName) {
        // create the address of the cache
        PathAddress containerAddr = PathAddress.pathAddress(
                PathElement.pathElement(SUBSYSTEM, InfinispanExtension.SUBSYSTEM_NAME),
                PathElement.pathElement("cache-container",containerName));
        return containerAddr ;
    }


    private PathAddress getCacheAddress(String containerName, String cacheName, String cacheType) {
        // create the address of the cache
        PathAddress cacheAddr = PathAddress.pathAddress(
                PathElement.pathElement(SUBSYSTEM, InfinispanExtension.SUBSYSTEM_NAME),
                PathElement.pathElement("cache-container",containerName),
                PathElement.pathElement(cacheType, cacheName));
        return cacheAddr ;
    }


    private String getSubsystemXml() throws IOException {
        URL url = Thread.currentThread().getContextClassLoader().getResource(SUBSYSTEM_XML_FILE);
        if (url == null) {
            throw new IllegalStateException(String.format("Failed to locate %s", SUBSYSTEM_XML_FILE));
        }
        try {
            BufferedReader reader = new BufferedReader(new FileReader(new File(url.toURI())));
            StringWriter writer = new StringWriter();
            try {
                String line = reader.readLine();
                while (line != null) {
                    writer.write(line);
                    line = reader.readLine();
                }
            } finally {
                reader.close();
            }
            return writer.toString();
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }
}