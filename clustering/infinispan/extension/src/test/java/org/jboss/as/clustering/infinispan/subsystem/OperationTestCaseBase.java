/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.clustering.infinispan.subsystem;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;

import java.io.IOException;
import java.util.Collections;

import org.jboss.as.clustering.controller.Attribute;
import org.jboss.as.clustering.controller.CommonUnaryRequirement;
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
 * @author Radoslav Husar
 */
public class OperationTestCaseBase extends AbstractSubsystemTest {

    static final String SUBSYSTEM_XML_FILE = String.format("subsystem-infinispan-%d_%d.xml", InfinispanSchema.CURRENT.major(), InfinispanSchema.CURRENT.minor());

    public OperationTestCaseBase() {
        super(InfinispanExtension.SUBSYSTEM_NAME, new InfinispanExtension());
    }

    KernelServicesBuilder createKernelServicesBuilder() {
        return this.createKernelServicesBuilder(this.createAdditionalInitialization());
    }

    AdditionalInitialization createAdditionalInitialization() {
        return new JGroupsSubsystemInitialization()
                .require(CommonUnaryRequirement.OUTBOUND_SOCKET_BINDING, "hotrod-server-1", "hotrod-server-2")
                .require(CommonUnaryRequirement.DATA_SOURCE, "ExampleDS", "new-datasource")
                ;
    }

    // cache container access
    protected static ModelNode getCacheContainerAddOperation(String containerName) {
        PathAddress address = getCacheContainerAddress(containerName);
        return Operations.createAddOperation(address, Collections.<Attribute, ModelNode>singletonMap(CacheContainerResourceDefinition.Attribute.DEFAULT_CACHE, new ModelNode("default")));
    }

    protected static ModelNode getCacheContainerReadOperation(String containerName, Attribute attribute) {
        return Operations.createReadAttributeOperation(getCacheContainerAddress(containerName), attribute);
    }

    protected static ModelNode getCacheContainerWriteOperation(String containerName, Attribute attribute, String value) {
        PathAddress cacheAddress = getCacheContainerAddress(containerName);
        return Operations.createWriteAttributeOperation(cacheAddress, attribute, new ModelNode(value));
    }

    protected static ModelNode getCacheContainerRemoveOperation(String containerName) {
        PathAddress containerAddr = getCacheContainerAddress(containerName);
        return Util.createRemoveOperation(containerAddr);
    }

    // cache access
    protected static ModelNode getCacheAddOperation(String containerName, String cacheType, String cacheName) {
        PathAddress address = getCacheAddress(containerName, cacheType, cacheName);
        return Operations.createAddOperation(address, Collections.emptyMap());
    }

    protected static ModelNode getCacheReadOperation(String containerName, String cacheType, String cacheName, Attribute attribute) {
        return Operations.createReadAttributeOperation(getCacheAddress(containerName, cacheType, cacheName), attribute);
    }

    protected static ModelNode getCacheWriteOperation(String containerName, String cacheType, String cacheName, Attribute attribute, String value) {
        return Operations.createWriteAttributeOperation(getCacheAddress(containerName, cacheType, cacheName), attribute, new ModelNode(value));
    }

    protected static ModelNode getCacheRemoveOperation(String containerName, String cacheType, String cacheName) {
        return Util.createRemoveOperation(getCacheAddress(containerName, cacheType, cacheName));
    }

    // cache store access
    protected static ModelNode getCacheStoreReadOperation(String containerName, String cacheType, String cacheName, Attribute attribute) {
        return Operations.createReadAttributeOperation(getCustomCacheStoreAddress(containerName, cacheType, cacheName), attribute);
    }

    protected static ModelNode getCacheStoreWriteOperation(String containerName, String cacheName, String cacheType, Attribute attribute, String value) {
        return Operations.createWriteAttributeOperation(getCustomCacheStoreAddress(containerName,  cacheType, cacheName), attribute, new ModelNode(value));
    }

    protected static ModelNode getJDBCCacheStoreReadOperation(String containerName, String cacheType, String cacheName, Attribute attribute) {
        return Operations.createReadAttributeOperation(getJDBCCacheStoreAddress(containerName, cacheType, cacheName), attribute);
    }

    protected static ModelNode getJDBCCacheStoreWriteOperation(String containerName, String cacheType, String cacheName, Attribute attribute, String value) {
        return Operations.createWriteAttributeOperation(getJDBCCacheStoreAddress(containerName, cacheType, cacheName), attribute, new ModelNode(value));
    }

    // cache store property access
    protected static ModelNode getCacheStoreGetPropertyOperation(PathAddress cacheStoreAddress, String propertyName) {
        return Operations.createMapGetOperation(cacheStoreAddress, StoreResourceDefinition.Attribute.PROPERTIES, propertyName);
    }

    protected static ModelNode getCacheStorePutPropertyOperation(PathAddress cacheStoreAddress, String propertyName, String propertyValue) {
        return Operations.createMapPutOperation(cacheStoreAddress, StoreResourceDefinition.Attribute.PROPERTIES, propertyName, propertyValue);
    }

    protected static ModelNode getCacheStoreRemovePropertyOperation(PathAddress cacheStoreAddress, String propertyName) {
        return Operations.createMapRemoveOperation(cacheStoreAddress, StoreResourceDefinition.Attribute.PROPERTIES, propertyName);
    }

    protected static ModelNode getCacheStoreClearPropertiesOperation(PathAddress cacheStoreAddress) {
        return Operations.createMapClearOperation(cacheStoreAddress, StoreResourceDefinition.Attribute.PROPERTIES);
    }

    protected static ModelNode getCacheStoreUndefinePropertiesOperation(PathAddress cacheStoreAddress) {
        return Util.getUndefineAttributeOperation(cacheStoreAddress, StoreResourceDefinition.Attribute.PROPERTIES.getName());
    }

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
    @SuppressWarnings("deprecation")
    protected static PathAddress getCacheStorePropertyAddress(String containerName, String cacheType, String cacheName, String propertyName) {
        return getCustomCacheStoreAddress(containerName, cacheType, cacheName).append(StorePropertyResourceDefinition.pathElement(propertyName));
    }

    protected static PathAddress getJDBCCacheStoreAddress(String containerName, String cacheType, String cacheName) {
        return getCacheAddress(containerName, cacheType, cacheName).append(JDBCStoreResourceDefinition.PATH);
    }

    protected static PathAddress getMixedKeyedJDBCCacheStoreAddress(String containerName, String cacheType, String cacheName) {
        return getCacheAddress(containerName, cacheType, cacheName).append(MixedKeyedJDBCStoreResourceDefinition.PATH);
    }

    protected static PathAddress getMixedKeyedJDBCCacheStoreLegacyAddress(String containerName, String cacheType, String cacheName) {
        return getCacheAddress(containerName, cacheType, cacheName).append(MixedKeyedJDBCStoreResourceDefinition.LEGACY_PATH);
    }

    protected static PathAddress getBinaryKeyedJDBCCacheStoreAddress(String containerName, String cacheType, String cacheName) {
        return getCacheAddress(containerName, cacheType, cacheName).append(BinaryKeyedJDBCStoreResourceDefinition.PATH);
    }

    protected static PathAddress getBinaryKeyedJDBCCacheStoreLegacyAddress(String containerName, String cacheType, String cacheName) {
        return getCacheAddress(containerName, cacheType, cacheName).append(BinaryKeyedJDBCStoreResourceDefinition.LEGACY_PATH);
    }

    protected static PathAddress getStringKeyedJDBCCacheStoreAddress(String containerName, String cacheType, String cacheName) {
        return getCacheAddress(containerName, cacheType, cacheName).append(StringKeyedJDBCStoreResourceDefinition.STRING_JDBC_PATH);
    }

    protected static PathAddress getStringKeyedJDBCCacheStoreLegacyAddress(String containerName, String cacheType, String cacheName) {
        return getCacheAddress(containerName, cacheType, cacheName).append(StringKeyedJDBCStoreResourceDefinition.LEGACY_PATH);
    }

    protected static PathAddress getRemoteCacheStoreAddress(String containerName, String cacheType, String cacheName) {
        return getCacheAddress(containerName, cacheType, cacheName).append(RemoteStoreResourceDefinition.PATH);
    }

    protected static PathAddress getRemoteCacheStoreLegacyAddress(String containerName, String cacheType, String cacheName) {
        return getCacheAddress(containerName, cacheType, cacheName).append(RemoteStoreResourceDefinition.LEGACY_PATH);
    }

    protected static PathAddress getFileCacheStoreAddress(String containerName, String cacheType, String cacheName) {
        return getCacheAddress(containerName, cacheType, cacheName).append(FileStoreResourceDefinition.PATH);
    }

    protected static PathAddress getFileCacheStoreLegacyAddress(String containerName, String cacheType, String cacheName) {
        return getCacheAddress(containerName, cacheType, cacheName).append(FileStoreResourceDefinition.LEGACY_PATH);
    }

    protected static PathAddress getCustomCacheStoreAddress(String containerName, String cacheType, String cacheName) {
        return getCacheAddress(containerName, cacheType, cacheName).append(CustomStoreResourceDefinition.PATH);
    }

    protected static PathAddress getCustomCacheStoreLegacyAddress(String containerName, String cacheType, String cacheName) {
        return getCacheAddress(containerName, cacheType, cacheName).append(CustomStoreResourceDefinition.LEGACY_PATH);
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