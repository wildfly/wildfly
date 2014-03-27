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
package org.wildfly.clustering.server.singleton;

import java.util.Collection;
import java.util.Collections;

import org.infinispan.configuration.cache.CacheMode;
import org.jboss.as.clustering.infinispan.CacheContainer;
import org.jboss.as.clustering.infinispan.subsystem.CacheServiceProvider;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.singleton.SingletonServiceBuilderFactory;

/**
 * Installs a {@link SingletonServiceBuilderFactory} per cache.
 * @author Paul Ferraro
 */
public class SingletonServiceBuilderFactoryProvider implements CacheServiceProvider {

    private static ServiceName getServiceName(String containerName, String cacheName) {
        return SingletonServiceBuilderFactory.SERVICE_NAME.append(containerName, cacheName);
    }

    @Override
    public Collection<ServiceName> getServiceNames(String containerName, String cacheName, boolean defaultCache) {
        return Collections.singleton(getServiceName(containerName, cacheName));
    }

    @Override
    public Collection<ServiceController<?>> install(ServiceTarget target, String containerName, String cacheName, CacheMode mode, boolean defaultCache, ModuleIdentifier moduleId) {
        ServiceName name = getServiceName(containerName, cacheName);

        ServiceBuilder<SingletonServiceBuilderFactory> builder = target.addService(name, mode.isClustered() ? new SingletonServiceBuilderFactoryService(containerName, cacheName) : new LocalSingletonServiceBuilderFactoryService());

        if (defaultCache) {
            builder.addAliases(getServiceName(containerName, CacheContainer.DEFAULT_CACHE_ALIAS));
        }

        ServiceController<SingletonServiceBuilderFactory> controller = builder.setInitialMode(ServiceController.Mode.ACTIVE).install();

        return Collections.<ServiceController<?>>singleton(controller);
    }
}
