/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import java.util.function.Consumer;
import java.util.function.Function;

import org.infinispan.Cache;
import org.infinispan.manager.EmbeddedCacheManager;
import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;
import org.jboss.as.clustering.infinispan.cache.LazyCache;
import org.jboss.as.controller.OperationContext;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.infinispan.service.InfinispanCacheRequirement;
import org.wildfly.clustering.infinispan.service.InfinispanRequirement;
import org.wildfly.clustering.service.CompositeDependency;
import org.wildfly.clustering.service.Dependency;
import org.wildfly.clustering.service.FunctionalService;
import org.wildfly.clustering.service.ServiceConfigurator;
import org.wildfly.clustering.service.ServiceDependency;
import org.wildfly.clustering.service.ServiceSupplierDependency;
import org.wildfly.clustering.service.SimpleServiceNameProvider;
import org.wildfly.clustering.service.SupplierDependency;

/**
 * @author Paul Ferraro
 */
public class LazyCacheServiceConfigurator extends SimpleServiceNameProvider implements CapabilityServiceConfigurator, Function<EmbeddedCacheManager, Cache<Object, Object>> {

    private final String containerName;
    private final String cacheName;

    private volatile SupplierDependency<EmbeddedCacheManager> container;
    private volatile Dependency cache;

    public LazyCacheServiceConfigurator(ServiceName name, String containerName, String cacheName) {
        super(name);
        this.containerName = containerName;
        this.cacheName = cacheName;
    }

    @Override
    public ServiceConfigurator configure(OperationContext context) {
        this.container = new ServiceSupplierDependency<>(InfinispanRequirement.CONTAINER.getServiceName(context, this.containerName));
        this.cache = new ServiceDependency(InfinispanCacheRequirement.CACHE.getServiceName(context, this.containerName, this.cacheName));
        return this;
    }

    @Override
    public ServiceBuilder<?> build(ServiceTarget target) {
        ServiceName name = this.getServiceName();
        ServiceBuilder<?> builder = target.addService(name);
        Consumer<Cache<Object, Object>> cache = new CompositeDependency(this.container, this.cache).register(builder).provides(name);
        Service service = new FunctionalService<>(cache, this, this.container);
        return builder.setInstance(service).setInitialMode(ServiceController.Mode.ON_DEMAND);
    }

    @Override
    public Cache<Object, Object> apply(EmbeddedCacheManager container) {
        return new LazyCache<>(container, this.cacheName);
    }
}
