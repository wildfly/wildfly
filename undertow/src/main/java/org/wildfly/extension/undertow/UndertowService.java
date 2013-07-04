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

package org.wildfly.extension.undertow;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

import io.undertow.Version;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2013 Red Hat Inc.
 * @author Stuart Douglas
 */
public class UndertowService implements Service<UndertowService> {

    public static final ServiceName UNDERTOW = ServiceName.JBOSS.append("undertow");
    public static final ServiceName SERVLET_CONTAINER = UNDERTOW.append(Constants.SERVLET_CONTAINER);
    public static final ServiceName SERVER = UNDERTOW.append(Constants.SERVER);
    /**
     * The base name for listener/handler/filter services.
     */
    public static final ServiceName LISTENER = UNDERTOW.append(Constants.LISTENER);
    public static final ServiceName HANDLER = UNDERTOW.append(Constants.HANDLER);
    public static final ServiceName FILTER = UNDERTOW.append(Constants.FILTER);
    public static final ServiceName ERROR_HANDLER = UNDERTOW.append(Constants.ERROR_HANDLER);
    /**
     * The base name for web deployments.
     */
    static final ServiceName WEB_DEPLOYMENT_BASE = UNDERTOW.append("deployment");
    private final String defaultContainer;
    private final String defaultServer;
    private final String defaultVirtualHost;
    private final Set<Server> registeredServers = new CopyOnWriteArraySet<>();
    private final List<UndertowEventListener> listeners = new CopyOnWriteArrayList<>();
    private volatile String instanceId;//todo this should be final and no setter should be exposed, currently mod cluster "wants it", this needs to change

    protected UndertowService(String defaultContainer, String defaultServer, String defaultVirtualHost, String instanceId) {
        this.defaultContainer = defaultContainer;
        this.defaultServer = defaultServer;
        this.defaultVirtualHost = defaultVirtualHost;
        this.instanceId = instanceId;
    }

    public static ServiceName deploymentServiceName(final String virtualHost, final String contextPath) {
        return WEB_DEPLOYMENT_BASE.append(virtualHost).append("".equals(contextPath) ? "/" : contextPath);
    }

    public static ServiceName virtualHostName(final String server, final String virtualHost) {
        return SERVER.append(server).append(Constants.HOST).append(virtualHost);
    }

    public static ServiceName locationServiceName(final String server, final String virtualHost, final String locationName) {
        return virtualHostName(server, virtualHost).append(Constants.LOCATION, locationName);
    }

    public static ServiceName consoleRedirectServiceName(final String server, final String virtualHost) {
        return virtualHostName(server, virtualHost).append("console", "redirect");
    }

    @Override
    public void start(StartContext context) throws StartException {
        UndertowLogger.ROOT_LOGGER.serverStarting(Version.getVersionString());
    }

    @Override
    public void stop(StopContext context) {
        UndertowLogger.ROOT_LOGGER.serverStopping(Version.getVersionString());

        fireEvent(new EventInvoker() {
            @Override
            public void invoke(UndertowEventListener listener) {
                listener.onShutdown();
            }
        });
    }

    @Override
    public UndertowService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    protected void registerServer(final Server server) {
        registeredServers.add(server);
        fireEvent(new EventInvoker() {
            @Override
            public void invoke(UndertowEventListener listener) {
                listener.onServerStart(server);
            }
        });
    }

    protected void unregisterServer(final Server server) {
        registeredServers.remove(server);
        fireEvent(new EventInvoker() {
            @Override
            public void invoke(UndertowEventListener listener) {
                listener.onServerStop(server);
            }
        });
    }

    public String getDefaultContainer() {
        return defaultContainer;
    }

    public String getDefaultServer() {
        return defaultServer;
    }

    public String getDefaultVirtualHost() {
        return defaultVirtualHost;
    }

    public Set<Server> getServers() {
        return Collections.unmodifiableSet(registeredServers);
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    /**
     * Registers custom Event listener to server
     *
     * @param listener event listener to register
     */
    public void registerListener(UndertowEventListener listener) {
        this.listeners.add(listener);
    }

    public void unregisterListener(UndertowEventListener listener) {
        this.listeners.remove(listener);
    }

    protected void fireEvent(EventInvoker invoker) {
        for (UndertowEventListener listener : listeners) {
            invoker.invoke(listener);
        }
    }
}
