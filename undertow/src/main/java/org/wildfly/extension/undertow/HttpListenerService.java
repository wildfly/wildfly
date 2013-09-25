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

import io.undertow.UndertowOptions;
import io.undertow.server.HandlerWrapper;
import io.undertow.server.HttpHandler;
import io.undertow.server.ListenerRegistry;
import io.undertow.server.OpenListener;
import io.undertow.server.handlers.ChannelUpgradeHandler;
import io.undertow.server.protocol.http.HttpOpenListener;
import org.jboss.as.network.SocketBinding;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.ValueService;
import org.jboss.msc.value.ImmediateValue;
import org.jboss.msc.value.InjectedValue;
import org.xnio.ChannelListener;
import org.xnio.IoUtils;
import org.xnio.OptionMap;
import org.xnio.StreamConnection;
import org.xnio.XnioWorker;
import org.xnio.channels.AcceptingChannel;

/**
 * @author Stuart Douglas
 * @author Tomaz Cerar
 */
public class HttpListenerService extends AbstractListenerService<HttpListenerService> {
    private volatile AcceptingChannel<StreamConnection> server;

    private final ChannelUpgradeHandler httpUpgradeHandler = new ChannelUpgradeHandler();
    protected final InjectedValue<ListenerRegistry> httpListenerRegistry = new InjectedValue<>();
    protected final InjectedValue<WebServerService> commonWebServerService = new InjectedValue<>();

    static final ServiceName HTTP_UPGRADE_REGISTRY = ServiceName.JBOSS.append("http-upgrade-registry");

    private final String serverName;
    private final long maxUploadSize;

    public HttpListenerService(String name, final String serverName, long maxUploadSize) {
        super(name);
        this.serverName = serverName;
        this.maxUploadSize = maxUploadSize;
        listenerHandlerWrappers.add(new HandlerWrapper() {
            @Override
            public HttpHandler wrap(final HttpHandler handler) {
                httpUpgradeHandler.setNonUpgradeHandler(handler);
                return httpUpgradeHandler;
            }
        });
    }

    protected Injector<WebServerService> getCommonWebServerInjector() {
        return this.commonWebServerService;
    }

    @Override
    protected OpenListener createOpenListener() {
        return new HttpOpenListener(getBufferPool().getValue(), OptionMap.create(UndertowOptions.BUFFER_PIPELINED_DATA, true), getBufferSize());
    }

    @Override
    public boolean isSecure() {
        return false;
    }

    @Override
    protected void preStart(final StartContext context) {
        //adds the HTTP upgrade service
        //TODO: have a bit more of a think about how we handle this
        context.getChildTarget().addService(HTTP_UPGRADE_REGISTRY.append(getName()), new ValueService<Object>(new ImmediateValue<Object>(httpUpgradeHandler)))
                .install();
        ListenerRegistry.Listener listener = new ListenerRegistry.Listener(getProtocol(), getName(), serverName, getBinding().getValue().getSocketAddress());
        listener.setContextInformation("socket-binding", getBinding().getValue());
        httpListenerRegistry.getValue().addListener(listener);
    }

    protected void startListening(XnioWorker worker, InetSocketAddress socketAddress, ChannelListener<AcceptingChannel<StreamConnection>> acceptListener)
            throws IOException {
        server = worker.createStreamConnectionServer(socketAddress, acceptListener, OptionMap.builder().addAll(SERVER_OPTIONS).set(UndertowOptions.MAX_ENTITY_SIZE, maxUploadSize).getMap());
        server.resumeAccepts();
        UndertowLogger.ROOT_LOGGER.listenerStarted("HTTP", getName(), socketAddress);
    }

    @Override
    protected void stopListening() {
        server.suspendAccepts();
        UndertowLogger.ROOT_LOGGER.listenerSuspend("HTTP", getName());
        IoUtils.safeClose(server);
        server = null;
        UndertowLogger.ROOT_LOGGER.listenerStopped("HTTP", getName(), getBinding().getValue().getSocketAddress());
        httpListenerRegistry.getValue().removeListener(getName());
    }

    @Override
    public HttpListenerService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    public InjectedValue<ListenerRegistry> getHttpListenerRegistry() {
        return httpListenerRegistry;
    }

    protected String getProtocol() {
        return "http";
    }

    @Override
    public void start(StartContext context) throws StartException {
        super.start(context);
        // update the WebServerService with the http/https port information
        final SocketBinding socketBinding = this.binding.getValue();
        if (isSecure()) {
            commonWebServerService.getValue().setHttpsPort(socketBinding.getAbsolutePort());
        } else {
            commonWebServerService.getValue().setHttpPort(socketBinding.getAbsolutePort());
        }
    }
}
