/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ee.subsystem;

import jakarta.enterprise.concurrent.ContextService;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.CapabilityServiceBuilder;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.ee.concurrent.WildFlyManagedThreadFactory;
import org.jboss.as.ee.concurrent.WildFlyContextService;
import org.jboss.as.ee.concurrent.service.ManagedThreadFactoryService;
import org.jboss.dmr.ModelNode;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @author Eduardo Martins
 */
public class ManagedThreadFactoryAdd extends AbstractAddStepHandler {

    static final ManagedThreadFactoryAdd INSTANCE = new ManagedThreadFactoryAdd();

    private ManagedThreadFactoryAdd() {
        super(ManagedThreadFactoryResourceDefinition.ATTRIBUTES);
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        final String name = PathAddress.pathAddress(operation.get(ModelDescriptionConstants.ADDRESS)).getLastElement().getValue();

        final String jndiName = ManagedExecutorServiceResourceDefinition.JNDI_NAME_AD.resolveModelAttribute(context, model).asString();
        final int priority = ManagedThreadFactoryResourceDefinition.PRIORITY_AD.resolveModelAttribute(context, model).asInt();

        final CapabilityServiceBuilder serviceBuilder = context.getCapabilityServiceTarget().addCapability(ManagedThreadFactoryResourceDefinition.CAPABILITY);
        String contextService = null;
        if (model.hasDefined(ManagedThreadFactoryResourceDefinition.CONTEXT_SERVICE)) {
            contextService = ManagedThreadFactoryResourceDefinition.CONTEXT_SERVICE_AD.resolveModelAttribute(context, model).asString();
        }
        final Consumer<WildFlyManagedThreadFactory> consumer = serviceBuilder.provides(ManagedThreadFactoryResourceDefinition.CAPABILITY);
        final Supplier<WildFlyContextService> ctxServiceSupplier = contextService != null ? serviceBuilder.requiresCapability(ContextServiceResourceDefinition.CAPABILITY.getName(), ContextService.class, contextService) : null;
        final ManagedThreadFactoryService service = new ManagedThreadFactoryService(consumer, ctxServiceSupplier, name, jndiName, priority);
        serviceBuilder.setInstance(service);
        serviceBuilder.install();
    }
}
