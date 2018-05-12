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

import java.util.function.Consumer;
import java.util.function.Function;

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
import org.wildfly.clustering.service.SimpleServiceNameProvider;
import org.wildfly.clustering.service.SupplierDependency;
import org.wildfly.clustering.web.undertow.UndertowUnaryRequirement;
import org.wildfly.extension.undertow.Server;

/**
 * Builds a service providing the route of a server.
 * @author Paul Ferraro
 */
public class RouteServiceConfigurator extends SimpleServiceNameProvider implements CapabilityServiceConfigurator, Function<Server, String> {

    private final String serverName;

    private volatile SupplierDependency<Server> server;

    public RouteServiceConfigurator(String serverName) {
        super(ServiceName.JBOSS.append("clustering", "web", "route", serverName));
        this.serverName = serverName;
    }

    @Override
    public String apply(Server server) {
        return server.getRoute();
    }

    @Override
    public ServiceConfigurator configure(CapabilityServiceSupport support) {
        this.server = new ServiceSupplierDependency<>(UndertowUnaryRequirement.SERVER.getServiceName(support, this.serverName));
        return this;
    }

    @Override
    public ServiceBuilder<?> build(ServiceTarget target) {
        ServiceBuilder<?> builder = target.addService(this.getServiceName());
        Consumer<String> route = this.server.register(builder).provides(this.getServiceName());
        Service service = new FunctionalService<>(route, this, this.server);
        return builder.setInstance(service).setInitialMode(ServiceController.Mode.ON_DEMAND);
    }
}
