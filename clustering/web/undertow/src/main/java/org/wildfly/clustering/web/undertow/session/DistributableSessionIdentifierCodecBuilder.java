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

import org.jboss.as.web.session.SessionIdentifierCodec;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.clustering.web.session.RouteLocatorBuilder;
import org.wildfly.clustering.web.session.RouteLocatorBuilderValue;
import org.wildfly.extension.undertow.session.RouteValue;
import org.wildfly.extension.undertow.session.RouteValueService;

/**
 * Builds a distributable {@link SessionIdentifierCodec} service.
 * @author Paul Ferraro
 */
public class DistributableSessionIdentifierCodecBuilder implements org.wildfly.extension.undertow.session.DistributableSessionIdentifierCodecBuilder {

    private final RouteLocatorBuilder builder;

    public DistributableSessionIdentifierCodecBuilder() {
        this(new RouteLocatorBuilderValue().getValue());
    }

    public DistributableSessionIdentifierCodecBuilder(RouteLocatorBuilder builder) {
        this.builder = builder;
    }

    @Override
    public ServiceBuilder<SessionIdentifierCodec> build(ServiceTarget target, ServiceName name, String deploymentName) {
        ServiceName locatorServiceName = name.append("locator");
        this.builder.build(target, locatorServiceName, deploymentName)
                .setInitialMode(ServiceController.Mode.ON_DEMAND)
                .install()
        ;
        return DistributableSessionIdentifierCodecService.build(target, name, locatorServiceName);
    }

    @Override
    public ServiceBuilder<?> buildServerDependency(ServiceTarget target) {
        final InjectedValue<RouteValue> route = new InjectedValue<>();
        return this.builder.buildServerDependency(target, route)
                .addDependency(RouteValueService.SERVICE_NAME, RouteValue.class, route)
        ;
    }
}
