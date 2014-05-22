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
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.clustering.group.Group;
import org.wildfly.clustering.registry.RegistryFactory;
import org.wildfly.clustering.spi.CacheServiceNames;

/**
 * {@link Service} that provides a non-clustered {@link RegistryFactory}.
 * @author Paul Ferraro
 * @param <K> the registry key type
 * @param <V> the registry value type
 */
public class LocalRegistryFactoryService<K, V> implements Service<RegistryFactory<K, V>> {

    public static <K, V> ServiceBuilder<RegistryFactory<K, V>> build(ServiceTarget target, ServiceName name, String containerName, String cacheName) {
        LocalRegistryFactoryService<K, V> service = new LocalRegistryFactoryService<>();
        return target.addService(name, service)
                .addDependency(CacheServiceNames.GROUP.getServiceName(containerName, cacheName), Group.class, service.group)
        ;
    }

    private final InjectedValue<Group> group = new InjectedValue<>();

    private volatile RegistryFactory<K, V> factory;

    private LocalRegistryFactoryService() {
        // Hide
    }

    @Override
    public RegistryFactory<K, V> getValue() {
        return this.factory;
    }

    @Override
    public void start(StartContext arg0) throws StartException {
        this.factory = new LocalRegistryFactory<>(this.group.getValue());
    }

    @Override
    public void stop(StopContext arg0) {
        this.factory = null;
    }
}
