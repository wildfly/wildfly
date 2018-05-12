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
package org.wildfly.clustering.web.undertow.session;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.as.web.session.RoutingSupport;
import org.jboss.as.web.session.SessionIdentifierCodec;
import org.jboss.as.web.session.SimpleRoutingSupport;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.service.FunctionalService;
import org.wildfly.clustering.service.ServiceConfigurator;
import org.wildfly.clustering.service.SimpleServiceNameProvider;
import org.wildfly.clustering.web.session.RouteLocator;
import org.wildfly.clustering.web.session.RouteLocatorServiceConfiguratorProvider;

/**
 * Builds a distributable {@link SessionIdentifierCodec} service.
 * @author Paul Ferraro
 */
public class DistributableSessionIdentifierCodecServiceConfigurator extends SimpleServiceNameProvider implements CapabilityServiceConfigurator, Function<RouteLocator, SessionIdentifierCodec> {

    private final CapabilityServiceConfigurator configurator;
    private final RoutingSupport routing = new SimpleRoutingSupport();

    public DistributableSessionIdentifierCodecServiceConfigurator(ServiceName name, String serverName, String deploymentName, RouteLocatorServiceConfiguratorProvider provider) {
        super(name);
        this.configurator = provider.getRouteLocatorServiceConfigurator(serverName, deploymentName);
    }

    @Override
    public SessionIdentifierCodec apply(RouteLocator locator) {
        return new DistributableSessionIdentifierCodec(locator, this.routing);
    }

    @Override
    public ServiceConfigurator configure(CapabilityServiceSupport support) {
        this.configurator.configure(support);
        return this;
    }

    @Override
    public ServiceBuilder<?> build(ServiceTarget target) {
        this.configurator.build(target).install();

        ServiceBuilder<?> builder = target.addService(this.getServiceName());
        Consumer<SessionIdentifierCodec> codec = builder.provides(this.getServiceName());
        Supplier<RouteLocator> locator = builder.requires(this.configurator.getServiceName());
        Service service = new FunctionalService<>(codec, this, locator);
        return builder.setInstance(service).setInitialMode(ServiceController.Mode.ON_DEMAND);
    }
}
