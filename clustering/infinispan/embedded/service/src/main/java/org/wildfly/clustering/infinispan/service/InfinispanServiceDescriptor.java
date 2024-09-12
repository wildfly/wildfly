/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.infinispan.service;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.manager.EmbeddedCacheManager;
import org.wildfly.service.descriptor.BinaryServiceDescriptor;
import org.wildfly.service.descriptor.UnaryServiceDescriptor;

/**
 * Descriptions of Infinispan services.
 * @author Paul Ferraro
 */
public interface InfinispanServiceDescriptor {

    UnaryServiceDescriptor<EmbeddedCacheManager> CACHE_CONTAINER = UnaryServiceDescriptor.of("org.wildfly.clustering.infinispan.cache-container", EmbeddedCacheManager.class);
    UnaryServiceDescriptor<GlobalConfiguration> CACHE_CONTAINER_CONFIGURATION = UnaryServiceDescriptor.of("org.wildfly.clustering.infinispan.cache-container-configuration", GlobalConfiguration.class);

    @SuppressWarnings("unchecked")
    UnaryServiceDescriptor<Cache<?, ?>> DEFAULT_CACHE = UnaryServiceDescriptor.of("org.wildfly.clustering.infinispan.default-cache", (Class<Cache<?, ?>>) (Class<?>) Cache.class);
    UnaryServiceDescriptor<Configuration> DEFAULT_CACHE_CONFIGURATION = UnaryServiceDescriptor.of("org.wildfly.clustering.infinispan.default-cache-configuration", Configuration.class);

    BinaryServiceDescriptor<Cache<?, ?>> CACHE = BinaryServiceDescriptor.of("org.wildfly.clustering.infinispan.cache", DEFAULT_CACHE);
    BinaryServiceDescriptor<Configuration> CACHE_CONFIGURATION = BinaryServiceDescriptor.of("org.wildfly.clustering.infinispan.cache-configuration", DEFAULT_CACHE_CONFIGURATION);
}
