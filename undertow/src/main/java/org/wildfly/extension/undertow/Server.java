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
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.CanonicalPathHandler;
import io.undertow.server.handlers.NameVirtualHostHandler;
import io.undertow.server.handlers.ResponseCodeHandler;
import io.undertow.server.handlers.error.SimpleErrorPageHandler;
import org.jboss.as.network.SocketBinding;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2013 Red Hat Inc.
 */
public class Server implements Service<Server> {
    private final String defaultHost;
    private final String name;
    private final NameVirtualHostHandler virtualHostHandler = new NameVirtualHostHandler();
    private final InjectedValue<ServletContainerService> servletContainer = new InjectedValue<>();
    private final InjectedValue<UndertowService> undertowService = new InjectedValue<>();
    private volatile HttpHandler root;
    private final List<AbstractListenerService<?>> listeners = new LinkedList<>();
    private final Set<Host> hosts = new CopyOnWriteArraySet<>();

    protected Server(String name, String defaultHost) {
        this.name = name;
        this.defaultHost = defaultHost;
    }

    @Override
    public void start(StartContext startContext) throws StartException {
        root = virtualHostHandler;
        root = new SimpleErrorPageHandler(root);
        root = new CanonicalPathHandler(root);

        UndertowLogger.ROOT_LOGGER.startedServer(name);
        undertowService.getValue().registerServer(this);
    }

    protected void registerListener(AbstractListenerService<?> listener) {
        listeners.add(listener);
        if (listener.isSecure()) {
            SocketBinding binding = (SocketBinding) listener.getBinding().getValue();
            servletContainer.getValue().registerSecurePort(listener.getName(), binding.getSocketAddress().getPort());
        }
    }

    protected void unregisterListener(AbstractListenerService<?> listener) {
        listeners.remove(listener);
        if (listener.isSecure()) {
            servletContainer.getValue().unregisterSecurePort(listener.getName());
        }
    }

    protected void registerHost(final Host host) {
        hosts.add(host);
        for (String hostName : host.getAllAliases()) {
            virtualHostHandler.addHost(hostName, host.getRootHandler());
        }
        if (host.getName().equals(getDefaultHost())) {
            virtualHostHandler.setDefaultHandler(host.getRootHandler());
        }
    }

    protected void unregisterHost(Host host) {
        for (String hostName : host.getAllAliases()) {
            virtualHostHandler.removeHost(hostName);
            hosts.remove(host);
        }
        if (host.getName().equals(getDefaultHost())) {
            virtualHostHandler.setDefaultHandler(ResponseCodeHandler.HANDLE_404);
        }
    }

    @Override
    public void stop(StopContext stopContext) {
        undertowService.getValue().unregisterServer(this);
    }

    @Override
    public Server getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    protected InjectedValue<ServletContainerService> getServletContainer() {
        return servletContainer;
    }

    protected HttpHandler getRoot() {
        return root;
    }

    protected InjectedValue<UndertowService> getUndertowService() {
        return undertowService;
    }

    public String getName() {
        return name;
    }

    public String getDefaultHost() {
        return defaultHost;
    }

    public Set<Host> getHosts() {
        return Collections.unmodifiableSet(hosts);
    }

    public List<AbstractListenerService<?>> getListeners() {
        return listeners;
    }
}
