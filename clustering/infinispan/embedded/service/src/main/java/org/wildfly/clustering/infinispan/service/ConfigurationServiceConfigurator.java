/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.infinispan.service;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.infinispan.commons.dataconversion.MediaType;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.StorageType;
import org.infinispan.manager.EmbeddedCacheManager;
import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.service.CompositeDependency;
import org.wildfly.clustering.service.Dependency;
import org.wildfly.clustering.service.FunctionalService;
import org.wildfly.clustering.service.ServiceConfigurator;
import org.wildfly.clustering.service.ServiceSupplierDependency;
import org.wildfly.clustering.service.SimpleServiceNameProvider;
import org.wildfly.clustering.service.SupplierDependency;

/**
 * Configures a {@link Service} providing a cache {@link Configuration}.
 * @author Paul Ferraro
 */
public class ConfigurationServiceConfigurator extends SimpleServiceNameProvider implements CapabilityServiceConfigurator, Supplier<Configuration>, Consumer<Configuration> {

    private final String containerName;
    private final String cacheName;
    private final Consumer<ConfigurationBuilder> consumer;

    private volatile SupplierDependency<EmbeddedCacheManager> container;
    private volatile Dependency dependency;

    public ConfigurationServiceConfigurator(ServiceName name, String containerName, String cacheName, Consumer<ConfigurationBuilder> consumer) {
        super(name);
        this.containerName = containerName;
        this.cacheName = cacheName;
        this.consumer = consumer;
    }

    public ConfigurationServiceConfigurator require(Dependency dependency) {
        this.dependency = dependency;
        return this;
    }

    @Override
    public ServiceConfigurator configure(CapabilityServiceSupport support) {
        this.container = new ServiceSupplierDependency<>(InfinispanRequirement.CONTAINER.getServiceName(support, this.containerName));
        return this;
    }

    @Override
    public final ServiceBuilder<?> build(ServiceTarget target) {
        ServiceBuilder<?> builder = target.addService(this.getServiceName());
        Consumer<Configuration> configuration = new CompositeDependency(this.container, this.dependency).register(builder).provides(this.getServiceName());
        Service service = new FunctionalService<>(configuration, Function.identity(), this, this);
        return builder.setInstance(service).setInitialMode(ServiceController.Mode.ON_DEMAND);
    }

    @Override
    public Configuration get() {
        ConfigurationBuilder builder = new ConfigurationBuilder();
        this.consumer.accept(builder);
        // Auto-enable simple cache optimization if cache is local, on-heap, non-transactional, and non-persistent, and statistics are disabled
        builder.simpleCache((builder.clustering().cacheMode() == CacheMode.LOCAL) && (builder.memory().storage() == StorageType.HEAP) && !builder.transaction().transactionMode().isTransactional() && builder.persistence().stores().isEmpty() && !builder.statistics().create().enabled());

        // Set media-type appropriate for the configured memory store
        builder.encoding().mediaType(builder.memory().storage().canStoreReferences() ? MediaType.APPLICATION_OBJECT_TYPE : this.container.get().getCacheManagerConfiguration().serialization().marshaller().mediaType().toString());

        Configuration configuration = builder.build();
        EmbeddedCacheManager container = this.container.get();
        container.defineConfiguration(this.cacheName, configuration);
        return configuration;
    }

    @Override
    public void accept(Configuration configuration) {
        EmbeddedCacheManager container = this.container.get();
        container.undefineConfiguration(this.cacheName);
    }
}
