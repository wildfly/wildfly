/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;
import javax.security.jacc.PolicyContext;
import javax.security.jacc.PolicyContextException;

import io.undertow.Version;
import org.jboss.as.controller.PathAddress;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.security.SecurityConstants;
import org.wildfly.extension.undertow.logging.UndertowLogger;
import org.wildfly.extension.undertow.security.jacc.HttpServletRequestPolicyContextHandler;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2013 Red Hat Inc.
 * @author Stuart Douglas
 */
@SuppressWarnings("ALL")
public class UndertowService implements Service<UndertowService> {

    @Deprecated
    public static final ServiceName UNDERTOW = ServiceName.JBOSS.append("undertow");
    @Deprecated
    public static final ServiceName SERVLET_CONTAINER = UNDERTOW.append(Constants.SERVLET_CONTAINER);
    @Deprecated
    public static final ServiceName SERVER = UNDERTOW.append(Constants.SERVER);
    /**
     * service name under which default server is bound.
     */
    public static final ServiceName DEFAULT_SERVER = UNDERTOW.append("default-server");

    /**
     * service name under which default host of default server is bound.
     */
    public static final ServiceName DEFAULT_HOST = DEFAULT_SERVER.append("default-host");

    public static final ServiceName UNDERTOW_DEPLOYMENT = ServiceName.of("undertow-deployment");
    /**
     * The base name for listener/handler/filter services.
     */
    public static final ServiceName HANDLER = UNDERTOW.append(Constants.HANDLER);
    public static final ServiceName FILTER = UNDERTOW.append(Constants.FILTER);


    /**
     * The base name for web deployments.
     */
    static final ServiceName WEB_DEPLOYMENT_BASE = UNDERTOW.append("deployment");
    private final String defaultContainer;
    private final String defaultServer;
    private final String defaultVirtualHost;
    private final Set<Server> registeredServers = new CopyOnWriteArraySet<>();
    private final List<UndertowEventListener> listeners = Collections.synchronizedList(new LinkedList<UndertowEventListener>());
    private final String instanceId;
    private volatile boolean statisticsEnabled;
    private final Set<Consumer<Boolean>> statisticsChangeListenters = new HashSet<>();

    protected UndertowService(String defaultContainer, String defaultServer, String defaultVirtualHost, String instanceId, boolean statisticsEnabled) {
        this.defaultContainer = defaultContainer;
        this.defaultServer = defaultServer;
        this.defaultVirtualHost = defaultVirtualHost;
        this.instanceId = instanceId;
        this.statisticsEnabled = statisticsEnabled;
    }

    public static ServiceName deploymentServiceName(ServiceName deploymentServiceName) {
        return deploymentServiceName.append(UNDERTOW_DEPLOYMENT);
    }

    /**
     * The old deployment unit service name. This is still registered as an alias, however {{@link #deploymentServiceName(ServiceName)}}
     * should be used instead.
     * @param serverName The server name
     * @param virtualHost The virtual host
     * @param contextPath The context path
     * @return The legacy deployment service alias
     */
    @Deprecated
    public static ServiceName deploymentServiceName(final String serverName, final String virtualHost, final String contextPath) {
        return WEB_DEPLOYMENT_BASE.append(serverName).append(virtualHost).append("".equals(contextPath) ? "/" : contextPath);
    }

    @Deprecated
    public static ServiceName virtualHostName(final String server, final String virtualHost) {
        return SERVER.append(server).append(virtualHost);
    }

    public static ServiceName locationServiceName(final String server, final String virtualHost, final String locationName) {
        return virtualHostName(server, virtualHost).append(Constants.LOCATION, locationName);
    }

    public static ServiceName accessLogServiceName(final String server, final String virtualHost) {
        return virtualHostName(server, virtualHost).append(Constants.ACCESS_LOG);
    }

    public static ServiceName ssoServiceName(final String server, final String virtualHost) {
        return virtualHostName(server, virtualHost).append("single-sign-on");
    }

    public static ServiceName consoleRedirectServiceName(final String server, final String virtualHost) {
        return virtualHostName(server, virtualHost).append("console", "redirect");
    }

    public static ServiceName filterRefName(final String server, final String virtualHost, final String locationName, final String filterName) {
        return virtualHostName(server, virtualHost).append(Constants.LOCATION, locationName).append("filter-ref").append(filterName);
    }

    public static ServiceName filterRefName(final String server, final String virtualHost, final String filterName) {
        return SERVER.append(server).append(virtualHost).append("filter-ref").append(filterName);
    }

    public static ServiceName getFilterRefServiceName(final PathAddress address, String name) {
        final PathAddress oneUp = address.subAddress(0, address.size() - 1);
        final PathAddress twoUp = oneUp.subAddress(0, oneUp.size() - 1);
        final PathAddress threeUp = twoUp.subAddress(0, twoUp.size() - 1);
        ServiceName serviceName;
        if (address.getLastElement().getKey().equals(Constants.FILTER_REF)) {
            if (oneUp.getLastElement().getKey().equals(Constants.HOST)) { //adding reference
                String host = oneUp.getLastElement().getValue();
                String server = twoUp.getLastElement().getValue();
                serviceName = UndertowService.filterRefName(server, host, name);
            } else {
                String location = oneUp.getLastElement().getValue();
                String host = twoUp.getLastElement().getValue();
                String server = threeUp.getLastElement().getValue();
                serviceName = UndertowService.filterRefName(server, host, location, name);
            }
        } else if (address.getLastElement().getKey().equals(Constants.HOST)) {
            String host = address.getLastElement().getValue();
            String server = oneUp.getLastElement().getValue();
            serviceName = UndertowService.filterRefName(server, host, name);
        } else {
            String location = address.getLastElement().getValue();
            String host = oneUp.getLastElement().getValue();
            String server = twoUp.getLastElement().getValue();
            serviceName = UndertowService.filterRefName(server, host, location, name);
        }
        return serviceName;
    }

    @Deprecated
    public static ServiceName listenerName(String listenerName) {
        return UNDERTOW.append(Constants.LISTENER).append(listenerName);
    }

    @Override
    public void start(StartContext context) throws StartException {
        UndertowLogger.ROOT_LOGGER.serverStarting(Version.getVersionString());
        // Register the active request PolicyContextHandler
        try {
            PolicyContext.registerHandler(SecurityConstants.WEB_REQUEST_KEY,
                    new HttpServletRequestPolicyContextHandler(), true);
        } catch (PolicyContextException pce) {
            UndertowLogger.ROOT_LOGGER.failedToRegisterPolicyContextHandler(SecurityConstants.WEB_REQUEST_KEY, pce);
        }
    }

    @Override
    public void stop(StopContext context) {
        // Remove PolicyContextHandler
        Set handlerKeys = PolicyContext.getHandlerKeys();
        handlerKeys.remove(SecurityConstants.WEB_REQUEST_KEY);

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

    public boolean isStatisticsEnabled() {
        return statisticsEnabled;
    }

    public synchronized void setStatisticsEnabled(boolean statisticsEnabled) {
        this.statisticsEnabled = statisticsEnabled;
        for(Consumer<Boolean> listener: statisticsChangeListenters) {
            listener.accept(statisticsEnabled);
        }
    }

    public synchronized void registerStatisticsListener(Consumer<Boolean> listener) {
        statisticsChangeListenters.add(listener);
    }

    public synchronized void unregisterStatisticsListener(Consumer<Boolean> listener) {
        statisticsChangeListenters.remove(listener);
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
        synchronized (listeners) {
            for (UndertowEventListener listener : listeners) {
                invoker.invoke(listener);
            }
        }
    }
}
