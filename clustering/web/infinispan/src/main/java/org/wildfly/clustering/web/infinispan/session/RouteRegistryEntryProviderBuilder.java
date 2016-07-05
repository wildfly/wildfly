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

import org.jboss.as.clustering.controller.CapabilityServiceBuilder;
import org.jboss.as.controller.OperationContext;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.ValueService;
import org.jboss.msc.value.Value;
import org.wildfly.clustering.registry.RegistryEntryProvider;
import org.wildfly.clustering.service.Builder;
import org.wildfly.clustering.service.InjectedValueDependency;
import org.wildfly.clustering.service.ValueDependency;
import org.wildfly.clustering.spi.CacheGroupServiceName;
import org.wildfly.clustering.web.ServerRequirement;

/**
 * Service that provides the {@link RegistryEntryProvider} for the routing {@link org.wildfly.clustering.registry.Registry}.
 * @author Paul Ferraro
 */
public class RouteRegistryEntryProviderBuilder implements CapabilityServiceBuilder<RegistryEntryProvider<String, Void>> {

    private final String serverName;

    private volatile ValueDependency<String> route;

    public RouteRegistryEntryProviderBuilder(String serverName) {
        this.serverName = serverName;
    }

    @Override
    public ServiceName getServiceName() {
        return CacheGroupServiceName.REGISTRY_ENTRY.getServiceName(InfinispanSessionManagerFactoryBuilder.DEFAULT_CACHE_CONTAINER, this.serverName);
    }

    @Override
    public Builder<RegistryEntryProvider<String, Void>> configure(OperationContext context) {
        this.route = new InjectedValueDependency<>(ServerRequirement.ROUTE.getServiceName(context, this.serverName), String.class);
        return this;
    }

    @Override
    public ServiceBuilder<RegistryEntryProvider<String, Void>> build(ServiceTarget target) {
        Value<RegistryEntryProvider<String, Void>> value = () -> new RouteRegistryEntryProvider(this.route.getValue());
        return this.route.register(target.addService(this.getServiceName(), new ValueService<>(value))).setInitialMode(ServiceController.Mode.ON_DEMAND);
    }
}
