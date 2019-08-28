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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

import static java.nio.charset.StandardCharsets.UTF_8;

import org.jboss.as.network.SocketBinding;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.CanonicalPathHandler;
import io.undertow.server.handlers.NameVirtualHostHandler;
import io.undertow.server.handlers.ResponseCodeHandler;
import io.undertow.server.handlers.error.SimpleErrorPageHandler;
import io.undertow.util.Headers;
import org.wildfly.extension.undertow.logging.UndertowLogger;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2013 Red Hat Inc.
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public class Server implements Service<Server> {
    private final String defaultHost;
    private final String name;
    private final NameVirtualHostHandler virtualHostHandler = new NameVirtualHostHandler();
    private final InjectedValue<ServletContainerService> servletContainer = new InjectedValue<>();
    private final InjectedValue<UndertowService> undertowService = new InjectedValue<>();
    private volatile HttpHandler root;
    private final List<ListenerService> listeners = new CopyOnWriteArrayList<>();
    private final Set<Host> hosts = new CopyOnWriteArraySet<>();

    private final HashMap<Integer,Integer> securePortMappings = new HashMap<>();

    protected Server(String name, String defaultHost) {
        this.name = name;
        this.defaultHost = defaultHost;
    }

    @Override
    public void start(StartContext startContext) throws StartException {
        root = virtualHostHandler;
        root = new SimpleErrorPageHandler(root);
        root = new CanonicalPathHandler(root);
        root = new DefaultHostHandler(root);

        UndertowLogger.ROOT_LOGGER.startedServer(name);
        undertowService.getValue().registerServer(this);
    }

    protected void registerListener(ListenerService listener) {
           listeners.add(listener);
           if (!listener.isSecure()) {
               SocketBinding binding = listener.getBinding().get();
               SocketBinding redirectBinding = listener.getRedirectSocket() != null ? listener.getRedirectSocket().get() : null;
               if (redirectBinding!=null) {
                   securePortMappings.put(binding.getAbsolutePort(), redirectBinding.getAbsolutePort());
               }else{
                   securePortMappings.put(binding.getAbsolutePort(), -1);
               }
           }
       }

       protected void unregisterListener(ListenerService listener) {
           listeners.remove(listener);
           if (!listener.isSecure()) {
               SocketBinding binding = listener.getBinding().get();
               securePortMappings.remove(binding.getAbsolutePort());
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

    public int lookupSecurePort(final int unsecurePort) {
        return securePortMappings.get(unsecurePort);
    }

    @Override
    public void stop(StopContext stopContext) {
        undertowService.getValue().unregisterServer(this);
    }

    @Override
    public Server getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    Injector<ServletContainerService> getServletContainerInjector() {
        return servletContainer;
    }

    public ServletContainerService getServletContainer() {
        return servletContainer.getValue();
    }

    protected HttpHandler getRoot() {
        return root;
    }

    protected Injector<UndertowService> getUndertowServiceInjector() {
        return undertowService;
    }

    UndertowService getUndertowService() {
        return undertowService.getValue();
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

    public List<UndertowListener> getListeners() {
        return (List)listeners;
    }

    public String getRoute() {
        final UndertowService service = this.undertowService.getValue();
        final String defaultServerRoute = service.getInstanceId();
        if (service.isObfuscateSessionRoute()) {
            try {
                final MessageDigest md = MessageDigest.getInstance("MD5");
                // salt
                md.update(this.name.getBytes(UTF_8));
                // encode
                final byte[] digestedBytes = md.digest(defaultServerRoute.getBytes(UTF_8));
                final Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding(); // = is not allowed in V0 Cookie
                final String encodedRoute = new String(encoder.encode(digestedBytes), UTF_8);
                UndertowLogger.ROOT_LOGGER.obfuscatedSessionRoute(encodedRoute, defaultServerRoute);
                return encodedRoute;
            } catch (NoSuchAlgorithmException e) {
                UndertowLogger.ROOT_LOGGER.unableToObfuscateSessionRoute(defaultServerRoute, e);
            }
        }
        return this.name.equals(service.getDefaultServer()) ? defaultServerRoute : String.join("-", defaultServerRoute, this.name);
    }

    private final class DefaultHostHandler implements HttpHandler {

        private final HttpHandler next;

        private DefaultHostHandler(HttpHandler next) {
            this.next = next;
        }

        @Override
        public void handleRequest(HttpServerExchange exchange) throws Exception {
            if(!exchange.getRequestHeaders().contains(Headers.HOST)) {
                exchange.getRequestHeaders().put(Headers.HOST, defaultHost + ":" + exchange.getDestinationAddress().getPort());
            }
            next.handleRequest(exchange);
        }
    }
}
