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

import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jboss.as.clustering.controller.CapabilityServiceBuilder;
import org.jboss.as.clustering.function.Consumers;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.registry.Registry;
import org.wildfly.clustering.registry.RegistryFactory;
import org.wildfly.clustering.service.AsynchronousServiceBuilder;
import org.wildfly.clustering.service.Builder;
import org.wildfly.clustering.service.CompositeDependency;
import org.wildfly.clustering.service.InjectedValueDependency;
import org.wildfly.clustering.service.SuppliedValueService;
import org.wildfly.clustering.service.ValueDependency;
import org.wildfly.clustering.spi.ClusteringCacheRequirement;

/**
 * Builds a {@link Registry} service.
 * @author Paul Ferraro
 */
public class RegistryBuilder<K, V> implements CapabilityServiceBuilder<Registry<K, V>>, Supplier<Registry<K, V>> {

    private final ServiceName name;
    private final String containerName;
    private final String cacheName;

    private volatile ValueDependency<RegistryFactory<K, V>> factory;
    private volatile ValueDependency<Map.Entry<K, V>> entry;

    public RegistryBuilder(ServiceName name, String containerName, String cacheName) {
        this.name = name;
        this.containerName = containerName;
        this.cacheName = cacheName;
    }

    @Override
    public Registry<K, V> get() {
        return this.factory.getValue().createRegistry(this.entry.getValue());
    }

    @Override
    public ServiceName getServiceName() {
        return this.name;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Builder<Registry<K, V>> configure(CapabilityServiceSupport support) {
        this.factory = new InjectedValueDependency<>(ClusteringCacheRequirement.REGISTRY_FACTORY.getServiceName(support, this.containerName, this.cacheName), (Class<RegistryFactory<K, V>>) (Class<?>) RegistryFactory.class);
        this.entry = new InjectedValueDependency<>(ClusteringCacheRequirement.REGISTRY_ENTRY.getServiceName(support, this.containerName, this.cacheName), (Class<Map.Entry<K, V>>) (Class<?>) Map.Entry.class);
        return this;
    }

    @Override
    public ServiceBuilder<Registry<K, V>> build(ServiceTarget target) {
        Service<Registry<K, V>> service = new SuppliedValueService<>(Function.identity(), this, Consumers.close());
        ServiceBuilder<Registry<K, V>> builder = new AsynchronousServiceBuilder<>(this.name, service).build(target).setInitialMode(ServiceController.Mode.ON_DEMAND);
        return new CompositeDependency(this.factory, this.entry).register(builder);
    }
}
