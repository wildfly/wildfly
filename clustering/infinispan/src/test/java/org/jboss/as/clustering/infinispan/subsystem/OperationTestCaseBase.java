package org.jboss.as.clustering.infinispan.subsystem;

import static org.jboss.as.clustering.infinispan.subsystem.ModelKeys.DEFAULT_CACHE;
import static org.jboss.as.clustering.infinispan.subsystem.ModelKeys.JNDI_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.net.URL;

import org.jboss.as.clustering.infinispan.InfinispanMessages;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
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

    protected static ModelNode getCacheContainerAddOperation(String containerName) {
        // create the address of the cache
        PathAddress containerAddr = getCacheContainerAddress(containerName);
        ModelNode addOp = new ModelNode() ;
        addOp.get(OP).set(ADD);
        addOp.get(OP_ADDR).set(containerAddr.toModelNode());
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
        PathAddress transportAddress = getCacheContainerAddress(containerName);
        ModelNode writeOp = new ModelNode() ;
        writeOp.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
        writeOp.get(OP_ADDR).set(transportAddress.toModelNode());
        // required attributes
        writeOp.get(NAME).set(name);
        writeOp.get(VALUE).set(value);
        return writeOp ;
    }

    protected static ModelNode getCacheContainerRemoveOperation(String containerName) {
        // create the address of the cache
        PathAddress containerAddr = getCacheContainerAddress(containerName);
        ModelNode removeOp = new ModelNode() ;
        removeOp.get(OP).set(REMOVE);
        removeOp.get(OP_ADDR).set(containerAddr.toModelNode());
        return removeOp ;
    }


    protected static ModelNode getCacheAddOperation(String containerName, String cacheName, String cacheType) {
        // create the address of the cache
        PathAddress localCacheAddr = getCacheAddress(containerName, cacheName, cacheType);
        ModelNode addOp = new ModelNode() ;
        addOp.get(OP).set(ADD);
        addOp.get(OP_ADDR).set(localCacheAddr.toModelNode());
        // required attributes
        addOp.get(JNDI_NAME).set("java:/fred/was/here");

        return addOp ;
    }

    protected static ModelNode getCacheReadOperation(String containerName, String cacheName, String cacheType, String name) {
        // create the address of the subsystem
        PathAddress transportAddress = getCacheAddress(containerName, cacheName, cacheType);
        ModelNode readOp = new ModelNode() ;
        readOp.get(OP).set(READ_ATTRIBUTE_OPERATION);
        readOp.get(OP_ADDR).set(transportAddress.toModelNode());
        // required attributes
        readOp.get(NAME).set(name);
        return readOp ;
    }

    protected static ModelNode getCacheWriteOperation(String containerName, String cacheName, String cacheType, String name, String value) {
        // create the address of the subsystem
        PathAddress transportAddress = getCacheAddress(containerName, cacheName, cacheType);
        ModelNode writeOp = new ModelNode() ;
        writeOp.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
        writeOp.get(OP_ADDR).set(transportAddress.toModelNode());
        // required attributes
        writeOp.get(NAME).set(name);
        writeOp.get(VALUE).set(value);
        return writeOp ;
    }

    protected static ModelNode getCacheRemoveOperation(String containerName, String cacheName, String cacheType) {
        // create the address of the cache
        PathAddress localCacheAddr = getCacheAddress(containerName, cacheName, cacheType);
        ModelNode removeOp = new ModelNode() ;
        removeOp.get(OP).set(REMOVE);
        removeOp.get(OP_ADDR).set(localCacheAddr.toModelNode());

        return removeOp ;
    }

    protected static PathAddress getCacheContainerAddress(String containerName) {
        // create the address of the cache
        PathAddress containerAddr = PathAddress.pathAddress(
                PathElement.pathElement(SUBSYSTEM, InfinispanExtension.SUBSYSTEM_NAME),
                PathElement.pathElement("cache-container",containerName));
        return containerAddr ;
    }

    protected static PathAddress getCacheAddress(String containerName, String cacheName, String cacheType) {
        // create the address of the cache
        PathAddress cacheAddr = PathAddress.pathAddress(
                PathElement.pathElement(SUBSYSTEM, InfinispanExtension.SUBSYSTEM_NAME),
                PathElement.pathElement("cache-container",containerName),
                PathElement.pathElement(cacheType, cacheName));
        return cacheAddr ;
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