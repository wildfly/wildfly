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
import org.jboss.as.controller.CapabilityServiceBuilder;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2013 Red Hat Inc.
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
final class LocationAdd extends AbstractAddStepHandler {
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

        final String serverName = serverAddress.getLastElement().getValue();
        final String hostName = hostAddress.getLastElement().getValue();
        final CapabilityServiceBuilder<?> sb = context.getCapabilityServiceTarget().addCapability(LocationDefinition.LOCATION_CAPABILITY);
        final Consumer<LocationService> sConsumer = sb.provides(LocationDefinition.LOCATION_CAPABILITY, UndertowService.locationServiceName(serverName, hostName, name));
        final Supplier<HttpHandler> hhSupplier = sb.requiresCapability(Capabilities.CAPABILITY_HANDLER, HttpHandler.class, handler);
        final Supplier<Host> hSupplier = sb.requiresCapability(Capabilities.CAPABILITY_HOST, Host.class, serverName, hostName);
        sb.setInstance(new LocationService(sConsumer, hhSupplier, hSupplier, name));
        sb.install();
    }
}
