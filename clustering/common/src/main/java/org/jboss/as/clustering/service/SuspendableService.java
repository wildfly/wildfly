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
import org.wildfly.clustering.server.service.Service;

/**
 * A {@link Service} decorator that auto-stops on server suspend and auto-starts on server resume.
 * @author Paul Ferraro
 */
public class SuspendableService implements Service, SuspendableActivity {

    private final Service service;
    private final SuspendableActivityRegistry registry;
    private final Executor executor;

    public SuspendableService(Service service, SuspendableActivityRegistry registry, Executor executor) {
        this.service = service;
        this.registry = registry;
        this.executor = executor;
    }

    @Override
    public boolean isStarted() {
        return this.service.isStarted();
    }

    @Override
    public void start() {
        this.registry.registerActivity(this);
        // If we are suspended, defer start until SuspendableActivity.resume(...)
        if (this.registry.getState() == ServerSuspendController.State.RUNNING) {
            this.service.start();
        }
    }

    @Override
    public void stop() {
        // If we are suspended, SuspendableActivity.suspend(...) will have already stopped the service
        if (this.registry.getState() == ServerSuspendController.State.RUNNING) {
            this.service.stop();
        }
        this.registry.unregisterActivity(this);
    }

    @Override
    public CompletionStage<Void> suspend(ServerSuspendContext context) {
        return this.service.isStarted() ? CompletableFuture.runAsync(this.service::stop, this.executor) : SuspendableActivity.COMPLETED;
    }

    @Override
    public CompletionStage<Void> resume(ServerResumeContext context) {
        return !this.service.isStarted() ? CompletableFuture.runAsync(this.service::start, this.executor) : SuspendableActivity.COMPLETED;
    }
}
