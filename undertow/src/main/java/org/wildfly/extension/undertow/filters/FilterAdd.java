/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow.filters;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.extension.undertow.UndertowService;

import java.util.function.Consumer;

/**
 * @author Tomaz Cerar (c) 2013 Red Hat Inc.
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
class FilterAdd extends AbstractAddStepHandler {

    private PredicateHandlerWrapperFactory factory;

    FilterAdd(PredicateHandlerWrapperFactory factory) {
        this.factory = factory;
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        final String name = context.getCurrentAddressValue();
        final ServiceTarget target = context.getServiceTarget();
        final ServiceBuilder<?> sb = target.addService(UndertowService.FILTER.append(name));
        final Consumer<PredicateHandlerWrapper> serviceConsumer = sb.provides(UndertowService.FILTER.append(name));
        PredicateHandlerWrapper wrapper = this.factory.createHandlerWrapper(context, model);
        sb.setInstance(Service.newInstance(serviceConsumer, wrapper));
        sb.setInitialMode(ServiceController.Mode.ON_DEMAND);
        sb.install();
    }
}
