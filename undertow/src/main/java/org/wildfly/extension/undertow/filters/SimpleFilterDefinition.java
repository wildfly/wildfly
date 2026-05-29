/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow.filters;

import org.jboss.as.controller.ResourceRegistration;
import org.jboss.as.controller.ServiceRemoveStepHandler;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.wildfly.extension.undertow.UndertowService;

/**
 * @author Tomaz Cerar (c) 2013 Red Hat Inc.
 */
abstract class SimpleFilterDefinition extends AbstractFilterDefinition {

    private final PredicateHandlerWrapperFactory factory;

    protected SimpleFilterDefinition(ResourceRegistration registration, PredicateHandlerWrapperFactory factory) {
        super(registration);
        this.factory = factory;
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        FilterAdd add = new FilterAdd(this.factory);
        registerAddOperation(resourceRegistration, add, OperationEntry.Flag.RESTART_RESOURCE_SERVICES);
        registerRemoveOperation(resourceRegistration, new ServiceRemoveStepHandler(UndertowService.FILTER, add), OperationEntry.Flag.RESTART_RESOURCE_SERVICES);
    }
}
