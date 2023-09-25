/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.web.routing.infinispan;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;
import org.jboss.as.controller.ServiceNameFactory;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.server.service.ClusteringCacheRequirement;
import org.wildfly.clustering.service.FunctionalService;
import org.wildfly.clustering.service.ServiceSupplierDependency;
import org.wildfly.clustering.service.SimpleServiceNameProvider;
import org.wildfly.clustering.service.SupplierDependency;
import org.wildfly.clustering.web.infinispan.routing.RouteRegistryEntry;
import org.wildfly.clustering.web.service.WebDeploymentRequirement;

/**
 * Service that provides the {@link Map.Entry} for the routing {@link org.wildfly.clustering.registry.Registry}.
 * @author Paul Ferraro
 */
public class RouteRegistryEntryProviderServiceConfigurator extends SimpleServiceNameProvider implements CapabilityServiceConfigurator, Function<String, Map.Entry<String, Void>> {

    private final SupplierDependency<String> route;

    public RouteRegistryEntryProviderServiceConfigurator(String containerName, String serverName) {
        super(ServiceNameFactory.parseServiceName(ClusteringCacheRequirement.REGISTRY_ENTRY.getName()).append(containerName, serverName));
        this.route = new ServiceSupplierDependency<>(ServiceNameFactory.parseServiceName(WebDeploymentRequirement.LOCAL_ROUTE.getName()).append(serverName));
    }

    @Override
    public Map.Entry<String, Void> apply(String route) {
        return new RouteRegistryEntry(route);
    }

    @Override
    public ServiceBuilder<?> build(ServiceTarget target) {
        ServiceName name = this.getServiceName();
        ServiceBuilder<?> builder = target.addService(name);
        Consumer<Map.Entry<String, Void>> entry = this.route.register(builder).provides(name);
        Service service = new FunctionalService<>(entry, this, this.route);
        return builder.setInstance(service).setInitialMode(ServiceController.Mode.ON_DEMAND);
    }
}
