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

import static java.security.AccessController.doPrivileged;

import java.security.PrivilegedAction;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import io.undertow.servlet.api.Deployment;
import org.jboss.as.server.suspend.ServerActivity;
import org.jboss.as.server.suspend.ServerActivityCallback;
import org.jboss.logging.Logger;
import org.jboss.modcluster.container.Connector;
import org.jboss.modcluster.container.ContainerEventHandler;
import org.jboss.modcluster.container.Context;
import org.jboss.modcluster.container.Engine;
import org.jboss.modcluster.container.Server;
import org.jboss.msc.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;
import org.jboss.threads.JBossThreadFactory;
import org.wildfly.extension.undertow.Host;
import org.wildfly.extension.undertow.UndertowEventListener;
import org.wildfly.extension.undertow.UndertowService;

/**
 * Builds a service exposing an Undertow subsystem adapter to mod_cluster's {@link ContainerEventHandler}.
 *
 * @author Paul Ferraro
 * @author Radoslav Husar
 */
public class UndertowEventHandlerAdapterService implements UndertowEventListener, Service, Runnable, ServerActivity {
    // No logger interface for this module and no reason to create one for this class only
    private static final Logger log = Logger.getLogger("org.jboss.mod_cluster.undertow");

    private final UndertowEventHandlerAdapterConfiguration configuration;
    private final Set<Context> contexts = new HashSet<>();
    private volatile ScheduledExecutorService executor;
    private volatile Server server;
    private volatile Connector connector;
    private volatile String serverName;

    public UndertowEventHandlerAdapterService(UndertowEventHandlerAdapterConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public void start(StartContext context) {
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

        // Start the periodic STATUS thread
        ThreadGroup group = new ThreadGroup(UndertowEventHandlerAdapterService.class.getSimpleName());
        ThreadFactory factory = doPrivileged(new PrivilegedAction<ThreadFactory>() {
            @Override
            public ThreadFactory run() {
                return new JBossThreadFactory(group, Boolean.FALSE, null, "%G - %t", null, null);
            }
        });
        this.executor = Executors.newScheduledThreadPool(1, factory);
        this.executor.scheduleWithFixedDelay(this, 0, this.configuration.getStatusInterval().toMillis(), TimeUnit.MILLISECONDS);
        this.configuration.getSuspendController().registerActivity(this);
    }

    @Override
    public void stop(StopContext context) {
        this.configuration.getSuspendController().unRegisterActivity(this);
        this.configuration.getUndertowService().unregisterListener(this);

        this.executor.shutdownNow();
        try {
            this.executor.awaitTermination(this.configuration.getStatusInterval().toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException ignore) {
            // Move on.
        }

        this.configuration.getContainerEventHandler().stop(this.server);
    }

    private Context createContext(Deployment deployment, Host host) {
        return new UndertowContext(deployment, new UndertowHost(host, new UndertowEngine(serverName, host.getServer().getValue(), this.configuration.getUndertowService(), this.connector)));
    }

    private Context createContext(String contextPath, Host host) {
        return new LocationContext(contextPath, new UndertowHost(host, new UndertowEngine(serverName, host.getServer().getValue(), this.configuration.getUndertowService(), this.connector)));
    }

    private synchronized void onStart(Context context) {
        ContainerEventHandler handler = this.configuration.getContainerEventHandler();
        handler.add(context);

        // TODO break into onDeploymentAdd once implemented in Undertow
        handler.start(context);
        this.contexts.add(context);
    }

    private synchronized void onStop(Context context) {
        ContainerEventHandler handler = this.configuration.getContainerEventHandler();
        handler.stop(context);

        // TODO break into onDeploymentRemove once implemented in Undertow
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
            log.error(e.getMessage(), e);
        }
    }

    @Override
    public synchronized void preSuspend(ServerActivityCallback listener) {
        try {
            for (Context context : this.contexts) {
                this.configuration.getContainerEventHandler().stop(context);
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
        for (Context context : this.contexts) {
            this.configuration.getContainerEventHandler().start(context);
        }
    }
}
