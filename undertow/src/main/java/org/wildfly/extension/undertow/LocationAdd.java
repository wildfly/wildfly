/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import java.util.List;

import io.undertow.server.HttpHandler;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.extension.undertow.filters.FilterRef;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2013 Red Hat Inc.
 */
class LocationAdd extends AbstractAddStepHandler {
    static LocationAdd INSTANCE = new LocationAdd();

    private LocationAdd() {
        super(LocationDefinition.HANDLER);
    }

    static <T> void addDep(ServiceBuilder<?> b, ServiceName name, Class<T> type, List<InjectedValue<T>> list) {
        InjectedValue<T> v = new InjectedValue<>();
        b.addDependency(name, type, v);
        list.add(v);
    }

    @Override
        protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        final PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
        final PathAddress hostAddress = address.subAddress(0, address.size() - 1);
        final PathAddress serverAddress = hostAddress.subAddress(0, hostAddress.size() - 1);
        final String name = address.getLastElement().getValue();
        final String handler = LocationDefinition.HANDLER.resolveModelAttribute(context, model).asString();
        final ModelNode fullModel = Resource.Tools.readModel(context.readResource(PathAddress.EMPTY_ADDRESS));

        final LocationService service = new LocationService(name);
        final String serverName = serverAddress.getLastElement().getValue();
        final String hostName = hostAddress.getLastElement().getValue();
        final ServiceName hostServiceName = UndertowService.virtualHostName(serverName, hostName);
        final ServiceName serviceName = UndertowService.locationServiceName(serverName, hostName, name);
        final ServiceBuilder<LocationService> builder = context.getServiceTarget().addService(serviceName, service)
                .addDependency(hostServiceName, Host.class, service.getHost())
                .addDependency(UndertowService.HANDLER.append(handler), HttpHandler.class, service.getHttpHandler());

        configureFilterRef(fullModel, builder, service, address);

        builder.setInitialMode(ServiceController.Mode.ACTIVE)
                .install();
    }

    private static void configureFilterRef(final ModelNode model, ServiceBuilder<LocationService> builder, LocationService service,PathAddress address) {
        if (model.hasDefined(Constants.FILTER_REF)) {
            for (Property property : model.get(Constants.FILTER_REF).asPropertyList()) {
                String name = property.getName();
                addDep(builder, UndertowService.getFilterRefServiceName(address,name), FilterRef.class, service.getFilters());
            }
        }
    }
}
