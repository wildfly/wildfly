/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.service.Builder;
import org.wildfly.clustering.service.InjectedValueDependency;
import org.wildfly.clustering.service.MappedValueService;
import org.wildfly.clustering.service.ValueDependency;
import org.wildfly.clustering.web.undertow.UndertowUnaryRequirement;
import org.wildfly.extension.undertow.Server;

/**
 * Builds a service providing the route of a server.
 * @author Paul Ferraro
 */
public class RouteBuilder implements CapabilityServiceBuilder<String>, Function<Server, String> {

    private final String serverName;

    private volatile ValueDependency<Server> server;

    public RouteBuilder(String serverName) {
        this.serverName = serverName;
    }

    @Override
    public String apply(Server server) {
        return server.getRoute();
    }

    @Override
    public ServiceName getServiceName() {
        return ServiceName.JBOSS.append("clustering", "web", "route", this.serverName);
    }

    @Override
    public Builder<String> configure(CapabilityServiceSupport support) {
        this.server = new InjectedValueDependency<>(UndertowUnaryRequirement.SERVER.getServiceName(support, this.serverName), Server.class);
        return this;
    }

    @Override
    public ServiceBuilder<String> build(ServiceTarget target) {
        Service<String> service = new MappedValueService<>(this, this.server);
        return this.server.register(target.addService(this.getServiceName(), service)).setInitialMode(ServiceController.Mode.ON_DEMAND);
    }
}
