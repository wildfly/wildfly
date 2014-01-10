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

import io.undertow.server.HandlerWrapper;
import io.undertow.server.HttpHandler;
import io.undertow.server.ListenerRegistry;
import io.undertow.server.OpenListener;
import io.undertow.server.handlers.ChannelUpgradeHandler;
import io.undertow.server.handlers.ProxyPeerAddressHandler;
import io.undertow.server.handlers.SSLHeaderHandler;
import io.undertow.server.protocol.http.HttpOpenListener;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
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
public class HttpListenerService extends ListenerService<HttpListenerService> {
    private volatile AcceptingChannel<StreamConnection> server;

    private final ChannelUpgradeHandler httpUpgradeHandler = new ChannelUpgradeHandler();
    protected final InjectedValue<ListenerRegistry> httpListenerRegistry = new InjectedValue<>();
    static final ServiceName HTTP_UPGRADE_REGISTRY = ServiceName.JBOSS.append("http-upgrade-registry");
    static final String PROTOCOL = "http";

    private final String serverName;

    public HttpListenerService(String name, final String serverName, OptionMap listenerOptions, boolean certificateForwarding, boolean proxyAddressForwarding) {
        super(name, listenerOptions);
        this.serverName = serverName;
        listenerHandlerWrappers.add(new HandlerWrapper() {
            @Override
            public HttpHandler wrap(final HttpHandler handler) {
                httpUpgradeHandler.setNonUpgradeHandler(handler);
                return httpUpgradeHandler;
            }
        });
        if (certificateForwarding) {
            listenerHandlerWrappers.add(new HandlerWrapper() {
                @Override
                public HttpHandler wrap(HttpHandler handler) {
                    return new SSLHeaderHandler(handler);
                }
            });
        }if (proxyAddressForwarding) {
            listenerHandlerWrappers.add(new HandlerWrapper() {
                @Override
                public HttpHandler wrap(HttpHandler handler) {
                    return new ProxyPeerAddressHandler(handler);
                }
            });
        }
    }

    @Override
    protected OpenListener createOpenListener() {
        return new HttpOpenListener(getBufferPool().getValue(), OptionMap.builder().addAll(commonOptions).addAll(listenerOptions).getMap(), getBufferSize());
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
        server = worker.createStreamConnectionServer(socketAddress, acceptListener, commonOptions);
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

    @Override
    protected String getProtocol() {
        return PROTOCOL;
    }
}
