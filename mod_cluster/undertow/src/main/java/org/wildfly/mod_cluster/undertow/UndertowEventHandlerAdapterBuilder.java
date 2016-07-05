/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.wildfly.mod_cluster.undertow;

import io.undertow.servlet.api.Deployment;

import java.security.PrivilegedAction;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.jboss.as.clustering.controller.CapabilityServiceBuilder;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.as.server.suspend.ServerActivity;
import org.jboss.as.server.suspend.ServerActivityCallback;
import org.jboss.as.server.suspend.SuspendController;
import org.jboss.logging.Logger;
import org.jboss.modcluster.container.Connector;
import org.jboss.modcluster.container.ContainerEventHandler;
import org.jboss.modcluster.container.Context;
import org.jboss.modcluster.container.Engine;
import org.jboss.modcluster.container.Server;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.threads.JBossThreadFactory;
import org.wildfly.extension.undertow.ListenerService;
import org.wildfly.clustering.service.AsynchronousServiceBuilder;
import org.wildfly.clustering.service.Builder;
import org.wildfly.clustering.service.InjectedValueDependency;
import org.wildfly.clustering.service.ValueDependency;
import org.wildfly.clustering.web.DefaultServerRequirement;
import org.wildfly.extension.mod_cluster.ContainerEventHandlerService;
import org.wildfly.extension.undertow.Host;
import org.wildfly.extension.undertow.UndertowEventListener;
import org.wildfly.extension.undertow.UndertowService;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * Builds a service exposing an Undertow subsystem adapter to mod_cluster's ContainerEventHandler.
 *
 * @author Paul Ferraro
 */
public class UndertowEventHandlerAdapterBuilder implements CapabilityServiceBuilder<Void>, UndertowEventListener, Service<Void>, Runnable, ServerActivity {
    static final ServiceName SERVICE_NAME = ContainerEventHandlerService.SERVICE_NAME.append("undertow");
    // No logger interface for this module and no reason to create one for this class only
    private static final Logger log = Logger.getLogger("org.jboss.mod_cluster.undertow");

    private final InjectedValue<UndertowService> service = new InjectedValue<>();
    private final InjectedValue<ContainerEventHandler> eventHandler = new InjectedValue<>();
    private final InjectedValue<SuspendController> suspendController = new InjectedValue<>();
    @SuppressWarnings("rawtypes")
    private final ValueDependency<ListenerService> listener;
    private final Duration statusInterval;
    private final Set<Context> contexts = new HashSet<>();
    private volatile ValueDependency<String> route;
    private volatile ScheduledExecutorService executor;
    private volatile Server server;
    private volatile Connector connector;

    public UndertowEventHandlerAdapterBuilder(String connector, Duration statusInterval) {
        this.listener = new InjectedValueDependency<>(UndertowService.listenerName(connector), ListenerService.class);
        this.statusInterval = statusInterval;
    }

    @Override
    public ServiceName getServiceName() {
        return SERVICE_NAME;
    }

    @Override
    public Builder<Void> configure(CapabilityServiceSupport support) {
        // TODO We don't have a capability reference for this yet
        // This will eventually use a server-specific route. See WFLY-6803
        this.route = new InjectedValueDependency<>(DefaultServerRequirement.ROUTE.getServiceName(support), String.class);
        return this;
    }

    @Override
    public ServiceBuilder<Void> build(ServiceTarget target) {
        return this.listener.register(new AsynchronousServiceBuilder<>(SERVICE_NAME, this).build(target))
                .addDependency(ContainerEventHandlerService.SERVICE_NAME, ContainerEventHandler.class, this.eventHandler)
                .addDependency(UndertowService.UNDERTOW, UndertowService.class, this.service)
                .addDependency(SuspendController.SERVICE_NAME, SuspendController.class, this.suspendController)
        ;
    }

    @Override
    public Void getValue() {
        return null;
    }

    @Override
    public void start(StartContext context) {
        UndertowService service = this.service.getValue();
        ContainerEventHandler eventHandler = this.eventHandler.getValue();
        this.connector = new UndertowConnector(this.listener.getValue());
        this.server = new UndertowServer(service, this.connector, this.route.getValue());

        // Register ourselves as a listener to the container events
        service.registerListener(this);

        // Initialize mod_cluster and start it now
        eventHandler.init(this.server);
        eventHandler.start(this.server);

        // Start the periodic STATUS thread
        ThreadGroup group = new ThreadGroup(UndertowEventHandlerAdapterBuilder.class.getSimpleName());
        PrivilegedAction<ThreadFactory> action = () -> new JBossThreadFactory(group, Boolean.FALSE, null, "%G - %t", null, null);
        ThreadFactory factory = WildFlySecurityManager.doUnchecked(action);
        this.executor = Executors.newScheduledThreadPool(1, factory);
        this.executor.scheduleWithFixedDelay(this, 0, this.statusInterval.getSeconds(), TimeUnit.SECONDS);
        suspendController.getValue().registerActivity(this);
    }

    @Override
    public void stop(StopContext context) {
        suspendController.getValue().unRegisterActivity(this);
        this.service.getValue().unregisterListener(this);

        this.executor.shutdown();

        ContainerEventHandler eventHandler = this.eventHandler.getValue();
        eventHandler.stop(this.server);
    }

    private Context createContext(Deployment deployment, Host host) {
        return new UndertowContext(deployment, new UndertowHost(host, new UndertowEngine(this.service.getValue(), host.getServer().getValue(), this.connector, this.route.getValue())));
    }

    @Override
    public synchronized void onDeploymentStart(Deployment deployment, Host host) {
        Context context = this.createContext(deployment, host);
        this.eventHandler.getValue().add(context);

        // TODO break into onDeploymentAdd once implemented in Undertow
        this.eventHandler.getValue().start(context);
        contexts.add(context);
    }

    @Override
    public synchronized void onDeploymentStop(Deployment deployment, Host host) {
        Context context = this.createContext(deployment, host);
        this.eventHandler.getValue().stop(context);

        // TODO break into onDeploymentRemove once implemented in Undertow
        this.eventHandler.getValue().remove(context);
        contexts.remove(context);
    }

    @Override
    public void run() {
        try {
            for (Engine engine : this.server.getEngines()) {
                this.eventHandler.getValue().status(engine);
            }
        } catch (Throwable e) {
            log.error(e.getMessage(), e);
        }
    }

    @Override
    public synchronized void preSuspend(ServerActivityCallback listener) {
        try {
            for (Context context : contexts) {
                this.eventHandler.getValue().stop(context);
            }
        } finally {
            listener.done();
        }
    }

    @Override
    public void suspended(ServerActivityCallback listener) {
        listener.done();
    }

    @Override
    public void resume() {
        for (Context context : contexts) {
            this.eventHandler.getValue().start(context);
        }
    }
}
