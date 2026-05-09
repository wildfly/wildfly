/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.mod_cluster.undertow;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

import io.undertow.servlet.api.Deployment;
import org.jboss.as.server.suspend.ServerResumeContext;
import org.jboss.as.server.suspend.ServerSuspendContext;
import org.jboss.as.server.suspend.SuspendPriority;
import org.jboss.as.server.suspend.SuspendableActivity;
import org.jboss.as.server.suspend.SuspendableActivityRegistration;
import org.jboss.as.server.suspend.SuspensionStateProvider;
import org.jboss.logging.Logger;
import org.jboss.modcluster.container.Connector;
import org.jboss.modcluster.container.ContainerEventHandler;
import org.jboss.modcluster.container.Context;
import org.jboss.modcluster.container.Engine;
import org.jboss.modcluster.container.Server;
import org.jboss.msc.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;
import org.wildfly.clustering.context.DefaultThreadFactory;
import org.wildfly.extension.undertow.Host;
import org.wildfly.extension.undertow.UndertowEventListener;
import org.wildfly.extension.undertow.UndertowService;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * Builds a service exposing an Undertow subsystem adapter to mod_cluster's {@link ContainerEventHandler}.
 *
 * @author Paul Ferraro
 * @author Radoslav Husar
 */
public class UndertowEventHandlerAdapterService implements UndertowEventListener, Service, Runnable, SuspendableActivity {
    // No logger interface for this module and no reason to create one for this class only
    private static final Logger log = Logger.getLogger("org.jboss.mod_cluster.undertow");
    private static final ThreadFactory THREAD_FACTORY = new DefaultThreadFactory(UndertowEventHandlerAdapterService.class, WildFlySecurityManager.getClassLoaderPrivileged(UndertowEventHandlerAdapterService.class));

    private final UndertowEventHandlerAdapterConfiguration configuration;
    private final Set<Context> contexts = ConcurrentHashMap.newKeySet();
    private volatile ScheduledExecutorService executor;
    private volatile SuspendableActivityRegistration suspendableActivityRegistration;
    private volatile Server server;
    private volatile Connector connector;
    private volatile String serverName;

    public UndertowEventHandlerAdapterService(UndertowEventHandlerAdapterConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public void start(StartContext context) {
        this.executor = Executors.newScheduledThreadPool(1, THREAD_FACTORY);

        this.suspendableActivityRegistration = this.configuration.getSuspendableActivityRegistrar().register(this, SuspendPriority.FIRST);

        UndertowService service = this.configuration.getUndertowService();
        ContainerEventHandler eventHandler = this.configuration.getContainerEventHandler();
        this.connector = new UndertowConnector(this.configuration.getListener());
        this.serverName = this.configuration.getServer().getName();
        this.server = new UndertowServer(this.serverName, service, this.connector);

        // Register ourselves as a listener to the container events
        service.registerListener(this);

        // Initialize mod_cluster and start it now
        eventHandler.init(this.server);
        eventHandler.start(this.server);
        for (Engine engine : this.server.getEngines()) {
            for (org.jboss.modcluster.container.Host host : engine.getHosts()) {
                host.getContexts().forEach(contexts::add);
            }
        }

        // Start the periodic STATUS thread
        this.executor.scheduleWithFixedDelay(this, 0, this.configuration.getStatusInterval().toMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    public void stop(StopContext context) {
        this.configuration.getUndertowService().unregisterListener(this);

        this.suspendableActivityRegistration.close();

        this.executor.shutdownNow();
        try {
            if (!this.executor.awaitTermination(this.configuration.getStatusInterval().toNanos(), TimeUnit.NANOSECONDS)) {
                log.debug("The mod_cluster status executor did not terminate within the status interval while being shut down.");
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }

        this.configuration.getContainerEventHandler().stop(this.server);
    }

    private Context createContext(Deployment deployment, Host host) {
        return new UndertowContext(deployment, new UndertowHost(host, new UndertowEngine(serverName, host.getServer().getValue(), this.configuration.getUndertowService(), this.connector)));
    }

    private Context createContext(String contextPath, Host host) {
        return new LocationContext(contextPath, new UndertowHost(host, new UndertowEngine(serverName, host.getServer().getValue(), this.configuration.getUndertowService(), this.connector)));
    }

    private void onStart(Context context) {
        ContainerEventHandler handler = this.configuration.getContainerEventHandler();

        SuspensionStateProvider.State state = this.suspendableActivityRegistration.getState();

        if (state == SuspensionStateProvider.State.RUNNING) {
            // Normal operation - trigger ENABLE-APP
            handler.start(context);
        } else {
            // Suspended mode - trigger STOP-APP without request nor session draining;
            // n.b. contexts will be started by UndertowEventHandlerAdapterService#resume()
            handler.add(context);
        }

        this.contexts.add(context);
    }

    private void onStop(Context context) {
        ContainerEventHandler handler = this.configuration.getContainerEventHandler();

        // Trigger STOP-APP with possible session draining
        handler.stop(context);

        // Trigger REMOVE-APP
        handler.remove(context);

        this.contexts.remove(context);
    }

    @Override
    public void onDeploymentStart(Deployment deployment, Host host) {
        if (this.filter(host)) {
            this.onStart(this.createContext(deployment, host));
        }
    }

    @Override
    public void onDeploymentStop(Deployment deployment, Host host) {
        if (this.filter(host)) {
            this.onStop(this.createContext(deployment, host));
        }
    }

    @Override
    public void onDeploymentStart(String contextPath, Host host) {
        if (this.filter(host)) {
            this.onStart(this.createContext(contextPath, host));
        }
    }

    @Override
    public void onDeploymentStop(String contextPath, Host host) {
        if (this.filter(host)) {
            this.onStop(this.createContext(contextPath, host));
        }
    }

    public boolean filter(Host host) {
        return host.getServer().getName().equals(serverName);
    }

    @Override
    public void run() {
        try {
            for (Engine engine : this.server.getEngines()) {
                this.configuration.getContainerEventHandler().status(engine);
            }
        } catch (Throwable e) {
            log.error(e.getLocalizedMessage(), e);
        }
    }

    private CompletionStage<Void> forEachContextAsync(BiConsumer<ContainerEventHandler, Context> action) {
        return CompletableFuture.runAsync(() -> this.contexts.forEach(context -> {
            try {
                action.accept(this.configuration.getContainerEventHandler(), context);
            } catch (RuntimeException e) {
                log.error(e.getLocalizedMessage(), e);
            }
        }), this.executor);
    }

    @Override
    public CompletionStage<Void> prepare(ServerSuspendContext serverSuspendContext) {
        return forEachContextAsync(ContainerEventHandler::stop);
    }

    @Override
    public CompletionStage<Void> suspend(ServerSuspendContext serverSuspendContext) {
        return COMPLETED;
    }

    @Override
    public CompletionStage<Void> resume(ServerResumeContext serverResumeContext) {
        return forEachContextAsync(ContainerEventHandler::start);
    }
}
