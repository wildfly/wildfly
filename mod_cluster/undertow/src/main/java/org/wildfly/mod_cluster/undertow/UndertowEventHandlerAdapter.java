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

import java.security.AccessController;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.jboss.modcluster.container.Connector;
import org.jboss.modcluster.container.ContainerEventHandler;
import org.jboss.modcluster.container.Context;
import org.jboss.modcluster.container.Engine;
import org.jboss.modcluster.container.Server;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.Value;
import org.jboss.threads.JBossThreadFactory;
import org.wildfly.extension.undertow.AbstractListenerService;
import org.wildfly.extension.undertow.Host;
import org.wildfly.extension.undertow.UndertowEventListener;
import org.wildfly.extension.undertow.UndertowService;
import org.wildfly.security.manager.GetAccessControlContextAction;

/**
 * Service exposing an Undertow subsystem adapter to mod_cluster's ContainerEventHandler.
 *
 * @author Paul Ferraro
 */
public class UndertowEventHandlerAdapter implements UndertowEventListener, Service<Void>, Runnable {
    @SuppressWarnings("rawtypes")
    private final Value<AbstractListenerService> listener;
    private final Value<UndertowService> service;
    private final Value<ContainerEventHandler> eventHandler;
    private volatile ScheduledExecutorService executor;
    private volatile Server server;
    private volatile Connector connector;

    public UndertowEventHandlerAdapter(Value<ContainerEventHandler> eventHandler, Value<UndertowService> service, @SuppressWarnings("rawtypes") Value<AbstractListenerService> listener) {
        this.eventHandler = eventHandler;
        this.service = service;
        this.listener = listener;
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
        this.server = new UndertowServer(service, connector);

        // Register ourselves as a listener to the container events
        service.registerListener(this);

        // Initialize mod_cluster and start it now
        eventHandler.init(server);
        eventHandler.start(server);

        // Start the periodic STATUS thread
        ThreadGroup group = new ThreadGroup(UndertowEventHandlerAdapter.class.getSimpleName());
        ThreadFactory factory = new JBossThreadFactory(group, Boolean.FALSE, null, "%G - %t", null, null, AccessController.doPrivileged(GetAccessControlContextAction.getInstance()));
        this.executor = Executors.newScheduledThreadPool(1, factory);
        // TODO make interval configurable
        this.executor.scheduleWithFixedDelay(this, 0, 10, TimeUnit.SECONDS);
    }

    @Override
    public void stop(StopContext context) {
        this.executor.shutdown();
        ContainerEventHandler eventHandler = this.eventHandler.getValue();
        eventHandler.stop(this.server);
    }

    private Context createContext(Deployment deployment, Host host) {
        return new UndertowContext(deployment, new UndertowHost(host, new UndertowEngine(host.getServer().getValue(), this.service.getValue(), this.connector)));
    }

    @Override
    public void onDeploymentStart(Deployment deployment, Host host) {
        Context context = this.createContext(deployment, host);
        this.eventHandler.getValue().add(context);

        // TODO break into onDeploymentAdd once implemented in Undertow
        this.eventHandler.getValue().start(context);
    }

    @Override
    public void onDeploymentStop(Deployment deployment, Host host) {
        Context context = this.createContext(deployment, host);
        this.eventHandler.getValue().stop(context);

        // TODO break into onDeploymentRemove once implemented in Undertow
        this.eventHandler.getValue().remove(context);
    }

    @Override
    public void onShutdown() {
        // Do nothing
    }

    @Override
    public void onHostStart(Host host) {
        // Do nothing
    }

    @Override
    public void onHostStop(Host host) {
        // Do nothing
    }

    @Override
    public void onServerStart(org.wildfly.extension.undertow.Server server) {
        // Do nothing
    }

    @Override
    public void onServerStop(org.wildfly.extension.undertow.Server server) {
        // Do nothing
    }

    @Override
    public void run() {
        for (Engine engine : this.server.getEngines()) {
            this.eventHandler.getValue().status(engine);
        }
    }
}
