/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

import org.jboss.as.clustering.msc.AsynchronousService;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.clustering.registry.Registry;
import org.wildfly.clustering.registry.RegistryEntryProvider;
import org.wildfly.clustering.registry.RegistryFactory;
import org.wildfly.clustering.spi.CacheServiceNames;

/**
 * Service that create a {@link Registry} from a factory and entry provider.
 * @author Paul Ferraro
 */
public class RegistryService<K, V> implements Service<Registry<K, V>> {

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <K, V> ServiceBuilder<Registry<K, V>> build(ServiceTarget target, String containerName, String cacheName) {
        RegistryService<K, V> service = new RegistryService<>();
        return AsynchronousService.addService(target, CacheServiceNames.REGISTRY.getServiceName(containerName, cacheName), service)
                .addDependency(CacheServiceNames.REGISTRY_FACTORY.getServiceName(containerName, cacheName), RegistryFactory.class, (Injector) service.factory)
                .addDependency(CacheServiceNames.REGISTRY_ENTRY.getServiceName(containerName, cacheName), RegistryEntryProvider.class, service.provider)
        ;
    }

    private final InjectedValue<RegistryFactory<K, V>> factory = new InjectedValue<>();
    private final InjectedValue<RegistryEntryProvider<K, V>> provider = new InjectedValue<>();

    private volatile Registry<K, V> registry;

    private RegistryService() {
        // Hide
    }

    @Override
    public Registry<K, V> getValue() {
        return this.registry;
    }

    @Override
    public void start(StartContext context) {
        this.registry = this.factory.getValue().createRegistry(this.provider.getValue());
    }

    @Override
    public void stop(StopContext context) {
        this.registry.close();
    }
}
