/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.infinispan.service;

import java.util.function.Consumer;

import org.infinispan.configuration.cache.Configuration;
import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.service.ServiceConfigurator;
import org.wildfly.clustering.service.ServiceSupplierDependency;
import org.wildfly.clustering.service.SupplierDependency;

/**
 * Configures a {@link org.jboss.msc.Service} providing a cache configuration based on a configuration template.
 * @author Paul Ferraro
 */
public class TemplateConfigurationServiceConfigurator implements CapabilityServiceConfigurator, Consumer<org.infinispan.configuration.cache.ConfigurationBuilder> {

    private final ConfigurationServiceConfigurator configurator;
    private final String containerName;
    private final String templateCacheName;

    private volatile SupplierDependency<Configuration> template;

    /**
     * Constructs a new cache configuration builder.
     * @param containerName the name of the cache container
     * @param cacheName the name of the target cache
     * @param templateCacheName the name of the template cache
     */
    public TemplateConfigurationServiceConfigurator(ServiceName name, String containerName, String cacheName, String templateCacheName) {
        this(name, containerName, cacheName, templateCacheName, builder -> {});
    }

    public TemplateConfigurationServiceConfigurator(ServiceName name, String containerName, String cacheName, String templateCacheName, Consumer<org.infinispan.configuration.cache.ConfigurationBuilder> templateConsumer) {
        this.configurator = new ConfigurationServiceConfigurator(name, containerName, cacheName, this.andThen(templateConsumer));
        this.containerName = containerName;
        this.templateCacheName = templateCacheName;
    }

    @Override
    public void accept(org.infinispan.configuration.cache.ConfigurationBuilder builder) {
        builder.read(this.template.get());
    }

    @Override
    public ServiceName getServiceName() {
        return this.configurator.getServiceName();
    }

    @Override
    public ServiceConfigurator configure(CapabilityServiceSupport support) {
        this.template = new ServiceSupplierDependency<>(InfinispanCacheRequirement.CONFIGURATION.getServiceName(support, this.containerName, this.templateCacheName));
        this.configurator.configure(support);
        return this;
    }

    @Override
    public ServiceBuilder<?> build(ServiceTarget target) {
        return this.configurator.require(this.template).build(target);
    }
}
