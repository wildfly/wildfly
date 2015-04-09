package org.jboss.as.clustering.infinispan.subsystem;

import static org.jboss.as.clustering.infinispan.subsystem.ModelKeys.DEFAULT_CACHE;
import static org.jboss.as.clustering.infinispan.subsystem.ModelKeys.JNDI_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;

import java.io.IOException;

import org.jboss.as.clustering.controller.Operations;
import org.jboss.as.clustering.jgroups.subsystem.JGroupsSubsystemInitialization;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.subsystem.test.AbstractSubsystemTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServicesBuilder;
import org.jboss.dmr.ModelNode;

/**
* Base test case for testing management operations.
*
* @author Richard Achmatowicz (c) 2011 Red Hat Inc.
*/

public class OperationTestCaseBase extends AbstractSubsystemTest {

    static final String SUBSYSTEM_XML_FILE = InfinispanSchema.CURRENT.format("subsystem-infinispan-%d_%d.xml");

    public OperationTestCaseBase() {
        super(InfinispanExtension.SUBSYSTEM_NAME, new InfinispanExtension());
    }

    KernelServicesBuilder createKernelServicesBuilder() {
        return this.createKernelServicesBuilder(this.createAdditionalInitialization());
    }

    AdditionalInitialization createAdditionalInitialization() {
        return new JGroupsSubsystemInitialization();
    }

    // cache container access
    protected static ModelNode getCacheContainerAddOperation(String containerName) {
        // create the address of the cache
        PathAddress containerAddr = getCacheContainerAddress(containerName);
        ModelNode operation = Util.createAddOperation(containerAddr);
        // required attributes
        operation.get(DEFAULT_CACHE).set("default");
        return operation;
    }

    protected static ModelNode getCacheContainerReadOperation(String containerName, String name) {
        // create the address of the subsystem
        return Operations.createReadAttributeOperation(getCacheContainerAddress(containerName), name);
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
        ModelNode operation = Util.createAddOperation(cacheAddr);
        // required attributes
        operation.get(JNDI_NAME).set("java:/fred/was/here");
        return operation;
    }

    protected static ModelNode getCacheReadOperation(String containerName, String cacheType, String cacheName, String name) {
        // create the address of the subsystem
        return Operations.createReadAttributeOperation(getCacheAddress(containerName, cacheType, cacheName), name);
    }

    protected static ModelNode getCacheWriteOperation(String containerName, String cacheType, String cacheName, String name, String value) {
        return Util.getWriteAttributeOperation(getCacheAddress(containerName, cacheType, cacheName), name, new ModelNode().set(value));
    }

    protected static ModelNode getCacheRemoveOperation(String containerName, String cacheType, String cacheName) {
        return Util.createRemoveOperation(getCacheAddress(containerName, cacheType, cacheName));
    }

    // cache store access
    protected static ModelNode getCacheStoreReadOperation(String containerName, String cacheType, String cacheName, String name) {
        // create the address of the subsystem
        return Operations.createReadAttributeOperation(getCustomCacheStoreAddress(containerName, cacheType, cacheName), name);
    }

    protected static ModelNode getCacheStoreWriteOperation(String containerName, String cacheName, String cacheType, String name, String value) {
        return Util.getWriteAttributeOperation(getCustomCacheStoreAddress(containerName,  cacheType, cacheName), name, new ModelNode().set(value));
    }

    protected static ModelNode getMixedKeyedJDBCCacheStoreReadOperation(String containerName, String cacheType, String cacheName, String name) {
        // create the address of the subsystem
        return Operations.createReadAttributeOperation(getMixedKeyedJDBCCacheStoreAddress(containerName, cacheType, cacheName), name);
    }

    protected static ModelNode getMixedKeyedJDBCCacheStoreWriteOperation(String containerName, String cacheType, String cacheName, String name, String value) {
        PathAddress cacheStoreAddress = getMixedKeyedJDBCCacheStoreAddress(containerName, cacheType, cacheName);
        return Util.getWriteAttributeOperation(cacheStoreAddress, name, new ModelNode().set(value));
    }

    protected static ModelNode getMixedKeyedJDBCCacheStoreWriteOperation(String containerName, String cacheType, String cacheName, String name, ModelNode value) {
        return Util.getWriteAttributeOperation(getMixedKeyedJDBCCacheStoreAddress(containerName, cacheType, cacheName), name, value);
    }

    //cache store property access
    protected static ModelNode getCacheStorePropertyAddOperation(String containerName, String cacheName, String cacheType, String propertyName, String value) {
        ModelNode operation = Util.createAddOperation(getCacheStorePropertyAddress(containerName,  cacheType, cacheName, propertyName));
        // required attributes
        operation.get(VALUE).set(value);
        return operation;
    }

    protected static ModelNode getCacheStorePropertyWriteOperation(String containerName, String cacheName, String cacheType, String propertyName, String value) {
        return Util.getWriteAttributeOperation(getCacheStorePropertyAddress(containerName, cacheType, cacheName, propertyName), VALUE, new ModelNode().set(value));
    }

    // address generation
    protected static PathAddress getCacheStorePropertyAddress(String containerName, String cacheType, String cacheName, String propertyName) {
        return getCustomCacheStoreAddress(containerName, cacheType, cacheName).append(StorePropertyResourceDefinition.pathElement(propertyName));
    }

    protected static PathAddress getMixedKeyedJDBCCacheStoreAddress(String containerName, String cacheType, String cacheName) {
        return getCacheAddress(containerName, cacheType, cacheName).append(MixedKeyedJDBCStoreResourceDefinition.PATH);
    }

    protected static PathAddress getBinaryKeyedJDBCCacheStoreAddress(String containerName, String cacheType, String cacheName) {
        return getCacheAddress(containerName, cacheType, cacheName).append(BinaryKeyedJDBCStoreResourceDefinition.PATH);
    }

    protected static PathAddress getStringKeyedJDBCCacheStoreAddress(String containerName, String cacheType, String cacheName) {
        return getCacheAddress(containerName, cacheType, cacheName).append(StringKeyedJDBCStoreResourceDefinition.PATH);
    }

    protected static PathAddress getRemoteCacheStoreAddress(String containerName, String cacheType, String cacheName) {
        return getCacheAddress(containerName, cacheType, cacheName).append(RemoteStoreResourceDefinition.PATH);
    }

    protected static PathAddress getFileCacheStoreAddress(String containerName, String cacheType, String cacheName) {
        return getCacheAddress(containerName, cacheType, cacheName).append(FileStoreResourceDefinition.PATH);
    }

    protected static PathAddress getCustomCacheStoreAddress(String containerName, String cacheType, String cacheName) {
        return getCacheAddress(containerName, cacheType, cacheName).append(CustomStoreResourceDefinition.PATH);
    }

    protected static PathAddress getCacheContainerAddress(String containerName) {
        return PathAddress.pathAddress(InfinispanSubsystemResourceDefinition.PATH).append(CacheContainerResourceDefinition.pathElement(containerName));
    }

    protected static PathAddress getCacheAddress(String containerName, String cacheType, String cacheName) {
        return getCacheContainerAddress(containerName).append(cacheType, cacheName);
    }

    protected String getSubsystemXml() throws IOException {
        return readResource(SUBSYSTEM_XML_FILE) ;
    }
}