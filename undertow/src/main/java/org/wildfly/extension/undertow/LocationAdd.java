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

package org.wildfly.extension.undertow;

import io.undertow.server.HttpHandler;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2013 Red Hat Inc.
 */
class LocationAdd extends AbstractAddStepHandler {
    static LocationAdd INSTANCE = new LocationAdd();

    private LocationAdd() {
        super(LocationDefinition.HANDLER);
    }

    @Override
        protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        final PathAddress hostAddress = context.getCurrentAddress().getParent();
        final PathAddress serverAddress = hostAddress.getParent();
        final String name = context.getCurrentAddressValue();
        final String handler = LocationDefinition.HANDLER.resolveModelAttribute(context, model).asString();

        final LocationService service = new LocationService(name);
        final String serverName = serverAddress.getLastElement().getValue();
        final String hostName = hostAddress.getLastElement().getValue();
        final ServiceName serviceName = UndertowService.locationServiceName(serverName, hostName, name);
        final ServiceBuilder<LocationService> builder = context.getCapabilityServiceTarget().addCapability(LocationDefinition.LOCATION_CAPABILITY, service)
                .addCapabilityRequirement(Capabilities.CAPABILITY_HANDLER, HttpHandler.class, service.getHttpHandler(),handler)
                .addCapabilityRequirement(Capabilities.CAPABILITY_HOST, Host.class, service.getHost(), serverName, hostName);

        builder.setInitialMode(ServiceController.Mode.ACTIVE)
                .addAliases(serviceName)
                .install();
    }
}
