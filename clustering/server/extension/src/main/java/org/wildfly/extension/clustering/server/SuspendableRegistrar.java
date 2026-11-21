/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.server;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicReference;

import org.jboss.as.server.suspend.ServerResumeContext;
import org.jboss.as.server.suspend.ServerSuspendContext;
import org.jboss.as.server.suspend.SuspendableActivity;
import org.jboss.as.server.suspend.SuspendableActivityRegistrar;
import org.jboss.as.server.suspend.SuspendableActivityRegistration;
import org.jboss.as.server.suspend.SuspensionStateProvider;
import org.wildfly.clustering.context.Contextualizer;
import org.wildfly.clustering.context.DefaultContextualizerFactory;
import org.wildfly.clustering.function.Consumer;
import org.wildfly.clustering.function.Runner;
import org.wildfly.clustering.function.Supplier;
import org.wildfly.clustering.server.Registrar;
import org.wildfly.clustering.server.Registration;

/**
 * A {@link Registrar} that creates suspendable registrations.
 * @author Paul Ferraro
 */
public class SuspendableRegistrar<R extends Registration> implements Registrar<Supplier<R>> {

    private final SuspendableActivityRegistrar registrar;

    public SuspendableRegistrar(SuspendableActivityRegistrar registrar) {
        this.registrar = registrar;
    }

    @Override
    public Registration register(Supplier<R> registrationFactory) {
        AtomicReference<R> reference = new AtomicReference<>();
        Contextualizer contextualizer = DefaultContextualizerFactory.INSTANCE.createContextualizer(Thread.currentThread().getContextClassLoader());
        Runnable registrationTask = registrationFactory.thenAccept(reference::set);
        Runnable closeTask = Runner.of(reference::get, Consumer.close());
        // Register suspendable activity that closes registration on suspend and re-registers service on resume
        SuspendableActivityRegistration activityRegistration = this.registrar.register(new SuspendableActivity() {
            @Override
            public CompletionStage<Void> suspend(ServerSuspendContext context) {
                return CompletableFuture.runAsync(closeTask);
            }

            @Override
            public CompletionStage<Void> resume(ServerResumeContext context) {
                return CompletableFuture.runAsync(contextualizer.contextualize(registrationTask));
            }
        });
        // If server is not running, defer registration until resume
        if (activityRegistration.getState() == SuspensionStateProvider.State.RUNNING) {
            registrationTask.run();
        }
        return new Registration() {
            @Override
            public void close() {
                try {
                    closeTask.run();
                } finally {
                    activityRegistration.close();
                }
            }
        };
    }
}
