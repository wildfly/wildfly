/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.web.routing;

import java.util.function.Consumer;
import java.util.function.Function;

import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;
import org.jboss.as.controller.ServiceNameFactory;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.service.FunctionalService;
import org.wildfly.clustering.service.SimpleServiceNameProvider;
import org.wildfly.clustering.service.SupplierDependency;
import org.wildfly.clustering.web.service.WebDeploymentRequirement;

/**
 * @author Paul Ferraro
 */
public class LocalRouteServiceConfigurator extends SimpleServiceNameProvider implements CapabilityServiceConfigurator {

    private final SupplierDependency<String> route;

    public LocalRouteServiceConfigurator(String serverName, SupplierDependency<String> route) {
        super(ServiceNameFactory.parseServiceName(WebDeploymentRequirement.LOCAL_ROUTE.getName()).append(serverName));
        this.route = route;
    }

    @Override
    public ServiceBuilder<?> build(ServiceTarget target) {
        ServiceBuilder<?> builder = target.addService(this.getServiceName());
        Consumer<String> route = this.route.register(builder).provides(this.getServiceName());
        Service service = new FunctionalService<>(route, Function.identity(), this.route);
        return builder.setInstance(service).setInitialMode(ServiceController.Mode.ON_DEMAND);
    }
}
