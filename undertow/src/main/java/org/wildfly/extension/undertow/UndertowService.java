/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;

import org.jboss.as.controller.PathAddress;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.wildfly.extension.undertow.logging.UndertowLogger;
import org.wildfly.service.descriptor.NullaryServiceDescriptor;

import io.undertow.Version;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2013 Red Hat Inc.
 * @author Stuart Douglas
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public class UndertowService implements Service<UndertowService> {
    public static NullaryServiceDescriptor<UndertowService> SERVICE_DESCRIPTOR = NullaryServiceDescriptor.of(Capabilities.CAPABILITY_UNDERTOW, UndertowService.class);

    /**
     * @deprecated Replaced by capability reference {@link UndertowRootDefinition#UNDERTOW_CAPABILITY}.
     */
    @Deprecated(forRemoval = true)
    public static final ServiceName UNDERTOW = UndertowRootDefinition.UNDERTOW_CAPABILITY.getCapabilityServiceName();
    /**
     * @deprecated Replaced by capability reference {@link ServletContainerDefinition#SERVLET_CONTAINER_CAPABILITY}.
     */
    @Deprecated(forRemoval = true)
    public static final ServiceName SERVLET_CONTAINER = UNDERTOW.append(Constants.SERVLET_CONTAINER);
    /**
     * @deprecated Replaced by capability reference {@link HostDefinition.HOST_CAPABILITY}.
     */
    @Deprecated(forRemoval = true)
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
     * @deprecated Replaced by capability reference {@link Capabilities#CAPABILITY_HANDLER}.
     */
    @Deprecated(forRemoval = true)
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
    private final boolean obfuscateSessionRoute;
    private volatile boolean statisticsEnabled;
    private final Set<Consumer<Boolean>> statisticsChangeListenters = new HashSet<>();
    private final Consumer<UndertowService> serviceConsumer;

    protected UndertowService(final Consumer<UndertowService> serviceConsumer, final String defaultContainer,
                              final String defaultServer, final String defaultVirtualHost,
                              final String instanceId, final boolean obfuscateSessionRoute, final boolean statisticsEnabled) {
        this.serviceConsumer = serviceConsumer;
        this.defaultContainer = defaultContainer;
        this.defaultServer = defaultServer;
        this.defaultVirtualHost = defaultVirtualHost;
        this.instanceId = instanceId;
        this.obfuscateSessionRoute = obfuscateSessionRoute;
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
    @Deprecated(forRemoval = true)
    public static ServiceName deploymentServiceName(final String serverName, final String virtualHost, final String contextPath) {
        return WEB_DEPLOYMENT_BASE.append(serverName).append(virtualHost).append("".equals(contextPath) ? "/" : contextPath);
    }

    @Deprecated(forRemoval = true)
    public static ServiceName virtualHostName(final String server, final String virtualHost) {
        return HostDefinition.HOST_CAPABILITY.getCapabilityServiceName(server, virtualHost);
    }

    @Deprecated(forRemoval = true)
    public static ServiceName locationServiceName(final String server, final String virtualHost, final String locationName) {
        return LocationDefinition.LOCATION_CAPABILITY.getCapabilityServiceName(server, virtualHost, locationName);
    }

    @Deprecated(forRemoval = true)
    public static ServiceName accessLogServiceName(final String server, final String virtualHost) {
        return AccessLogDefinition.ACCESS_LOG_CAPABILITY.getCapabilityServiceName(server, virtualHost);
    }

    @Deprecated(forRemoval = true)
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

    @Deprecated(forRemoval = true)
    public static ServiceName listenerName(String listenerName) {
        return ListenerResourceDefinition.LISTENER_CAPABILITY.getCapabilityServiceName(listenerName);
    }

    @Override
    public void start(final StartContext context) throws StartException {
        UndertowLogger.ROOT_LOGGER.serverStarting(Version.getVersionString());

        serviceConsumer.accept(this);
    }

    @Override
    public void stop(final StopContext context) {
        serviceConsumer.accept(null);

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

    public boolean isObfuscateSessionRoute() {
        return obfuscateSessionRoute;
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
