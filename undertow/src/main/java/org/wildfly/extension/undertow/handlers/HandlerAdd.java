/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow.handlers;

import java.util.function.Consumer;
import java.util.function.Supplier;

import io.undertow.server.HttpHandler;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.CapabilityServiceBuilder;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.wildfly.extension.requestcontroller.RequestController;
import org.wildfly.extension.undertow.Capabilities;

/**
 * @author Tomaz Cerar (c) 2013 Red Hat Inc.
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
final class HandlerAdd extends AbstractAddStepHandler {
    private HandlerFactory factory;

    HandlerAdd(HandlerFactory factory) {
        this.factory = factory;
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        final String name = context.getCurrentAddressValue();
        final boolean capabilityAvailable = context.hasOptionalCapability(Capabilities.REF_REQUEST_CONTROLLER, HandlerDefinition.CAPABILITY.getDynamicName(context.getCurrentAddress()), null);

        final CapabilityServiceBuilder<?> sb = context.getCapabilityServiceTarget().addCapability(HandlerDefinition.CAPABILITY);
        final Consumer<HttpHandler> hhConsumer = sb.provides(HandlerDefinition.CAPABILITY);
        final Supplier<RequestController> rcSupplier = capabilityAvailable ? sb.requiresCapability(Capabilities.REF_REQUEST_CONTROLLER, RequestController.class) : null;
        sb.setInstance(new HandlerService(hhConsumer, rcSupplier, this.factory.createHandler(context, model), name));
        sb.setInitialMode(ServiceController.Mode.ON_DEMAND);
        sb.install();
    }
}
