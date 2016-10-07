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

import org.infinispan.configuration.cache.Configuration;
import org.jboss.as.clustering.controller.CapabilityServiceBuilder;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.wildfly.clustering.infinispan.spi.CacheContainer;
import org.wildfly.clustering.infinispan.spi.InfinispanRequirement;
import org.wildfly.clustering.service.Builder;
import org.wildfly.clustering.service.InjectedValueDependency;
import org.wildfly.clustering.service.ValueDependency;

/**
 * Builder for {@link Configuration} services.
 * @author Paul Ferraro
 */
public class ConfigurationBuilder implements CapabilityServiceBuilder<Configuration>, Service<Configuration> {

    private final ServiceName name;
    private final String containerName;
    private final String cacheName;
    private final Consumer<org.infinispan.configuration.cache.ConfigurationBuilder> consumer;

    private volatile ValueDependency<CacheContainer> container;

    public ConfigurationBuilder(ServiceName name, String containerName, String cacheName, Consumer<org.infinispan.configuration.cache.ConfigurationBuilder> consumer) {
        this.name = name;
        this.containerName = containerName;
        this.cacheName = cacheName;
        this.consumer = consumer;
    }

    @Override
    public ServiceName getServiceName() {
        return this.name;
    }

    @Override
    public Builder<Configuration> configure(CapabilityServiceSupport support) {
        this.container = new InjectedValueDependency<>(InfinispanRequirement.CONTAINER.getServiceName(support, this.containerName), CacheContainer.class);
        return this;
    }

    @Override
    public ServiceBuilder<Configuration> build(ServiceTarget target) {
        return this.container.register(target.addService(this.name, this).setInitialMode(ServiceController.Mode.PASSIVE));
    }

    @Override
    public Configuration getValue() {
        return this.container.getValue().getCacheConfiguration(this.cacheName);
    }

    @Override
    public void start(StartContext context) throws StartException {
        org.infinispan.configuration.cache.ConfigurationBuilder builder = new org.infinispan.configuration.cache.ConfigurationBuilder();
        this.consumer.accept(builder);
        this.container.getValue().defineConfiguration(this.cacheName, builder.build());
    }

    @Override
    public void stop(StopContext context) {
        this.container.getValue().undefineConfiguration(this.cacheName);
    }
}
