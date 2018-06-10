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

import java.util.AbstractMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.service.FunctionalService;
import org.wildfly.clustering.service.SimpleServiceNameProvider;
import org.wildfly.clustering.service.SupplierDependency;
import org.wildfly.clustering.spi.ClusteringCacheRequirement;

/**
 * Service that provides the {@link Map.Entry} for the routing {@link org.wildfly.clustering.registry.Registry}.
 * @author Paul Ferraro
 */
public class RouteRegistryEntryProviderBuilder extends SimpleServiceNameProvider implements CapabilityServiceConfigurator, Function<String, Map.Entry<String, Void>> {

    private final SupplierDependency<String> route;

    public RouteRegistryEntryProviderBuilder(String serverName, SupplierDependency<String> route) {
        super(ServiceName.parse(ClusteringCacheRequirement.REGISTRY_ENTRY.resolve(InfinispanSessionManagerFactoryServiceConfigurator.DEFAULT_CACHE_CONTAINER, serverName)));
        this.route = route;
    }

    @Override
    public Map.Entry<String, Void> apply(String route) {
        return new AbstractMap.SimpleImmutableEntry<>(route, null);
    }

    @Override
    public ServiceBuilder<?> build(ServiceTarget target) {
        ServiceBuilder<?> builder = target.addService(this.getServiceName());
        Consumer<Map.Entry<String, Void>> entry = this.route.register(builder).provides(this.getServiceName());
        Service service = new FunctionalService<>(entry, this, this.route);
        return builder.setInstance(service).setInitialMode(ServiceController.Mode.ON_DEMAND);
    }
}
