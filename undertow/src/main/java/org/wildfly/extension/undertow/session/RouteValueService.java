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

import org.jboss.msc.service.AbstractService;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.extension.undertow.UndertowService;

/**
 * Service that exposes instance id of the server as the route.
 * @author Paul Ferraro
 */
public class RouteValueService extends AbstractService<RouteValue> {

    public static final ServiceName SERVICE_NAME = UndertowService.SERVER.append("route");

    public static ServiceBuilder<RouteValue> build(ServiceTarget target) {
        RouteValueService service = new RouteValueService();
        return target.addService(SERVICE_NAME, service)
                .addDependency(UndertowService.UNDERTOW, UndertowService.class, service.service)
        ;
    }

    private final InjectedValue<UndertowService> service = new InjectedValue<>();

    private RouteValueService() {
        // Hide
    }

    @Override
    public RouteValue getValue() {
        return new RouteValue(this.service.getValue());
    }
}
