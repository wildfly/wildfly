/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.jgroups.subsystem;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import org.jboss.as.clustering.controller.ManagementRegistrar;
import org.jboss.as.clustering.controller.Operation;
import org.jboss.as.controller.AbstractRuntimeOnlyHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.LifecycleEvent;
import org.jboss.msc.service.LifecycleListener;
import org.jboss.msc.service.ServiceController;
import org.wildfly.clustering.jgroups.spi.ChannelFactory;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.ServiceInstaller;

/**
 * Operation handler for protocol stack diagnostic runtime operations.
 * @author Paul Ferraro
 */
public class StackOperationHandler extends AbstractRuntimeOnlyHandler implements ManagementRegistrar<ManagementResourceRegistration> {

    private final Map<String, Operation<ChannelFactory>> operations = new HashMap<>();

    public StackOperationHandler() {
        for (Operation<ChannelFactory> operation : EnumSet.allOf(StackOperation.class)) {
            this.operations.put(operation.getName(), operation);
        }
    }

    /* Method is synchronized to avoid duplicate service exceptions if called concurrently */
    @Override
    protected synchronized void executeRuntimeStep(OperationContext context, ModelNode op) throws OperationFailedException {
        String name = op.get(ModelDescriptionConstants.OP).asString();
        Operation<ChannelFactory> operation = this.operations.get(name);
        Function<ChannelFactory, ModelNode> operationFunction = new Function<>() {
            @Override
            public ModelNode apply(ChannelFactory factory) {
                try {
                    return operation.execute(context, op, factory);
                } catch (OperationFailedException e) {
                    throw new RuntimeException(e);
                }
            }
        };
        ServiceDependency<ChannelFactory> factory = ServiceDependency.on(ChannelFactory.SERVICE_DESCRIPTOR, context.getCurrentAddressValue());
        AtomicReference<ModelNode> reference = new AtomicReference<>();
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch removeLatch = new CountDownLatch(1);
        ServiceController<?> controller = ServiceInstaller.builder(operationFunction, factory)
                .withCaptor(reference::set)
                .requires(factory)
                .build()
                .install(context.getCapabilityServiceTarget());
        controller.addListener(new CountDownLifecycleListener(startLatch, EnumSet.of(LifecycleEvent.UP, LifecycleEvent.FAILED)));
        controller.addListener(new CountDownLifecycleListener(removeLatch, EnumSet.of(LifecycleEvent.REMOVED)));
        // Force ChannelFactory service to start
        controller.setMode(ServiceController.Mode.ACTIVE);
        try {
            startLatch.await();
            ModelNode result = reference.get();
            if (result != null) {
                context.getResult().set(result);
            } else {
                context.getFailureDescription().set(controller.getStartException().getLocalizedMessage());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new OperationFailedException(e);
        } finally {
            // Make sure service is removed
            try {
                controller.setMode(ServiceController.Mode.REMOVE);
                removeLatch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            context.completeStep(OperationContext.ResultHandler.NOOP_RESULT_HANDLER);
        }
    }

    @Override
    public void register(ManagementResourceRegistration registration) {
        for (Operation<ChannelFactory> operation : EnumSet.allOf(StackOperation.class)) {
            registration.registerOperationHandler(operation.getDefinition(), this);
        }
    }

    private static class CountDownLifecycleListener implements LifecycleListener {
        private final Set<LifecycleEvent> targetEvents;
        private final CountDownLatch latch;

        CountDownLifecycleListener(CountDownLatch latch, Set<LifecycleEvent> targetEvents) {
            this.targetEvents = targetEvents;
            this.latch = latch;
        }

        @Override
        public void handleEvent(ServiceController<?> controller, LifecycleEvent event) {
            if (this.targetEvents.contains(event)) {
                this.latch.countDown();
            }
        }
    }
}
