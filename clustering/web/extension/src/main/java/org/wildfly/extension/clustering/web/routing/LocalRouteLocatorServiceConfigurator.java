/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.web.routing;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.service.FunctionalService;
import org.wildfly.clustering.service.ServiceConfigurator;
import org.wildfly.clustering.service.ServiceSupplierDependency;
import org.wildfly.clustering.service.SupplierDependency;
import org.wildfly.clustering.web.WebDeploymentConfiguration;
import org.wildfly.clustering.web.cache.routing.LocalRouteLocator;
import org.wildfly.clustering.web.routing.RouteLocator;
import org.wildfly.clustering.web.service.WebDeploymentRequirement;

/**
 * Configures a service providing a local route locator.
 * @author Paul Ferraro
 */
public class LocalRouteLocatorServiceConfigurator extends RouteLocatorServiceNameProvider implements CapabilityServiceConfigurator, Supplier<RouteLocator> {

    private final WebDeploymentConfiguration deploymentConfiguration;

    private volatile SupplierDependency<String> route;

    public LocalRouteLocatorServiceConfigurator(WebDeploymentConfiguration deploymentConfiguration) {
        super(deploymentConfiguration);
        this.deploymentConfiguration = deploymentConfiguration;
    }

    @Override
    public RouteLocator get() {
        return new LocalRouteLocator(this.route.get());
    }

    @Override
    public ServiceConfigurator configure(CapabilityServiceSupport support) {
        this.route = new ServiceSupplierDependency<>(WebDeploymentRequirement.LOCAL_ROUTE.getServiceName(support, this.deploymentConfiguration.getServerName()));
        return this;
    }

    @Override
    public ServiceBuilder<?> build(ServiceTarget target) {
        ServiceName name = this.getServiceName();
        ServiceBuilder<?> builder = target.addService(name);
        Consumer<RouteLocator> locator = this.route.register(builder).provides(name);
        Service service = new FunctionalService<>(locator, Function.identity(), this);
        return builder.setInstance(service).setInitialMode(ServiceController.Mode.ON_DEMAND);
    }
}
