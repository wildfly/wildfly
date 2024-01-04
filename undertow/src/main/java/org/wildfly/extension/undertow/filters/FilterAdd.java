/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow.filters;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.extension.undertow.UndertowService;

import java.util.Collection;
import java.util.function.Consumer;

import io.undertow.server.HandlerWrapper;

/**
 * @author Tomaz Cerar (c) 2013 Red Hat Inc.
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
class FilterAdd extends AbstractAddStepHandler {

    private HandlerWrapperFactory factory;

    FilterAdd(HandlerWrapperFactory factory, Collection<AttributeDefinition> attributes) {
        super(attributes);
        this.factory = factory;
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        final String name = context.getCurrentAddressValue();
        final ServiceTarget target = context.getServiceTarget();
        final ServiceBuilder<?> sb = target.addService(UndertowService.FILTER.append(name));
        final Consumer<HandlerWrapper> serviceConsumer = sb.provides(UndertowService.FILTER.append(name));
        HandlerWrapper wrapper = this.factory.createHandlerWrapper(context, model);
        sb.setInstance(Service.newInstance(serviceConsumer, wrapper));
        sb.setInitialMode(ServiceController.Mode.ON_DEMAND);
        sb.install();
    }
}
