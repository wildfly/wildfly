/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.jboss.as.clustering.controller.CommonServiceDescriptor;
import org.jboss.as.clustering.subsystem.AdditionalInitialization;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.network.OutboundSocketBinding;
import org.jboss.as.subsystem.test.AbstractSubsystemTest;
import org.jboss.as.subsystem.test.KernelServicesBuilder;
import org.jboss.dmr.ModelNode;
import org.wildfly.clustering.jgroups.spi.ForkChannelFactory;

/**
 * Base test case for testing management operations.
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 * @author Radoslav Husar
 */
public class OperationTestCaseBase extends AbstractSubsystemTest {

    static final String SUBSYSTEM_XML_FILE = String.format("infinispan-%d.%d.xml", InfinispanSubsystemSchema.CURRENT.getVersion().major(), InfinispanSubsystemSchema.CURRENT.getVersion().minor());

    public OperationTestCaseBase() {
        super(InfinispanSubsystemResourceDefinitionRegistrar.REGISTRATION.getName(), new InfinispanExtension());
    }

    KernelServicesBuilder createKernelServicesBuilder() {
        return this.createKernelServicesBuilder(this.createAdditionalInitialization());
    }

    AdditionalInitialization createAdditionalInitialization() {
        return new JGroupsSubsystemInitialization()
                .require(ForkChannelFactory.DEFAULT_SERVICE_DESCRIPTOR)
                .require(ForkChannelFactory.SERVICE_DESCRIPTOR, "maximal-channel")
                .require(OutboundSocketBinding.SERVICE_DESCRIPTOR, List.of("hotrod-server-1", "hotrod-server-2"))
                .require(CommonServiceDescriptor.DATA_SOURCE, List.of("ExampleDS", "new-datasource"))
                ;
    }

    // cache container access
    protected static ModelNode getCacheContainerAddOperation(String containerName) {
        PathAddress address = getCacheContainerAddress(containerName);
        return Util.createAddOperation(address, Map.of(CacheContainerResourceDefinitionRegistrar.DEFAULT_CACHE.getName(), new ModelNode("default")));
    }

    protected static ModelNode getCacheContainerReadOperation(String containerName, AttributeDefinition attribute) {
        return Util.getReadAttributeOperation(getCacheContainerAddress(containerName), attribute.getName());
    }

    protected static ModelNode getCacheContainerWriteOperation(String containerName, AttributeDefinition attribute, String value) {
        PathAddress cacheAddress = getCacheContainerAddress(containerName);
        return Util.getWriteAttributeOperation(cacheAddress, attribute.getName(), new ModelNode(value));
    }

    protected static ModelNode getCacheContainerRemoveOperation(String containerName) {
        PathAddress containerAddr = getCacheContainerAddress(containerName);
        return Util.createRemoveOperation(containerAddr);
    }

    // cache access
    protected static ModelNode getCacheAddOperation(String containerName, PathElement path) {
        PathAddress address = getCacheAddress(containerName, path);
        return Util.createAddOperation(address, Map.of());
    }

    protected static ModelNode getCacheReadOperation(String containerName, PathElement cachePath, AttributeDefinition attribute) {
        return Util.getReadAttributeOperation(getCacheAddress(containerName, cachePath), attribute.getName());
    }

    protected static ModelNode getCacheWriteOperation(String containerName, PathElement cachePath, AttributeDefinition attribute, String value) {
        return Util.getWriteAttributeOperation(getCacheAddress(containerName, cachePath), attribute.getName(), new ModelNode(value));
    }

    protected static ModelNode getCacheRemoveOperation(String containerName, PathElement cachePath) {
        return Util.createRemoveOperation(getCacheAddress(containerName, cachePath));
    }

    // cache store access
    protected static ModelNode getCacheStoreReadOperation(String containerName, PathElement cachePath, AttributeDefinition attribute) {
        return Util.getReadAttributeOperation(getCustomCacheStoreAddress(containerName, cachePath), attribute.getName());
    }

    protected static ModelNode getCacheStoreWriteOperation(String containerName, PathElement cachePath, AttributeDefinition attribute, String value) {
        return Util.getWriteAttributeOperation(getCustomCacheStoreAddress(containerName, cachePath), attribute.getName(), new ModelNode(value));
    }

    protected static ModelNode getJDBCCacheStoreReadOperation(String containerName, PathElement cachePath, AttributeDefinition attribute) {
        return Util.getReadAttributeOperation(getJDBCCacheStoreAddress(containerName, cachePath), attribute.getName());
    }

    protected static ModelNode getJDBCCacheStoreWriteOperation(String containerName, PathElement cachePath, AttributeDefinition attribute, String value) {
        return Util.getWriteAttributeOperation(getJDBCCacheStoreAddress(containerName, cachePath), attribute.getName(), new ModelNode(value));
    }

    // cache store property access
    protected static ModelNode getCacheStoreGetPropertyOperation(PathAddress cacheStoreAddress, String propertyName) {
        return Util.createMapGetOperation(cacheStoreAddress, StoreResourceDefinitionRegistrar.PROPERTIES.getName(), propertyName);
    }

    protected static ModelNode getCacheStorePutPropertyOperation(PathAddress cacheStoreAddress, String propertyName, String propertyValue) {
        return Util.createMapPutOperation(cacheStoreAddress, StoreResourceDefinitionRegistrar.PROPERTIES.getName(), propertyName, propertyValue);
    }

    protected static ModelNode getCacheStoreRemovePropertyOperation(PathAddress cacheStoreAddress, String propertyName) {
        return Util.createMapRemoveOperation(cacheStoreAddress, StoreResourceDefinitionRegistrar.PROPERTIES.getName(), propertyName);
    }

    protected static ModelNode getCacheStoreClearPropertiesOperation(PathAddress cacheStoreAddress) {
        return Util.createMapClearOperation(cacheStoreAddress, StoreResourceDefinitionRegistrar.PROPERTIES.getName());
    }

    protected static ModelNode getCacheStoreUndefinePropertiesOperation(PathAddress cacheStoreAddress) {
        return Util.getUndefineAttributeOperation(cacheStoreAddress, StoreResourceDefinitionRegistrar.PROPERTIES.getName());
    }

    protected static PathAddress getJDBCCacheStoreAddress(String containerName, PathElement cachePath) {
        return getCacheAddress(containerName, cachePath).append(StoreResourceRegistration.JDBC.getPathElement());
    }

    protected static PathAddress getRemoteCacheStoreAddress(String containerName, PathElement cachePath) {
        return getCacheAddress(containerName, cachePath).append(StoreResourceRegistration.REMOTE.getPathElement());
    }

    protected static PathAddress getFileCacheStoreAddress(String containerName, PathElement cachePath) {
        return getCacheAddress(containerName, cachePath).append(StoreResourceRegistration.FILE.getPathElement());
    }

    protected static PathAddress getCustomCacheStoreAddress(String containerName, PathElement cachePath) {
        return getCacheAddress(containerName, cachePath).append(StoreResourceRegistration.CUSTOM.getPathElement());
    }

    protected static PathAddress getCacheContainerAddress(String containerName) {
        return PathAddress.pathAddress(InfinispanSubsystemResourceDefinitionRegistrar.REGISTRATION.getPathElement()).append(PathElement.pathElement(CacheContainerResourceDefinitionRegistrar.REGISTRATION.getPathElement().getKey(), containerName));
    }

    protected static PathAddress getCacheAddress(String containerName, PathElement cachePath) {
        return getCacheContainerAddress(containerName).append(cachePath);
    }

    protected String getSubsystemXml() throws IOException {
        return readResource(SUBSYSTEM_XML_FILE) ;
    }
}