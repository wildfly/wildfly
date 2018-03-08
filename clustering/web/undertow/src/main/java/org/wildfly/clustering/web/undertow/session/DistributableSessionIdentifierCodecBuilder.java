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

import java.util.function.Function;

import org.jboss.as.clustering.controller.CapabilityServiceBuilder;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.as.web.session.RoutingSupport;
import org.jboss.as.web.session.SessionIdentifierCodec;
import org.jboss.as.web.session.SimpleRoutingSupport;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.clustering.service.Builder;
import org.wildfly.clustering.service.MappedValueService;
import org.wildfly.clustering.web.session.RouteLocator;
import org.wildfly.clustering.web.session.RouteLocatorBuilderProvider;

/**
 * Builds a distributable {@link SessionIdentifierCodec} service.
 * @author Paul Ferraro
 */
public class DistributableSessionIdentifierCodecBuilder implements CapabilityServiceBuilder<SessionIdentifierCodec>, Function<RouteLocator, SessionIdentifierCodec> {

    private final ServiceName name;
    private final CapabilityServiceBuilder<RouteLocator> locatorBuilder;
    private final RoutingSupport routing = new SimpleRoutingSupport();

    public DistributableSessionIdentifierCodecBuilder(ServiceName name, String serverName, String deploymentName, RouteLocatorBuilderProvider provider) {
        this.name = name;
        this.locatorBuilder = provider.getRouteLocatorBuilder(serverName, deploymentName);
    }

    @Override
    public SessionIdentifierCodec apply(RouteLocator locator) {
        return new DistributableSessionIdentifierCodec(locator, this.routing);
    }

    @Override
    public ServiceName getServiceName() {
        return this.name;
    }

    @Override
    public Builder<SessionIdentifierCodec> configure(CapabilityServiceSupport support) {
        this.locatorBuilder.configure(support);
        return this;
    }

    @Override
    public ServiceBuilder<SessionIdentifierCodec> build(ServiceTarget target) {
        this.locatorBuilder.build(target).setInitialMode(ServiceController.Mode.ON_DEMAND).install();
        InjectedValue<RouteLocator> locatorValue = new InjectedValue<>();
        Service<SessionIdentifierCodec> service = new MappedValueService<>(this, locatorValue);
        return target.addService(this.name, service)
                .addDependency(this.locatorBuilder.getServiceName(), RouteLocator.class, locatorValue)
                .setInitialMode(ServiceController.Mode.ON_DEMAND)
                ;
    }
}
