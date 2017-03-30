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

package org.wildfly.extension.undertow.handlers;

import io.undertow.server.HttpHandler;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.CapabilityServiceBuilder;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.wildfly.extension.requestcontroller.RequestController;
import org.wildfly.extension.undertow.Capabilities;

/**
 * @author Tomaz Cerar (c) 2013 Red Hat Inc.
 */
class HandlerAdd extends AbstractAddStepHandler {
    private Handler handler;

    HandlerAdd(Handler handler) {
        super(handler.getAttributes());
        this.handler = handler;
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        final String name = context.getCurrentAddressValue();

        final HandlerService service = new HandlerService(handler.createHandler(context, model), name);

        CapabilityServiceBuilder<HttpHandler> builder = context.getCapabilityServiceTarget().addCapability(Handler.CAPABILITY, service)
                .setInitialMode(ServiceController.Mode.ON_DEMAND);
        final RuntimeCapability newCapability = Handler.CAPABILITY.fromBaseCapability(context.getCurrentAddress());
        if (context.hasOptionalCapability(Capabilities.REF_REQUEST_CONTROLLER, newCapability.getName(), null)) {
            builder.addCapabilityRequirement(Capabilities.REF_REQUEST_CONTROLLER, RequestController.class, service.getRequestControllerInjectedValue());
        }

        builder.install();
    }
}
