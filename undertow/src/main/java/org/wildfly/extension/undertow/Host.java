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
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.PathHandler;
import io.undertow.servlet.api.Deployment;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.util.CopyOnWriteMap;
import io.undertow.util.Methods;
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
public class Host implements Service<Host>, FilterLocation {
    private final PathHandler pathHandler = new PathHandler();
    private volatile HttpHandler rootHandler = null;
    private final Set<String> allAliases;
    private final String name;
    private final String defaultWebModule;
    private final InjectedValue<Server> server = new InjectedValue<>();
    private final InjectedValue<UndertowService> undertowService = new InjectedValue<>();
    private volatile AccessLogService  accessLogService;
    private final List<FilterRef> filters = new CopyOnWriteArrayList<>();
    private final Set<Deployment> deployments = new CopyOnWriteArraySet<>();
    private final Map<String, LocationService> locations = new CopyOnWriteMap<>();
    private final Map<String, AuthenticationMechanism> additionalAuthenticationMechanisms = new ConcurrentHashMap<>();
    private final HostRootHandler hostRootHandler = new HostRootHandler();

    private final DefaultResponseCodeHandler defaultHandler;
    protected Host(final String name, final List<String> aliases, final String defaultWebModule, final int defaultResponseCode ) {
        this.name = name;
        this.defaultWebModule = defaultWebModule;
        Set<String> hosts = new HashSet<>(aliases.size() + 1);
        hosts.add(name);
        hosts.addAll(aliases);
        allAliases = Collections.unmodifiableSet(hosts);
        this.defaultHandler = new DefaultResponseCodeHandler(defaultResponseCode);
        this.setupDefaultResponseCodeHandler();
    }

    protected Host(final String name, final List<String> aliases, final String defaultWebModule ) {
        this.name = name;
        this.defaultWebModule = defaultWebModule;
        Set<String> hosts = new HashSet<>(aliases.size() + 1);
        hosts.add(name);
        hosts.addAll(aliases);
        this.allAliases = Collections.unmodifiableSet(hosts);
        this.defaultHandler = null;
    }

    private String getDeployedContextPath(DeploymentInfo deploymentInfo) {
        return "".equals(deploymentInfo.getContextPath()) ? "/" : deploymentInfo.getContextPath();
    }

    @Override
    public void start(StartContext context) throws StartException {
        server.getValue().registerHost(this);
        UndertowLogger.ROOT_LOGGER.hostStarting(name);
    }

    private HttpHandler configureRootHandler() {
        AccessLogService logService = accessLogService;
        HttpHandler rootHandler = pathHandler;
        if (logService != null) {
            rootHandler = logService.configureAccessLogHandler(pathHandler);
        }

        ArrayList<FilterRef> filters = new ArrayList<>(this.filters);

        //handle options * requests
        rootHandler = new OptionsHandler(rootHandler);

        //handle requests that use the Expect: 100-continue header
        rootHandler = Handlers.httpContinueRead(rootHandler);

        return LocationService.configureHandlerChain(rootHandler,filters);
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

    void setAccessLogService(AccessLogService service) {
        this.accessLogService = service;
        rootHandler = null;
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
        return hostRootHandler;
    }

    List<FilterRef> getFilters() {
        return Collections.unmodifiableList(filters);
    }

    protected HttpHandler getOrCreateRootHandler() {
        HttpHandler root = rootHandler;
        if(root == null) {
            synchronized (this) {
                root = rootHandler;
                if(root == null) {
                    return rootHandler = configureRootHandler();
                }
            }
        }
        return root;
    }

    public String getDefaultWebModule() {
        return defaultWebModule;
    }

    public void registerDeployment(final Deployment deployment, HttpHandler handler) {
        DeploymentInfo deploymentInfo = deployment.getDeploymentInfo();
        String path = getDeployedContextPath(deploymentInfo);
        registerHandler(path, handler);
        deployments.add(deployment);
        UndertowLogger.ROOT_LOGGER.registerWebapp(path, getServer().getName());
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
        UndertowLogger.ROOT_LOGGER.unregisterWebapp(path, getServer().getName());
    }

    public void registerHandler(String path, HttpHandler handler) {
        pathHandler.addPrefixPath(path, handler);
    }

    public void unregisterHandler(String path) {
        pathHandler.removePrefixPath(path);
        // if there is registered location for given path, serve it from now on
        LocationService location = locations.get(path);
        if (location != null) {
            pathHandler.addPrefixPath(location.getLocationPath(), location.getLocationHandler());
        }
        // else serve the default response code
        else if (path.equals("/")) {
            this.setupDefaultResponseCodeHandler();
        }
    }

    void registerLocation(LocationService location) {
        locations.put(location.getLocationPath(), location);
        registerHandler(location.getLocationPath(), location.getLocationHandler());
    }

    void unregisterLocation(LocationService location) {
        locations.remove(location.getLocationPath());
        unregisterHandler(location.getLocationPath());
    }

    /**
     * @return set of currently registered {@link Deployment}s on this host
     */
    public Set<Deployment> getDeployments() {
        return Collections.unmodifiableSet(deployments);
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


    @Override
    public void addFilterRef(FilterRef filterRef) {
        filters.add(filterRef);
        rootHandler = null;
    }

    @Override
    public void removeFilterRef(FilterRef filterRef) {
        filters.remove(filterRef);
        rootHandler = null;
    }

    protected void setupDefaultResponseCodeHandler(){
        if(this.defaultHandler != null){
            this.registerHandler("/", this.defaultHandler);
        }
    }


    private static final class OptionsHandler implements HttpHandler {

        private final HttpHandler next;

        private OptionsHandler(HttpHandler next) {
            this.next = next;
        }

        @Override
        public void handleRequest(HttpServerExchange exchange) throws Exception {
            if(exchange.getRequestMethod().equals(Methods.OPTIONS) &&
                    exchange.getRelativePath().equals("*")) {
                //handle the OPTIONS requests
                //basically just return an empty response
                exchange.endExchange();
                return;
            }
            next.handleRequest(exchange);
        }
    }

    private class HostRootHandler implements HttpHandler {

        @Override
        public void handleRequest(HttpServerExchange exchange) throws Exception {
            getOrCreateRootHandler().handleRequest(exchange);
        }
    }
}
