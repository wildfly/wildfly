package org.wildfly.extension.undertow.filters;

import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.wildfly.extension.undertow.AbstractHandlerDefinition;

/**
 * @author Tomaz Cerar (c) 2013 Red Hat Inc.
 */
abstract class Filter extends AbstractHandlerDefinition {
    private String name;

    protected Filter(String name) {
        super(name);
        this.name = name;
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        FilterAdd add = new FilterAdd(this);
        registerAddOperation(resourceRegistration, add, OperationEntry.Flag.RESTART_RESOURCE_SERVICES);
        //registerRemoveOperation(resourceRegistration, new ServiceRemoveStepHandler(UndertowService.FILTER, add), OperationEntry.Flag.RESTART_RESOURCE_SERVICES);
        registerRemoveOperation(resourceRegistration, ReloadRequiredRemoveStepHandler.INSTANCE, OperationEntry.Flag.RESTART_RESOURCE_SERVICES);

    }

    @Override
    public String getXmlElementName() {
        return name;
    }
}
