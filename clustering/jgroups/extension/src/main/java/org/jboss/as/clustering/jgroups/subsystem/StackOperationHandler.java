/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.jgroups.subsystem;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.jboss.as.controller.AbstractRuntimeOnlyHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.wildfly.clustering.jgroups.spi.ChannelFactory;
import org.wildfly.subsystem.resource.ManagementResourceRegistrar;
import org.wildfly.subsystem.resource.executor.RuntimeOperation;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.ServiceInstaller;

/**
 * Operation handler for protocol stack diagnostic runtime operations.
 * @author Paul Ferraro
 */
public class StackOperationHandler extends AbstractRuntimeOnlyHandler implements ManagementResourceRegistrar {

    private final Map<String, RuntimeOperation<ChannelFactory>> operations = new HashMap<>();

    public StackOperationHandler() {
        for (RuntimeOperation<ChannelFactory> operation : EnumSet.allOf(StackOperation.class)) {
            this.operations.put(operation.getName(), operation);
        }
    }

    @Override
    protected void executeRuntimeStep(OperationContext context, ModelNode op) throws OperationFailedException {
        String name = op.get(ModelDescriptionConstants.OP).asString();
        RuntimeOperation<ChannelFactory> operation = this.operations.get(name);
        CompletableFuture<ChannelFactory> future = new CompletableFuture<>();
        ServiceController<?> controller = ServiceInstaller.builder(ServiceDependency.on(ChannelFactory.SERVICE_DESCRIPTOR, context.getCurrentAddressValue()))
                .withCaptor(future::complete)
                .asActive() // Force ChannelFactory service to start
                .build()
                .install(context.getCapabilityServiceTarget());
        try {
            ChannelFactory channelFactory = future.join();
            context.getResult().set(operation.execute(context, op, channelFactory));
        } catch (Throwable e) {
            context.getFailureDescription().set(e.getLocalizedMessage());
        } finally {
            // Make sure service is removed
            controller.setMode(ServiceController.Mode.REMOVE);
            context.completeStep(OperationContext.ResultHandler.NOOP_RESULT_HANDLER);
        }
    }

    @Override
    public void register(ManagementResourceRegistration registration) {
        for (RuntimeOperation<ChannelFactory> operation : EnumSet.allOf(StackOperation.class)) {
            registration.registerOperationHandler(operation.getOperationDefinition(), this);
        }
    }
}
