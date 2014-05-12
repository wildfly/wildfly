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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import io.undertow.server.HandlerWrapper;
import io.undertow.server.HttpHandler;
import io.undertow.server.OpenListener;
import org.jboss.as.network.ManagedBinding;
import org.jboss.as.network.SocketBinding;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.xnio.ChannelListener;
import org.xnio.ChannelListeners;
import org.xnio.OptionMap;
import org.xnio.Options;
import org.xnio.Pool;
import org.xnio.StreamConnection;
import org.xnio.XnioWorker;
import org.xnio.channels.AcceptingChannel;

/**
 * @author Tomaz Cerar
 */
public abstract class ListenerService<T> implements Service<T> {

    protected static final OptionMap commonOptions = OptionMap.builder()
            .set(Options.TCP_NODELAY, true)
            .set(Options.REUSE_ADDRESSES, true)
            .set(Options.BALANCING_TOKENS, 1)
            .set(Options.BALANCING_CONNECTIONS, 2)
            .getMap();

    protected final InjectedValue<XnioWorker> worker = new InjectedValue<>();
    protected final InjectedValue<SocketBinding> binding = new InjectedValue<>();
    protected final InjectedValue<SocketBinding> redirectSocket = new InjectedValue<>();
    protected final InjectedValue<Pool> bufferPool = new InjectedValue<>();
    protected final InjectedValue<Server> serverService = new InjectedValue<>();
    protected final List<HandlerWrapper> listenerHandlerWrappers = new ArrayList<>();

    private final String name;
    protected final OptionMap listenerOptions;
    protected final OptionMap socketOptions;
    protected volatile OpenListener openListener;


    protected ListenerService(String name, OptionMap listenerOptions, OptionMap socketOptions) {
        this.name = name;
        this.listenerOptions = listenerOptions;
        this.socketOptions = socketOptions;
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

    public InjectedValue<Pool> getBufferPool() {
        return bufferPool;
    }

    public InjectedValue<Server> getServerService() {
        return serverService;
    }

    protected int getBufferSize() {
        return 8192;
    }

    public String getName() {
        return name;
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
    public void start(StartContext context) throws StartException {
        preStart(context);
        serverService.getValue().registerListener(this);
        try {
            final InetSocketAddress socketAddress = binding.getValue().getSocketAddress();
            openListener = createOpenListener();
            final ChannelListener<AcceptingChannel<StreamConnection>> acceptListener = ChannelListeners.openListenerAdapter(openListener);
            HttpHandler handler = serverService.getValue().getRoot();
            for(HandlerWrapper wrapper : listenerHandlerWrappers) {
                handler = wrapper.wrap(handler);
            }
            openListener.setRootHandler(handler);
            startListening(worker.getValue(), socketAddress, acceptListener);
            registerBinding();
        } catch (IOException e) {
            throw new StartException("Could not start http listener", e);
        }
    }

    @Override
    public void stop(StopContext context) {
        serverService.getValue().unregisterListener(this);
        stopListening();
        unregisterBinding();
    }



    protected abstract OpenListener createOpenListener();

    abstract void startListening(XnioWorker worker, InetSocketAddress socketAddress, ChannelListener<AcceptingChannel<StreamConnection>> acceptListener) throws IOException;

    abstract void stopListening();

    protected abstract String getProtocol();

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
