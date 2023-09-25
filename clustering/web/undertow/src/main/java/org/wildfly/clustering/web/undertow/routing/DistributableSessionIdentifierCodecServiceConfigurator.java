/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.web.undertow.routing;

import java.util.function.Consumer;
import java.util.function.Function;

import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;
import org.jboss.as.web.session.RoutingSupport;
import org.jboss.as.web.session.SessionIdentifierCodec;
import org.jboss.as.web.session.SimpleRoutingSupport;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.service.FunctionalService;
import org.wildfly.clustering.service.SimpleServiceNameProvider;
import org.wildfly.clustering.service.SupplierDependency;
import org.wildfly.clustering.web.routing.RouteLocator;

/**
 * Builds a distributable {@link SessionIdentifierCodec} service.
 * @author Paul Ferraro
 */
public class DistributableSessionIdentifierCodecServiceConfigurator extends SimpleServiceNameProvider implements CapabilityServiceConfigurator, Function<RouteLocator, SessionIdentifierCodec> {

    private final SupplierDependency<RouteLocator> locatorDependency;
    private final RoutingSupport routing = new SimpleRoutingSupport();

    public DistributableSessionIdentifierCodecServiceConfigurator(ServiceName name, SupplierDependency<RouteLocator> locatorDependency) {
        super(name);
        this.locatorDependency = locatorDependency;
    }

    @Override
    public SessionIdentifierCodec apply(RouteLocator locator) {
        return new DistributableSessionIdentifierCodec(locator, this.routing);
    }

    @Override
    public ServiceBuilder<?> build(ServiceTarget target) {
        ServiceBuilder<?> builder = target.addService(this.getServiceName());
        Consumer<SessionIdentifierCodec> codec = this.locatorDependency.register(builder).provides(this.getServiceName());
        Service service = new FunctionalService<>(codec, this, this.locatorDependency);
        return builder.setInstance(service).setInitialMode(ServiceController.Mode.ON_DEMAND);
    }
}
