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

import org.infinispan.configuration.cache.Configuration;
import org.infinispan.manager.EmbeddedCacheManager;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.clustering.service.AsynchronousServiceBuilder;
import org.wildfly.clustering.service.Builder;

/**
 * Builder for {@link Configuration} services.
 * @author Paul Ferraro
 */
public class ConfigurationBuilder implements Builder<Configuration>, Service<Configuration> {

    private final InjectedValue<EmbeddedCacheManager> container = new InjectedValue<>();
    private final String containerName;
    private final String cacheName;
    private final ConfigurationFactory factory;

    public ConfigurationBuilder(String containerName, String cacheName, ConfigurationFactory factory) {
        this.containerName = containerName;
        this.cacheName = cacheName;
        this.factory = factory;
    }

    @Override
    public ServiceName getServiceName() {
        return CacheServiceName.CONFIGURATION.getServiceName(this.containerName, this.cacheName);
    }

    @Override
    public ServiceBuilder<Configuration> build(ServiceTarget target) {
        return new AsynchronousServiceBuilder<>(this.getServiceName(), this).build(target)
                .addDependency(CacheContainerServiceName.CACHE_CONTAINER.getServiceName(this.containerName), EmbeddedCacheManager.class, this.container)
                .setInitialMode(ServiceController.Mode.ON_DEMAND)
        ;
    }

    @Override
    public Configuration getValue() {
        return this.container.getValue().getCacheConfiguration(this.cacheName);
    }

    @Override
    public void start(StartContext context) throws StartException {
        this.container.getValue().defineConfiguration(this.cacheName, this.factory.createConfiguration());
    }

    @Override
    public void stop(StopContext context) {
        // Infinispan has no undefineConfiguration(...)
    }
}
