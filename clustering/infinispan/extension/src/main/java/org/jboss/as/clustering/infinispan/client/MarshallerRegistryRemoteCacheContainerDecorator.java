/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.client;

import org.infinispan.client.hotrod.impl.MarshallerRegistry;
import org.wildfly.clustering.cache.infinispan.remote.RemoteCacheContainerDecorator;
import org.wildfly.clustering.infinispan.client.RemoteCacheContainer;

/**
 * Decorator that overrides the marshaller registry with a deployment-specific one.
 * This is necessary because {@code RemoteQueryFactory} obtains its {@code SerializationContext}
 * from the container's marshaller registry rather than the per-cache marshaller,
 * so query deserialization requires a registry that includes deployment-specific schemas.
 *
 * @author Radoslav Husar
 * @since 41
 */
class MarshallerRegistryRemoteCacheContainerDecorator extends RemoteCacheContainerDecorator implements RemoteCacheContainer {
    private final ManagedRemoteCacheContainer container;
    private final MarshallerRegistry marshallerRegistry;

    MarshallerRegistryRemoteCacheContainerDecorator(ManagedRemoteCacheContainer container, MarshallerRegistry marshallerRegistry) {
        super(container);
        this.container = container;
        this.marshallerRegistry = marshallerRegistry;
    }

    @Override
    public MarshallerRegistry getMarshallerRegistry() {
        return this.marshallerRegistry;
    }

    @Override
    public String getName() {
        return this.container.getName();
    }

    @Override
    public String[] getServers() {
        return this.container.getServers();
    }

    @Override
    public int getActiveConnectionCount() {
        return this.container.getActiveConnectionCount();
    }

    @Override
    public int getConnectionCount() {
        return this.container.getConnectionCount();
    }

    @Override
    public int getIdleConnectionCount() {
        return this.container.getIdleConnectionCount();
    }

    @Override
    public long getRetries() {
        return this.container.getRetries();
    }
}
