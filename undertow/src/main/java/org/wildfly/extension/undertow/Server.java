/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
import java.util.function.Consumer;
import java.util.function.Supplier;

import static java.nio.charset.StandardCharsets.UTF_8;

import org.jboss.as.network.SocketBinding;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.CanonicalPathHandler;
import io.undertow.server.handlers.NameVirtualHostHandler;
import io.undertow.server.handlers.ResponseCodeHandler;
import io.undertow.server.handlers.error.SimpleErrorPageHandler;
import io.undertow.util.Headers;
import org.wildfly.extension.undertow.logging.UndertowLogger;
import org.wildfly.service.descriptor.UnaryServiceDescriptor;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2013 Red Hat Inc.
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public class Server implements Service<Server> {
    // TODO Extract interface from this class and relocate to an SPI module
    public static final UnaryServiceDescriptor<Server> SERVICE_DESCRIPTOR = UnaryServiceDescriptor.of("org.wildfly.undertow.server", Server.class);

    private final Consumer<Server> serverConsumer;
    private final Supplier<ServletContainerService> servletContainer;
    private final Supplier<UndertowService> undertowService;
    private final String defaultHost;
    private String finalRoute = null;
    private final String name;
    private final NameVirtualHostHandler virtualHostHandler = new NameVirtualHostHandler();
    private final List<ListenerService> listeners = new CopyOnWriteArrayList<>();
    private final Set<Host> hosts = new CopyOnWriteArraySet<>();
    private final HashMap<Integer,Integer> securePortMappings = new HashMap<>();
    private volatile HttpHandler root;

    protected Server(final Consumer<Server> serverConsumer,
                     final Supplier<ServletContainerService> servletContainer,
                     final Supplier<UndertowService> undertowService,
                     final String name, final String defaultHost) {
        this.serverConsumer = serverConsumer;
        this.servletContainer = servletContainer;
        this.undertowService = undertowService;
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
        undertowService.get().registerServer(this);
        serverConsumer.accept(this);
    }

    @Override
    public void stop(final StopContext stopContext) {
        serverConsumer.accept(null);
        undertowService.get().unregisterServer(this);
    }

    @Override
    public Server getValue() {
        return this;
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

    public ServletContainerService getServletContainer() {
        return servletContainer.get();
    }

    protected HttpHandler getRoot() {
        return root;
    }

    UndertowService getUndertowService() {
        return undertowService.get();
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
        if (this.finalRoute != null) {
            return this.finalRoute;
        }
        final UndertowService service = this.undertowService.get();
        final String defaultServerRoute = service.getInstanceId();
        if (service.isObfuscateSessionRoute()) {
            try {
                final MessageDigest md = MessageDigest.getInstance("MD5");
                // salt
                md.update(this.name.getBytes(UTF_8));
                // encode
                final byte[] digestedBytes = md.digest(defaultServerRoute.getBytes(UTF_8));
                final Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding(); // = is not allowed in V0 Cookie
                this.finalRoute = new String(encoder.encode(digestedBytes), UTF_8);
                UndertowLogger.ROOT_LOGGER.obfuscatedSessionRoute(this.finalRoute, defaultServerRoute);
            } catch (NoSuchAlgorithmException e) {
                UndertowLogger.ROOT_LOGGER.unableToObfuscateSessionRoute(defaultServerRoute, e);
            }
        }
        if (this.finalRoute == null ) {
            this.finalRoute = this.name.equals(service.getDefaultServer()) ? defaultServerRoute : String.join("-", defaultServerRoute, this.name);
        }
        return this.finalRoute;
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
