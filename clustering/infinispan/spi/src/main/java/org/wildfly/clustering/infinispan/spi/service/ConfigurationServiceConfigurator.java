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
package org.wildfly.clustering.infinispan.spi.service;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.infinispan.spi.CacheContainer;
import org.wildfly.clustering.infinispan.spi.InfinispanRequirement;
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

    private volatile SupplierDependency<CacheContainer> container;
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
        Configuration configuration = builder.build();
        this.container.get().defineConfiguration(this.cacheName, configuration);
        return configuration;
    }

    @Override
    public void accept(Configuration configuration) {
        this.container.get().undefineConfiguration(this.cacheName);
    }
}
