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
import java.net.InetSocketAddress;
import java.util.function.Consumer;

import io.undertow.UndertowOptions;
import io.undertow.server.ListenerRegistry;
import io.undertow.server.OpenListener;
import io.undertow.server.handlers.ChannelUpgradeHandler;
import io.undertow.server.handlers.ProxyPeerAddressHandler;
import io.undertow.server.handlers.SSLHeaderHandler;
import io.undertow.server.protocol.http.HttpOpenListener;
import io.undertow.server.protocol.http2.Http2UpgradeHandler;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.network.NetworkUtils;
import org.jboss.as.server.deployment.DelegatingSupplier;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.wildfly.extension.undertow.logging.UndertowLogger;
import org.xnio.ChannelListener;
import org.xnio.IoUtils;
import org.xnio.OptionMap;
import org.xnio.StreamConnection;
import org.xnio.XnioWorker;
import org.xnio.channels.AcceptingChannel;

import static org.wildfly.extension.undertow.HttpListenerResourceDefinition.HTTP_UPGRADE_REGISTRY_CAPABILITY;

/**
 * @author Stuart Douglas
 * @author Tomaz Cerar
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public class HttpListenerService extends ListenerService {
    private volatile AcceptingChannel<StreamConnection> server;

    private final ChannelUpgradeHandler httpUpgradeHandler = new ChannelUpgradeHandler();
    /**
     * @deprecated Replaced by HTTP_UPGRADE_REGISTRY.getCapabilityServiceName()
     */
    @Deprecated
    protected final DelegatingSupplier<ListenerRegistry> httpListenerRegistry = new DelegatingSupplier<>();
    static final ServiceName HTTP_UPGRADE_REGISTRY = ServiceName.JBOSS.append("http-upgrade-registry");
    static final String PROTOCOL = "http";

    private final String serverName;
    private final PathAddress address ;

    public HttpListenerService(final Consumer<ListenerService> serviceConsumer, final PathAddress address, final String serverName, OptionMap listenerOptions, OptionMap socketOptions, boolean certificateForwarding, boolean proxyAddressForwarding, boolean proxyProtocol) {
        super(serviceConsumer, address.getLastElement().getValue(), listenerOptions, socketOptions, proxyProtocol);
        this.address = address;
        this.serverName = serverName;
        addWrapperHandler(handler -> {
            httpUpgradeHandler.setNonUpgradeHandler(handler);
            return httpUpgradeHandler;
        });
        if(listenerOptions.get(UndertowOptions.ENABLE_HTTP2, false)) {
            addWrapperHandler(Http2UpgradeHandler::new);
        }
        if (certificateForwarding) {
            addWrapperHandler(SSLHeaderHandler::new);
        }
        if (proxyAddressForwarding) {
            addWrapperHandler(ProxyPeerAddressHandler::new);
        }
    }

    @Override
    protected OpenListener createOpenListener() {
        return new HttpOpenListener(getBufferPool().get(), OptionMap.builder().addAll(commonOptions).addAll(listenerOptions).set(UndertowOptions.ENABLE_STATISTICS, getUndertowService().isStatisticsEnabled()).getMap());
    }

    @Override
    public boolean isSecure() {
        return false;
    }

    @Override
    protected void preStart(final StartContext context) {
        //adds the HTTP upgrade service
        //TODO: have a bit more of a think about how we handle this
        final ServiceBuilder<?> sb = context.getChildTarget().addService(HTTP_UPGRADE_REGISTRY_CAPABILITY.getCapabilityServiceName(address));
        final Consumer<Object> serviceConsumer = sb.provides(HTTP_UPGRADE_REGISTRY_CAPABILITY.getCapabilityServiceName(address), HTTP_UPGRADE_REGISTRY.append(getName()));
        sb.setInstance(Service.newInstance(serviceConsumer, httpUpgradeHandler));
        sb.install();
        ListenerRegistry.Listener listener = new ListenerRegistry.Listener(getProtocol(), getName(), serverName, getBinding().get().getSocketAddress());
        listener.setContextInformation("socket-binding", getBinding().get());
        httpListenerRegistry.get().addListener(listener);
    }

    protected void startListening(XnioWorker worker, InetSocketAddress socketAddress, ChannelListener<AcceptingChannel<StreamConnection>> acceptListener)
            throws IOException {
        server = worker.createStreamConnectionServer(socketAddress, acceptListener, OptionMap.builder().addAll(commonOptions).addAll(socketOptions).getMap());
        server.resumeAccepts();

        final InetSocketAddress boundAddress = server.getLocalAddress(InetSocketAddress.class);
        UndertowLogger.ROOT_LOGGER.listenerStarted("HTTP", getName(), NetworkUtils.formatIPAddressForURI(boundAddress.getAddress()), boundAddress.getPort());
    }

    @Override
    protected void cleanFailedStart() {
        httpListenerRegistry.get().removeListener(getName());
    }

    protected void unregisterBinding() {
        httpListenerRegistry.get().removeListener(getName());
        super.unregisterBinding();
    }

    @Override
    protected void stopListening() {
        final InetSocketAddress boundAddress = server.getLocalAddress(InetSocketAddress.class);
        server.suspendAccepts();
        UndertowLogger.ROOT_LOGGER.listenerSuspend("HTTP", getName());
        IoUtils.safeClose(server);
        server = null;
        UndertowLogger.ROOT_LOGGER.listenerStopped("HTTP", getName(), NetworkUtils.formatIPAddressForURI(boundAddress.getAddress()), boundAddress.getPort());
    }

    @Override
    public HttpListenerService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    public DelegatingSupplier<ListenerRegistry> getHttpListenerRegistry() {
        return httpListenerRegistry;
    }

    @Override
    public String getProtocol() {
        return PROTOCOL;
    }
}
