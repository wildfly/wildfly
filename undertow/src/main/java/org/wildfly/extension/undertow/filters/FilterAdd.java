/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow.filters;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.CapabilityServiceBuilder;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceController;
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
        final CapabilityServiceBuilder<?> csb = context.getCapabilityServiceTarget().addCapability(FilterCapabilities.FILTER_CAPABILITY.getDefinition());
        final Consumer<PredicateHandlerWrapper> serviceConsumer =  csb.provides(FilterCapabilities.FILTER_CAPABILITY.getDefinition(), UndertowService.FILTER.append(name));
        final PredicateHandlerWrapper wrapper = this.factory.createHandlerWrapper(context, model);
        csb.setInstance(Service.newInstance(serviceConsumer, wrapper));
        csb.setInitialMode(ServiceController.Mode.ON_DEMAND);
        csb.install();
    }
}