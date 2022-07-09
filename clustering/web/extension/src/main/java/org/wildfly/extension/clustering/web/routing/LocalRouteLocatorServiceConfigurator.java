/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
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
