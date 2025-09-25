/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;

import org.jboss.as.server.suspend.ServerResumeContext;
import org.jboss.as.server.suspend.ServerSuspendContext;
import org.jboss.as.server.suspend.ServerSuspendController;
import org.jboss.as.server.suspend.SuspendableActivity;
import org.jboss.as.server.suspend.SuspendableActivityRegistry;
import org.wildfly.clustering.server.manager.Service;

/**
 * A {@link Service} decorator that auto-stops on server suspend and auto-starts on server resume.
 * @author Paul Ferraro
 */
public class SuspendableService implements Service {

    private final Service service;
    private final SuspendableActivityRegistry registry;
    private final SuspendableActivity activity;

    public SuspendableService(Service service, SuspendableActivityRegistry registry, Executor executor) {
        this.service = service;
        this.registry = registry;
        this.activity = new SuspendableActivity() {
            @Override
            public CompletionStage<Void> suspend(ServerSuspendContext context) {
                // Avoid spurious stop on startup during activity registration
                if (context.isStarting()) return SuspendableActivity.COMPLETED;
                return CompletableFuture.runAsync(service::stop, executor);
            }

            @Override
            public CompletionStage<Void> resume(ServerResumeContext context) {
                return CompletableFuture.runAsync(service::start, executor);
            }
        };
    }

    @Override
    public boolean isStarted() {
        return this.service.isStarted();
    }

    @Override
    public void start() {
        this.registry.registerActivity(this.activity);
        // Only start now if we are not suspended
        if (this.registry.getState() == ServerSuspendController.State.RUNNING) {
            this.service.start();
        }
    }

    @Override
    public void stop() {
        // Only stop if we are not already suspended
        if (this.registry.getState() == ServerSuspendController.State.RUNNING) {
            this.service.stop();
        }
        this.registry.unregisterActivity(this.activity);
    }
}
