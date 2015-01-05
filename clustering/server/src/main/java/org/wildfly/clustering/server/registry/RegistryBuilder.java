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

package org.wildfly.clustering.server.registry;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.clustering.registry.Registry;
import org.wildfly.clustering.registry.RegistryEntryProvider;
import org.wildfly.clustering.registry.RegistryFactory;
import org.wildfly.clustering.service.AsynchronousServiceBuilder;
import org.wildfly.clustering.service.Builder;
import org.wildfly.clustering.spi.CacheGroupServiceName;

/**
 * Builds a {@link Registry} service.
 * @author Paul Ferraro
 */
public class RegistryBuilder<K, V> implements Builder<Registry<K, V>>, Service<Registry<K, V>> {

    @SuppressWarnings("rawtypes")
    private final InjectedValue<RegistryFactory> factory = new InjectedValue<>();
    @SuppressWarnings("rawtypes")
    private final InjectedValue<RegistryEntryProvider> provider = new InjectedValue<>();
    private final String containerName;
    private final String cacheName;

    private volatile Registry<K, V> registry;

    public RegistryBuilder(String containerName, String cacheName) {
        this.containerName = containerName;
        this.cacheName = cacheName;
    }

    @Override
    public ServiceName getServiceName() {
        return CacheGroupServiceName.REGISTRY.getServiceName(this.containerName, this.cacheName);
    }

    @Override
    public ServiceBuilder<Registry<K, V>> build(ServiceTarget target) {
        return new AsynchronousServiceBuilder<>(this.getServiceName(), this).build(target)
                .addDependency(CacheGroupServiceName.REGISTRY_FACTORY.getServiceName(this.containerName, this.cacheName), RegistryFactory.class, this.factory)
                .addDependency(CacheGroupServiceName.REGISTRY_ENTRY.getServiceName(this.containerName, this.cacheName), RegistryEntryProvider.class, this.provider)
                .setInitialMode(ServiceController.Mode.ON_DEMAND)
        ;
    }

    @Override
    public Registry<K, V> getValue() {
        return this.registry;
    }

    @Override
    public void start(StartContext context) {
        RegistryFactory<K, V> factory = this.factory.getValue();
        RegistryEntryProvider<K, V> provider = this.provider.getValue();
        this.registry = factory.createRegistry(provider);
    }

    @Override
    public void stop(StopContext context) {
        this.registry.close();
    }
}
