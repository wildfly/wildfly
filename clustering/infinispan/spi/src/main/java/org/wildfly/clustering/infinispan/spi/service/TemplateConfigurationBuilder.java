/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.clustering.service.Builder;

/**
 * Builds a cache configuration based on the configuration of a template cache.
 * @author Paul Ferraro
 */
public class TemplateConfigurationBuilder implements Builder<Configuration>, ConfigurationFactory {

    private final InjectedValue<Configuration> template = new InjectedValue<>();
    private final Builder<Configuration> builder;
    private final String containerName;
    private final String templateCacheName;

    /**
     * Constructs a new cache configuration builder.
     * @param containerName the name of the cache container
     * @param cacheName the name of the target cache
     * @param templateCacheName the name of the template cache
     */
    public TemplateConfigurationBuilder(String containerName, String cacheName, String templateCacheName) {
        this.builder = new ConfigurationBuilder(containerName, cacheName, this);
        this.containerName = containerName;
        this.templateCacheName = templateCacheName;
    }

    @Override
    public ServiceName getServiceName() {
        return this.builder.getServiceName();
    }

    @Override
    public ServiceBuilder<Configuration> build(ServiceTarget target) {
        return this.builder.build(target).addDependency(CacheServiceName.CONFIGURATION.getServiceName(this.containerName, this.templateCacheName), Configuration.class, this.template);
    }

    @Override
    public Configuration createConfiguration() {
        return new org.infinispan.configuration.cache.ConfigurationBuilder().read(this.template.getValue()).build();
    }
}
