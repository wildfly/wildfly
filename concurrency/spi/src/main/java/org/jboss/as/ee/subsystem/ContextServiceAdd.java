/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ee.subsystem;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.ee.concurrent.ContextServiceTypesConfiguration;
import org.jboss.as.ee.concurrent.service.ContextServiceService;
import org.jboss.dmr.ModelNode;

/**
 * @author Eduardo Martins
 */
public class ContextServiceAdd extends AbstractAddStepHandler {

    static final ContextServiceAdd INSTANCE = new ContextServiceAdd();

    private ContextServiceAdd() {
        super(ContextServiceResourceDefinition.ATTRIBUTES);
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, Resource resource) throws OperationFailedException {
        final ModelNode model = resource.getModel();
        final String name = context.getCurrentAddressValue();
        final String jndiName = ContextServiceResourceDefinition.JNDI_NAME_AD.resolveModelAttribute(context, model).asString();
        // WFLY-16705 deprecated USE_TRANSACTION_SETUP_PROVIDER_AD is ignored since it's of no use anymore (replaced by spec's context service config of context type Transaction)
        // TODO https://issues.redhat.com/browse/WFLY-17912 -- allow Context Service configuration via the subsystem management API
        // install the service which manages the default context service
        final ContextServiceService contextServiceService = new ContextServiceService(name, jndiName, ContextServiceTypesConfiguration.DEFAULT);
        context.getCapabilityServiceTarget().addCapability(ContextServiceResourceDefinition.CAPABILITY).setInstance(contextServiceService).install();
    }
}
