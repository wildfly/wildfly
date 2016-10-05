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

package org.wildfly.extension.undertow.session;

import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.as.web.session.RoutingSupport;
import org.jboss.as.web.session.SessionIdentifierCodec;
import org.jboss.as.web.session.SimpleRoutingSupport;
import org.jboss.as.web.session.SimpleSessionIdentifierCodec;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.ValueService;
import org.jboss.msc.value.InjectedValue;
import org.jboss.msc.value.Value;
import org.wildfly.extension.undertow.ServerDefinition;

/**
 * Service providing a non-distributable {@link SessionIdentifierCodec} implementation.
 * @author Paul Ferraro
 */
public class SimpleSessionIdentifierCodecBuilder {

    private final ServiceName name;
    private final CapabilityServiceSupport support;
    private final String serverName;
    private final InjectedValue<String> route = new InjectedValue<>();
    private final RoutingSupport routing = new SimpleRoutingSupport();

    public SimpleSessionIdentifierCodecBuilder(ServiceName name, CapabilityServiceSupport support, String serverName) {
        this.name = name;
        this.support = support;
        this.serverName = serverName;
    }

    public ServiceBuilder<SessionIdentifierCodec> build(ServiceTarget target) {
        Value<SessionIdentifierCodec> value = () -> new SimpleSessionIdentifierCodec(this.routing, this.route.getValue());
        return target.addService(this.name, new ValueService<>(value))
                .addDependency(this.support.getCapabilityServiceName(ServerDefinition.ROUTE_CAPABILITY_NAME, this.serverName), String.class, this.route)
                .setInitialMode(ServiceController.Mode.ON_DEMAND)
                ;
    }
}
