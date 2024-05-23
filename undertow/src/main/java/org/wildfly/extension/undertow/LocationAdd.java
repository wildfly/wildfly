/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
    static final LocationAdd INSTANCE = new LocationAdd();

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
        final Supplier<Host> hSupplier = sb.requires(Host.SERVICE_DESCRIPTOR, serverName, hostName);
        sb.setInstance(new LocationService(sConsumer, hhSupplier, hSupplier, name));
        sb.install();
    }
}
