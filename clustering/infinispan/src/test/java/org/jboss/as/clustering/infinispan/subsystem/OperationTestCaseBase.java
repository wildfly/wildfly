package org.jboss.as.clustering.infinispan.subsystem;

import static org.jboss.as.clustering.infinispan.subsystem.ModelKeys.DEFAULT_CACHE;
import static org.jboss.as.clustering.infinispan.subsystem.ModelKeys.JNDI_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.net.URL;

import org.jboss.as.clustering.infinispan.InfinispanMessages;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.subsystem.test.AbstractSubsystemTest;
import org.jboss.dmr.ModelNode;

/**
* Base test case for testing management operations.
*
* @author Richard Achmatowicz (c) 2011 Red Hat Inc.
*/

public class OperationTestCaseBase extends AbstractSubsystemTest {

    static final String SUBSYSTEM_XML_FILE = "subsystem-infinispan-test.xml" ;

    public OperationTestCaseBase() {
        super(InfinispanExtension.SUBSYSTEM_NAME, new InfinispanExtension());
    }

    // cache container access
    protected static ModelNode getCacheContainerAddOperation(String containerName) {
        // create the address of the cache
        PathAddress containerAddr = getCacheContainerAddress(containerName);
        ModelNode addOp = Util.createAddOperation(containerAddr);
        // required attributes
        addOp.get(DEFAULT_CACHE).set("default");
        return addOp ;
    }

    protected static ModelNode getCacheContainerReadOperation(String containerName, String name) {
        // create the address of the subsystem
        PathAddress transportAddress = getCacheContainerAddress(containerName);
        ModelNode readOp = new ModelNode() ;
        readOp.get(OP).set(READ_ATTRIBUTE_OPERATION);
        readOp.get(OP_ADDR).set(transportAddress.toModelNode());
        // required attributes
        readOp.get(NAME).set(name);
        return readOp ;
    }

    protected static ModelNode getCacheContainerWriteOperation(String containerName, String name, String value) {
        // create the address of the subsystem
        PathAddress cacheAddress = getCacheContainerAddress(containerName);
        return Util.getWriteAttributeOperation(cacheAddress, name, new ModelNode().set(value));
    }

    protected static ModelNode getCacheContainerRemoveOperation(String containerName) {
        // create the address of the cache
        PathAddress containerAddr = getCacheContainerAddress(containerName);
        return Util.createRemoveOperation(containerAddr);
    }

    // cache access
    protected static ModelNode getCacheAddOperation(String containerName, String cacheType, String cacheName) {
        // create the address of the cache
        PathAddress cacheAddr = getCacheAddress(containerName, cacheType, cacheName);
        ModelNode addOp = Util.createAddOperation(cacheAddr);
        // required attributes
        addOp.get(JNDI_NAME).set("java:/fred/was/here");
        return addOp ;
    }

    protected static ModelNode getCacheReadOperation(String containerName, String cacheType, String cacheName, String name) {
        // create the address of the subsystem
        PathAddress cacheAddress = getCacheAddress(containerName, cacheType, cacheName);
        ModelNode readOp = new ModelNode() ;
        readOp.get(OP).set(READ_ATTRIBUTE_OPERATION);
        readOp.get(OP_ADDR).set(cacheAddress.toModelNode());
        // required attributes
        readOp.get(NAME).set(name);
        return readOp ;
    }

    protected static ModelNode getCacheWriteOperation(String containerName, String cacheType, String cacheName, String name, String value) {
        PathAddress cacheAddress = getCacheAddress(containerName, cacheType, cacheName);
        return Util.getWriteAttributeOperation(cacheAddress, name, new ModelNode().set(value));
    }

    protected static ModelNode getCacheRemoveOperation(String containerName, String cacheType, String cacheName) {
        PathAddress cacheAddr = getCacheAddress(containerName, cacheType, cacheName);
        return Util.createRemoveOperation(cacheAddr) ;
    }

    // cache store access
    protected static ModelNode getCacheStoreReadOperation(String containerName, String cacheType, String cacheName, String name) {
        // create the address of the subsystem
        PathAddress cacheAddress = getCacheAddress(containerName, cacheType, cacheName);
        ModelNode readOp = new ModelNode() ;
        readOp.get(OP).set(READ_ATTRIBUTE_OPERATION);
        readOp.get(OP_ADDR).set(cacheAddress.toModelNode());
        // required attributes
        readOp.get(NAME).set(name);
        return readOp ;
    }

    protected static ModelNode getCacheStoreWriteOperation(String containerName, String cacheName, String cacheType, String name, String value) {
        PathAddress cacheStoreAddress = getCacheStoreAddress(containerName,  cacheType, cacheName);
        return Util.getWriteAttributeOperation(cacheStoreAddress, name, new ModelNode().set(value));
    }

    protected static ModelNode getMixedKeyedJDBCCacheStoreReadOperation(String containerName, String cacheType, String cacheName, String name) {
        // create the address of the subsystem
        PathAddress cacheAddress = getMixedKeyedJDBCCacheStoreAddress(containerName, cacheType, cacheName);
        ModelNode readOp = new ModelNode() ;
        readOp.get(OP).set(READ_ATTRIBUTE_OPERATION);
        readOp.get(OP_ADDR).set(cacheAddress.toModelNode());
        // required attributes
        readOp.get(NAME).set(name);
        return readOp ;
    }

    protected static ModelNode getMixedKeyedJDBCCacheStoreWriteOperation(String containerName, String cacheType, String cacheName, String name, String value) {
        PathAddress cacheStoreAddress = getMixedKeyedJDBCCacheStoreAddress(containerName, cacheType, cacheName);
        return Util.getWriteAttributeOperation(cacheStoreAddress, name, new ModelNode().set(value));
    }


    // address generation
    protected static PathAddress getMixedKeyedJDBCCacheStoreAddress(String containerName, String cacheType, String cacheName) {
        return getCacheAddress(containerName, cacheType, cacheName).append(ModelKeys.MIXED_KEYED_JDBC_STORE, ModelKeys.MIXED_KEYED_JDBC_STORE_NAME);
    }

    protected static PathAddress getBinaryKeyedJDBCCacheStoreAddress(String containerName, String cacheType, String cacheName) {
        return getCacheAddress(containerName, cacheType, cacheName).append(ModelKeys.BINARY_KEYED_JDBC_STORE, ModelKeys.BINARY_KEYED_JDBC_STORE_NAME);
    }

    protected static PathAddress getStringKeyedJDBCCacheStoreAddress(String containerName, String cacheType, String cacheName) {
        return getCacheAddress(containerName, cacheType, cacheName).append(ModelKeys.STRING_KEYED_JDBC_STORE, ModelKeys.STRING_KEYED_JDBC_STORE_NAME);
    }

    protected static PathAddress getRemoteCacheStoreAddress(String containerName, String cacheType, String cacheName) {
        return getCacheAddress(containerName, cacheType, cacheName).append(ModelKeys.REMOTE_STORE, ModelKeys.REMOTE_STORE_NAME);
    }

    protected static PathAddress getFileCacheStoreAddress(String containerName, String cacheType, String cacheName) {
        return getCacheAddress(containerName, cacheType, cacheName).append(ModelKeys.FILE_STORE, ModelKeys.FILE_STORE_NAME);
    }

    protected static PathAddress getCacheStoreAddress(String containerName, String cacheType, String cacheName) {
        return getCacheAddress(containerName, cacheType, cacheName).append(ModelKeys.STORE, ModelKeys.STORE_NAME);
    }

    protected static PathAddress getCacheContainerAddress(String containerName) {
        return PathAddress.pathAddress(InfinispanExtension.SUBSYSTEM_PATH).append(ModelKeys.CACHE_CONTAINER, containerName);
    }

    protected static PathAddress getCacheAddress(String containerName, String cacheType, String cacheName) {
        return getCacheContainerAddress(containerName).append(cacheType, cacheName);
    }

    protected String getSubsystemXml() throws IOException {
        return getSubsystemXml(SUBSYSTEM_XML_FILE) ;
    }

    protected String getSubsystemXml(String xml_file) throws IOException {
        URL url = Thread.currentThread().getContextClassLoader().getResource(xml_file);
        if (url == null) {
            throw new IllegalStateException(InfinispanMessages.MESSAGES.notFound(xml_file));
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