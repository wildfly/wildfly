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

import java.io.IOException;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.net.ssl.SSLContext;

import io.undertow.UndertowOptions;
import io.undertow.protocols.ssl.UndertowXnioSsl;
import io.undertow.connector.ByteBufferPool;
import io.undertow.server.HandlerWrapper;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.OpenListener;
import io.undertow.server.protocol.proxy.ProxyProtocolOpenListener;
import io.undertow.util.StatusCodes;

import org.jboss.as.network.ManagedBinding;
import org.jboss.as.network.SocketBinding;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

import org.wildfly.extension.undertow.logging.UndertowLogger;

import org.xnio.ChannelListener;
import org.xnio.ChannelListeners;
import org.xnio.OptionMap;
import org.xnio.Options;
import org.xnio.StreamConnection;
import org.xnio.XnioWorker;
import org.xnio.channels.AcceptingChannel;

/**
 * @author Tomaz Cerar
 */
public abstract class ListenerService implements Service<UndertowListener>, UndertowListener {

    protected static final OptionMap commonOptions = OptionMap.builder()
            .set(Options.TCP_NODELAY, true)
            .set(Options.REUSE_ADDRESSES, true)
            .set(Options.BALANCING_TOKENS, 1)
            .set(Options.BALANCING_CONNECTIONS, 2)
            .getMap();

    protected final InjectedValue<XnioWorker> worker = new InjectedValue<>();
    protected final InjectedValue<SocketBinding> binding = new InjectedValue<>();
    protected final InjectedValue<SocketBinding> redirectSocket = new InjectedValue<>();
    @SuppressWarnings("rawtypes")
    protected final InjectedValue<ByteBufferPool> bufferPool = new InjectedValue<>();
    protected final InjectedValue<Server> serverService = new InjectedValue<>();
    private final List<HandlerWrapper> listenerHandlerWrappers = new ArrayList<>();

    private final String name;
    protected final OptionMap listenerOptions;
    protected final OptionMap socketOptions;
    protected volatile OpenListener openListener;
    private volatile boolean enabled;
    private volatile boolean started;
    private Consumer<Boolean> statisticsChangeListener;
    private final boolean proxyProtocol;
    private volatile HandlerWrapper stoppingWrapper;

    protected ListenerService(String name, OptionMap listenerOptions, OptionMap socketOptions, boolean proxyProtocol) {
        this.name = name;
        this.listenerOptions = listenerOptions;
        this.socketOptions = socketOptions;
        this.proxyProtocol = proxyProtocol;
    }

    public InjectedValue<XnioWorker> getWorker() {
        return worker;
    }

    public InjectedValue<SocketBinding> getBinding() {
        return binding;
    }

    public InjectedValue<SocketBinding> getRedirectSocket() {
        return redirectSocket;
    }

    @SuppressWarnings("rawtypes")
    public InjectedValue<ByteBufferPool> getBufferPool() {
        return bufferPool;
    }

    public InjectedValue<Server> getServerService() {
        return serverService;
    }

    protected UndertowService getUndertowService() {
        return serverService.getValue().getUndertowService();
    }

    public String getName() {
        return name;
    }

    @Override
    public Server getServer() {
        return this.serverService.getValue();
    }

    public boolean isEnabled() {
        return enabled;
    }

    protected UndertowXnioSsl getSsl() {
        return null;
    }

    protected OptionMap getSSLOptions(SSLContext sslContext) {
        return OptionMap.EMPTY;
    }

    public synchronized void setEnabled(boolean enabled) {
        if(started && enabled != this.enabled) {
            if(enabled) {
                final InetSocketAddress socketAddress = binding.getValue().getSocketAddress();
                final ChannelListener<AcceptingChannel<StreamConnection>> acceptListener = ChannelListeners.openListenerAdapter(openListener);
                try {
                    startListening(worker.getValue(), socketAddress, acceptListener);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } else {
                stopListening();
            }
        }
        this.enabled = enabled;
    }

    public abstract boolean isSecure();

    protected void registerBinding() {
        binding.getValue().getSocketBindings().getNamedRegistry().registerBinding(new ListenerBinding(binding.getValue()));
    }

    protected void unregisterBinding() {
        final SocketBinding binding = this.binding.getValue();
        binding.getSocketBindings().getNamedRegistry().unregisterBinding(binding.getName());
    }

    protected abstract void preStart(StartContext context);

    @Override
    public void start(final StartContext context) throws StartException {
        started = true;
        preStart(context);
        serverService.getValue().registerListener(this);
        try {
            openListener = createOpenListener();
            HttpHandler handler = serverService.getValue().getRoot();
            for(HandlerWrapper wrapper : listenerHandlerWrappers) {
                handler = wrapper.wrap(handler);
            }
            openListener.setRootHandler(handler);
            if(enabled) {
                final InetSocketAddress socketAddress = binding.getValue().getSocketAddress();


                final ChannelListener<AcceptingChannel<StreamConnection>> acceptListener;
                if(proxyProtocol) {
                    UndertowXnioSsl xnioSsl = getSsl();
                    acceptListener = ChannelListeners.openListenerAdapter(new ProxyProtocolOpenListener(openListener, xnioSsl, bufferPool.getValue(), xnioSsl != null ? getSSLOptions(xnioSsl.getSslContext()) : null));
                } else {
                    acceptListener = ChannelListeners.openListenerAdapter(openListener);
                }
                startListening(worker.getValue(), socketAddress, acceptListener);
            }
            registerBinding();
        } catch (IOException e) {
            cleanFailedStart();
            if (e instanceof BindException) {
                final StringBuilder sb = new StringBuilder().append(e.getLocalizedMessage());
                final InetSocketAddress socketAddress = binding.getValue().getSocketAddress();
                if (socketAddress != null)
                    sb.append(" ").append(socketAddress);
                throw new StartException(sb.toString());
            } else {
                throw UndertowLogger.ROOT_LOGGER.couldNotStartListener(name, e);
            }
        }
        statisticsChangeListener = (enabled) -> {
            OptionMap options = openListener.getUndertowOptions();
            OptionMap.Builder builder = OptionMap.builder().addAll(options);
            builder.set(UndertowOptions.ENABLE_STATISTICS, enabled);
            openListener.setUndertowOptions(builder.getMap());
        };
        getUndertowService().registerStatisticsListener(statisticsChangeListener);
        final ServiceContainer container = context.getController().getServiceContainer();
        this.stoppingWrapper = new HandlerWrapper() {
            @Override
            public HttpHandler wrap(HttpHandler handler) {
                return new HttpHandler() {
                    @Override
                    public void handleRequest(HttpServerExchange exchange) throws Exception {
                        //graceful shutdown is handled by the host service, so if the container is actually shutting down there
                        //is a brief window where this will result in a 404 rather than a 503
                        //even without graceful shutdown we start returning 503 once the container has started shutting down
                        if(container.isShutdown()) {
                            exchange.setStatusCode(StatusCodes.SERVICE_UNAVAILABLE);
                            return;
                        }
                        handler.handleRequest(exchange);
                    }
                };
            }
        };
        addWrapperHandler(stoppingWrapper);
    }

    protected abstract void cleanFailedStart();

    @Override
    public void stop(StopContext context) {
        started = false;
        serverService.getValue().unregisterListener(this);
        if(enabled) {
            stopListening();
        }
        unregisterBinding();
        getUndertowService().unregisterStatisticsListener(statisticsChangeListener);
        statisticsChangeListener = null;
        listenerHandlerWrappers.remove(stoppingWrapper);
        stoppingWrapper = null;
    }

    void addWrapperHandler(HandlerWrapper wrapper){
        listenerHandlerWrappers.add(wrapper);
    }

    public OpenListener getOpenListener() {
        return openListener;
    }

    protected abstract OpenListener createOpenListener();

    abstract void startListening(XnioWorker worker, InetSocketAddress socketAddress, ChannelListener<AcceptingChannel<StreamConnection>> acceptListener) throws IOException;

    abstract void stopListening();

    public abstract String getProtocol();

    @Override
    public boolean isShutdown() {
        XnioWorker worker = getWorker().getOptionalValue();
        return worker == null || worker.isShutdown();
    }

    @Override
    public SocketBinding getSocketBinding() {
        return binding.getValue();
    }

    private static class ListenerBinding implements ManagedBinding {

        private final SocketBinding binding;

        private ListenerBinding(final SocketBinding binding) {
            this.binding = binding;
        }

        @Override
        public String getSocketBindingName() {
            return binding.getName();
        }

        @Override
        public InetSocketAddress getBindAddress() {
            return binding.getSocketAddress();
        }

        @Override
        public void close() throws IOException {

        }
    }


}
