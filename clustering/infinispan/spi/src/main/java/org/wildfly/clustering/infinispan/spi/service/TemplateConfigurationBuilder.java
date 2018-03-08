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

import java.util.function.Consumer;

import org.infinispan.configuration.cache.Configuration;
import org.jboss.as.clustering.controller.CapabilityServiceBuilder;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.infinispan.spi.InfinispanCacheRequirement;
import org.wildfly.clustering.service.Builder;
import org.wildfly.clustering.service.InjectedValueDependency;
import org.wildfly.clustering.service.ValueDependency;

/**
 * Builds a cache configuration based on the configuration of a template cache.
 * @author Paul Ferraro
 */
public class TemplateConfigurationBuilder implements CapabilityServiceBuilder<Configuration>, Consumer<org.infinispan.configuration.cache.ConfigurationBuilder> {

    private final CapabilityServiceBuilder<Configuration> builder;
    private final String containerName;
    private final String templateCacheName;

    private volatile ValueDependency<Configuration> template;

    /**
     * Constructs a new cache configuration builder.
     * @param containerName the name of the cache container
     * @param cacheName the name of the target cache
     * @param templateCacheName the name of the template cache
     */
    public TemplateConfigurationBuilder(ServiceName name, String containerName, String cacheName, String templateCacheName) {
        this(name, containerName, cacheName, templateCacheName, builder -> {});
    }

    public TemplateConfigurationBuilder(ServiceName name, String containerName, String cacheName, String templateCacheName, Consumer<org.infinispan.configuration.cache.ConfigurationBuilder> templateConsumer) {
        this.builder = new ConfigurationBuilder(name, containerName, cacheName, this.andThen(templateConsumer));
        this.containerName = containerName;
        this.templateCacheName = templateCacheName;
    }

    @Override
    public void accept(org.infinispan.configuration.cache.ConfigurationBuilder builder) {
        builder.read(this.template.getValue());
    }

    @Override
    public ServiceName getServiceName() {
        return this.builder.getServiceName();
    }

    @Override
    public Builder<Configuration> configure(CapabilityServiceSupport support) {
        this.template = new InjectedValueDependency<>(InfinispanCacheRequirement.CONFIGURATION.getServiceName(support, this.containerName, this.templateCacheName), Configuration.class);
        this.builder.configure(support);
        return this;
    }

    @Override
    public ServiceBuilder<Configuration> build(ServiceTarget target) {
        return this.template.register(this.builder.build(target));
    }
}
