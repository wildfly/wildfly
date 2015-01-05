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

import java.util.ServiceLoader;

import org.jboss.as.web.session.RoutingSupport;
import org.jboss.as.web.session.SessionIdentifierCodec;
import org.jboss.as.web.session.SimpleRoutingSupport;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.ValueService;
import org.jboss.msc.value.InjectedValue;
import org.jboss.msc.value.Value;
import org.wildfly.clustering.service.Builder;
import org.wildfly.clustering.web.session.RouteLocator;
import org.wildfly.clustering.web.session.RouteLocatorBuilderProvider;
import org.wildfly.extension.undertow.session.RouteValue;
import org.wildfly.extension.undertow.session.RouteValueService;

/**
 * Builds a distributable {@link SessionIdentifierCodec} service.
 * @author Paul Ferraro
 */
public class DistributableSessionIdentifierCodecBuilder implements org.wildfly.extension.undertow.session.DistributableSessionIdentifierCodecBuilder, Value<SessionIdentifierCodec> {

    private static RouteLocatorBuilderProvider load() {
        for (RouteLocatorBuilderProvider provider: ServiceLoader.load(RouteLocatorBuilderProvider.class, RouteLocatorBuilderProvider.class.getClassLoader())) {
            return provider;
        }
        return null;
    }

    private final RouteLocatorBuilderProvider provider;
    private final InjectedValue<RouteLocator> locator = new InjectedValue<>();
    private final RoutingSupport routing = new SimpleRoutingSupport();

    public DistributableSessionIdentifierCodecBuilder() {
        this(load());
    }

    public DistributableSessionIdentifierCodecBuilder(RouteLocatorBuilderProvider provider) {
        this.provider = provider;
    }

    @Override
    public ServiceBuilder<SessionIdentifierCodec> build(ServiceTarget target, ServiceName name, String deploymentName) {
        Builder<RouteLocator> locatorBuilder = this.provider.getRouteLocatorBuilder(deploymentName);
        locatorBuilder.build(target).install();
        return target.addService(name, new ValueService<>(this))
                .addDependency(locatorBuilder.getServiceName(), RouteLocator.class, this.locator)
        ;
    }

    @Override
    public DistributableSessionIdentifierCodec getValue() {
        return new DistributableSessionIdentifierCodec(this.locator.getValue(), this.routing);
    }

    @Override
    public ServiceBuilder<?> buildServerDependency(ServiceTarget target) {
        final InjectedValue<RouteValue> route = new InjectedValue<>();
        return this.provider.getRouteLocatorConfigurationBuilder(route).build(target)
                .addDependency(RouteValueService.SERVICE_NAME, RouteValue.class, route)
                .setInitialMode(ServiceController.Mode.ON_DEMAND);
    }
}
