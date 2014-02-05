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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

import io.undertow.Handlers;
import io.undertow.security.api.AuthenticationMechanism;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.PathHandler;
import io.undertow.servlet.api.Deployment;
import io.undertow.servlet.api.DeploymentInfo;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.extension.undertow.filters.FilterRef;
import org.wildfly.extension.undertow.logging.UndertowLogger;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2013 Red Hat Inc.
 * @author Radoslav Husar
 */
public class Host implements Service<Host> {
    private final PathHandler pathHandler = new PathHandler();
    private volatile HttpHandler rootHandler = pathHandler;
    private final Set<String> allAliases;
    private final String name;
    private final String defaultWebModule;
    private final InjectedValue<Server> server = new InjectedValue<>();
    private final InjectedValue<UndertowService> undertowService = new InjectedValue<>();
    private final InjectedValue<AccessLogService> accessLogService = new InjectedValue<>();
    private final List<InjectedValue<FilterRef>> filters = new CopyOnWriteArrayList<>();
    private final Set<Deployment> deployments = new CopyOnWriteArraySet<>();
    private final Map<String, AuthenticationMechanism> additionalAuthenticationMechanisms = new ConcurrentHashMap<>();

    protected Host(String name, List<String> aliases, String defaultWebModule) {
        this.name = name;
        this.defaultWebModule = defaultWebModule;
        Set<String> hosts = new HashSet<>(aliases.size() + 1);
        hosts.add(name);
        hosts.addAll(aliases);
        allAliases = Collections.unmodifiableSet(hosts);
    }

    private String getDeployedContextPath(DeploymentInfo deploymentInfo) {
        return "".equals(deploymentInfo.getContextPath()) ? "/" : deploymentInfo.getContextPath();
    }

    @Override
    public void start(StartContext context) throws StartException {
        rootHandler = configureRootHandler();
        server.getValue().registerHost(this);
        UndertowLogger.ROOT_LOGGER.hostStarting(name);
    }

    private HttpHandler configureRootHandler() {
        AccessLogService logService = accessLogService.getOptionalValue();
        if (logService != null) {
            rootHandler = logService.configureAccessLogHandler(pathHandler);
        }
        ArrayList<FilterRef> filters = new ArrayList<>(this.filters.size());
        for (InjectedValue<FilterRef> injectedFilter : this.filters) {
            filters.add(injectedFilter.getValue());
        }

        //handle requests that use the Expect: 100-continue header
        rootHandler = Handlers.httpContinueRead(rootHandler);
        //we always need to add date header
        //commented out for now as it causes issues with restEasy
        //rootHandler = Handlers.date(rootHandler);
        Collections.reverse(filters);
        HttpHandler handler = rootHandler;
        for (FilterRef filter : filters) {
            handler = filter.createHttpHandler(handler);
        }
        return handler;
    }

    @Override
    public void stop(StopContext context) {
        server.getValue().unregisterHost(this);
        pathHandler.clearPaths();
        UndertowLogger.ROOT_LOGGER.hostStopping(name);
    }

    @Override
    public Host getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    protected InjectedValue<Server> getServerInjection() {
        return server;
    }

    protected InjectedValue<AccessLogService> getAccessLogService() {
        return accessLogService;
    }

    public Server getServer() {
        return server.getValue();
    }

    protected InjectedValue<UndertowService> getUndertowService() {
        return undertowService;
    }

    public Set<String> getAllAliases() {
        return allAliases;
    }

    public String getName() {
        return name;
    }

    protected HttpHandler getRootHandler() {
        return rootHandler;
    }

    public String getDefaultWebModule() {
        return defaultWebModule;
    }

    public void registerDeployment(final Deployment deployment, HttpHandler handler) {
        DeploymentInfo deploymentInfo = deployment.getDeploymentInfo();
        String path = getDeployedContextPath(deploymentInfo);
        registerHandler(path, handler);
        deployments.add(deployment);
        UndertowLogger.ROOT_LOGGER.registerWebapp(path);
        undertowService.getValue().fireEvent(new EventInvoker() {
            @Override
            public void invoke(UndertowEventListener listener) {
                listener.onDeploymentStart(deployment, Host.this);
            }
        });
    }

    public void unregisterDeployment(final Deployment deployment) {
        DeploymentInfo deploymentInfo = deployment.getDeploymentInfo();
        String path = getDeployedContextPath(deploymentInfo);
        undertowService.getValue().fireEvent(new EventInvoker() {
            @Override
            public void invoke(UndertowEventListener listener) {
                listener.onDeploymentStop(deployment, Host.this);
            }
        });
        unregisterHandler(path);
        deployments.remove(deployment);
        UndertowLogger.ROOT_LOGGER.unregisterWebapp(path);
    }

    public void registerHandler(String path, HttpHandler handler) {
        pathHandler.addPrefixPath(path, handler);
    }

    public void unregisterHandler(String path) {
        pathHandler.removePrefixPath(path);
    }

    /**
     * @return set of currently registered {@link Deployment}s on this host
     */
    public Set<Deployment> getDeployments() {
        return Collections.unmodifiableSet(deployments);
    }

    List<InjectedValue<FilterRef>> getFilters() {
        return filters;
    }

    void registerAdditionalAuthenticationMechanism(String name, AuthenticationMechanism authenticationMechanism){
        additionalAuthenticationMechanisms.put(name, authenticationMechanism);
    }

    void unregisterAdditionalAuthenticationMechanism(String name){
        additionalAuthenticationMechanisms.remove(name);
    }

    public Map<String, AuthenticationMechanism> getAdditionalAuthenticationMechanisms() {
        return new LinkedHashMap<>(additionalAuthenticationMechanisms);
    }
}
