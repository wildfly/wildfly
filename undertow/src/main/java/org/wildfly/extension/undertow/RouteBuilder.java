/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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
package org.wildfly.extension.undertow;

import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.ValueService;
import org.jboss.msc.value.InjectedValue;
import org.jboss.msc.value.Value;

/**
 * Builds a route for a server.
 * For the default server, the route is just the instance-id.
 * For non-default servers, the route is composed of the server name appended to the instance-id.
 * @author Paul Ferraro
 */
public class RouteBuilder implements Value<String> {

    private static final String DELIMITER = ":";

    private final String serverName;
    private final InjectedValue<UndertowService> service = new InjectedValue<>();

    public RouteBuilder(String serverName) {
        this.serverName = serverName;
    }

    public ServiceBuilder<String> build(ServiceTarget target) {
        return target.addService(ServerDefinition.ROUTE_CAPABILITY.getCapabilityServiceName(this.serverName), new ValueService<>(this))
                .addDependency(UndertowService.UNDERTOW, UndertowService.class, this.service)
                .setInitialMode(ServiceController.Mode.ON_DEMAND)
                ;
    }

    @Override
    public String getValue() {
        UndertowService service = this.service.getValue();
        String instanceId = service.getInstanceId();

        // Don't append server name for the default server
        if (this.serverName.equals(service.getDefaultServer())) return instanceId;

        StringBuilder builder = new StringBuilder(instanceId.length() + DELIMITER.length() + this.serverName.length());
        builder.append(instanceId);
        builder.append(DELIMITER);
        builder.append(this.serverName);
        return builder.toString();
    }
}
