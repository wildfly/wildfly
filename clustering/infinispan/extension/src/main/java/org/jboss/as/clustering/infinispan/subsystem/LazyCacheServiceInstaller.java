/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.infinispan.subsystem;

import java.util.function.Function;

import org.infinispan.Cache;
import org.infinispan.manager.EmbeddedCacheManager;
import org.jboss.as.clustering.infinispan.cache.LazyCache;
import org.jboss.as.controller.RequirementServiceTarget;
import org.jboss.msc.service.ServiceController;
import org.wildfly.clustering.infinispan.service.InfinispanServiceDescriptor;
import org.wildfly.clustering.server.service.BinaryServiceConfiguration;
import org.wildfly.service.descriptor.BinaryServiceDescriptor;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.ServiceInstaller;

/**
 * Installs a lazy cache service.
 * @author Paul Ferraro
 */
public class LazyCacheServiceInstaller implements ServiceInstaller {
    static final BinaryServiceDescriptor<Cache<?, ?>> SERVICE_DESCRIPTOR = BinaryServiceDescriptor.of(InfinispanServiceDescriptor.CACHE.getName() + ".lazy", InfinispanServiceDescriptor.CACHE.getType());

    private final BinaryServiceConfiguration config;

    LazyCacheServiceInstaller(BinaryServiceConfiguration config) {
        this.config = config;
    }

    @Override
    public ServiceController<?> install(RequirementServiceTarget target) {
        String cacheName = this.config.getChildName();
        ServiceDependency<Cache<?, ?>> cache = this.config.getServiceDependency(InfinispanServiceDescriptor.CACHE_CONTAINER).map(new Function<>() {
            @Override
            public Cache<?, ?> apply(EmbeddedCacheManager manager) {
                return new LazyCache<>(manager, cacheName);
            }
        });
        return ServiceInstaller.builder(cache)
                .provides(this.config.resolveServiceName(SERVICE_DESCRIPTOR))
                .requires(this.config.getServiceDependency(InfinispanServiceDescriptor.CACHE))
                .build()
                .install(target);
    }
}
