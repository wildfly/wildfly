/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.infinispan.client.service;

import java.util.List;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.configuration.Configuration;
import org.infinispan.client.hotrod.configuration.RemoteCacheConfiguration;
import org.jboss.modules.Module;
import org.wildfly.clustering.infinispan.client.RemoteCacheContainer;
import org.wildfly.service.descriptor.BinaryServiceDescriptor;
import org.wildfly.service.descriptor.UnaryServiceDescriptor;

/**
 * @author Paul Ferraro
 */
public interface HotRodServiceDescriptor {
    UnaryServiceDescriptor<RemoteCacheContainer> REMOTE_CACHE_CONTAINER = UnaryServiceDescriptor.of("org.wildfly.clustering.infinispan.remote-cache-container", RemoteCacheContainer.class);

    UnaryServiceDescriptor<Configuration> REMOTE_CACHE_CONTAINER_CONFIGURATION = UnaryServiceDescriptor.of("org.wildfly.clustering.infinispan.remote-cache-container-configuration", Configuration.class);
    @SuppressWarnings("unchecked")
    UnaryServiceDescriptor<List<Module>> REMOTE_CACHE_CONTAINER_MODULES = UnaryServiceDescriptor.of("org.wildfly.clustering.infinispan.remote-cache-container-modules", (Class<List<Module>>) (Class<?>) List.class);

    BinaryServiceDescriptor<RemoteCacheConfiguration> REMOTE_CACHE_CONFIGURATION = BinaryServiceDescriptor.of("org.wildfly.clustering.infinispan.remote-cache-configuration", RemoteCacheConfiguration.class);

    @SuppressWarnings("unchecked")
    UnaryServiceDescriptor<RemoteCache<?, ?>> DEFAULT_REMOTE_CACHE = UnaryServiceDescriptor.of("org.wildfly.clustering.infinispan.default-remote-cache", (Class<RemoteCache<?, ?>>) (Class<?>) RemoteCache.class);
    BinaryServiceDescriptor<RemoteCache<?, ?>> REMOTE_CACHE = BinaryServiceDescriptor.of("org.wildfly.clustering.infinispan.remote-cache", DEFAULT_REMOTE_CACHE);
}
