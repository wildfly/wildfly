/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.web.routing;

import java.util.function.Consumer;

import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.web.WebDeploymentConfiguration;
import org.wildfly.clustering.web.cache.routing.NullRouteLocator;
import org.wildfly.clustering.web.routing.RouteLocator;

/**
 * Configures a service providing a null route locator.
 * @author Paul Ferraro
 */
public class NullRouteLocatorServiceConfigurator extends RouteLocatorServiceNameProvider implements CapabilityServiceConfigurator {

    public NullRouteLocatorServiceConfigurator(WebDeploymentConfiguration deploymentConfiguration) {
        super(deploymentConfiguration);
    }

    @Override
    public ServiceBuilder<?> build(ServiceTarget target) {
        ServiceName name = this.getServiceName();
        ServiceBuilder<?> builder = target.addService(name);
        Consumer<RouteLocator> locator = builder.provides(name);
        Service service = Service.newInstance(locator, new NullRouteLocator());
        return builder.setInstance(service).setInitialMode(ServiceController.Mode.ON_DEMAND);
    }
}
