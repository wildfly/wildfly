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

package org.wildfly.extension.undertow.session;

import org.jboss.as.clustering.controller.CapabilityServiceBuilder;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.as.web.session.RoutingSupport;
import org.jboss.as.web.session.SessionIdentifierCodec;
import org.jboss.as.web.session.SimpleRoutingSupport;
import org.jboss.as.web.session.SimpleSessionIdentifierCodec;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.service.Builder;
import org.wildfly.clustering.service.InjectedValueDependency;
import org.wildfly.clustering.service.MappedValueService;
import org.wildfly.clustering.service.ValueDependency;
import org.wildfly.extension.undertow.Capabilities;
import org.wildfly.extension.undertow.Server;

/**
 * Service providing a non-distributable {@link SessionIdentifierCodec} implementation.
 * @author Paul Ferraro
 */
public class SimpleSessionIdentifierCodecBuilder implements CapabilityServiceBuilder<SessionIdentifierCodec> {

    private final ServiceName name;
    private final String serverName;
    private final RoutingSupport routing = new SimpleRoutingSupport();

    private volatile ValueDependency<Server> server;

    public SimpleSessionIdentifierCodecBuilder(ServiceName name, String serverName) {
        this.name = name;
        this.serverName = serverName;
    }

    @Override
    public ServiceName getServiceName() {
        return this.name;
    }

    @Override
    public Builder<SessionIdentifierCodec> configure(CapabilityServiceSupport support) {
        this.server = new InjectedValueDependency<>(support.getCapabilityServiceName(Capabilities.CAPABILITY_SERVER, this.serverName), Server.class);
        return this;
    }

    @Override
    public ServiceBuilder<SessionIdentifierCodec> build(ServiceTarget target) {
        Service<SessionIdentifierCodec> service = new MappedValueService<>(server -> new SimpleSessionIdentifierCodec(this.routing, server.getRoute()), this.server);
        return this.server.register(target.addService(this.name, service)).setInitialMode(ServiceController.Mode.ON_DEMAND);
    }
}
