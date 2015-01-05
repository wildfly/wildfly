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

package org.wildfly.clustering.web.infinispan.session;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.Value;
import org.wildfly.clustering.registry.RegistryEntryProvider;
import org.wildfly.clustering.service.Builder;
import org.wildfly.clustering.spi.CacheGroupServiceName;

/**
 * Service that provides the {@link RegistryEntryProvider} for the routing {@link org.wildfly.clustering.registry.Registry}.
 * @author Paul Ferraro
 */
public class RouteRegistryEntryProviderBuilder implements Builder<RegistryEntryProvider<String, Void>>, Service<RegistryEntryProvider<String, Void>> {

    private final Value<? extends Value<String>> route;

    private volatile RegistryEntryProvider<String, Void> provider;

    public RouteRegistryEntryProviderBuilder(Value<? extends Value<String>> route) {
        this.route = route;
    }

    @Override
    public ServiceName getServiceName() {
        return CacheGroupServiceName.REGISTRY_ENTRY.getServiceName(InfinispanSessionManagerFactoryBuilder.DEFAULT_CACHE_CONTAINER);
    }

    @Override
    public ServiceBuilder<RegistryEntryProvider<String, Void>> build(ServiceTarget target) {
        return target.addService(this.getServiceName(), this).setInitialMode(ServiceController.Mode.ON_DEMAND);
    }

    @Override
    public RegistryEntryProvider<String, Void> getValue() {
        return this.provider;
    }

    @Override
    public void start(StartContext context) throws StartException {
        this.provider = new RouteRegistryEntryProvider(this.route.getValue());
    }

    @Override
    public void stop(StopContext context) {
        this.provider = null;
    }
}
