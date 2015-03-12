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
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import javax.net.ssl.SSLContext;

import io.undertow.UndertowMessages;
import io.undertow.UndertowOptions;
import io.undertow.server.ConnectorStatistics;
import io.undertow.server.HttpHandler;
import io.undertow.server.OpenListener;
import io.undertow.server.protocol.http.AlpnOpenListener;
import io.undertow.server.protocol.http.HttpOpenListener;
import io.undertow.server.protocol.http2.Http2OpenListener;
import io.undertow.server.protocol.spdy.SpdyOpenListener;
import org.jboss.as.domain.management.SecurityRealm;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.extension.undertow.logging.UndertowLogger;
import org.xnio.ChannelListener;
import org.xnio.IoUtils;
import org.xnio.OptionMap;
import org.xnio.OptionMap.Builder;
import org.xnio.Options;
import org.xnio.Pool;
import org.xnio.StreamConnection;
import org.xnio.XnioWorker;
import org.xnio.channels.AcceptingChannel;
import org.xnio.ssl.JsseXnioSsl;
import org.xnio.ssl.SslConnection;
import org.xnio.ssl.XnioSsl;

/**
 * An extension of {@see HttpListenerService} to add SSL.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 * @author Tomaz Cerar
 */
public class HttpsListenerService extends HttpListenerService {

    private final InjectedValue<SecurityRealm> securityRealm = new InjectedValue<>();
    private volatile AcceptingChannel<SslConnection> sslServer;
    static final String PROTOCOL = "https";

    public HttpsListenerService(final String name, String serverName, OptionMap listenerOptions, OptionMap socketOptions) {
        super(name, serverName, listenerOptions, socketOptions, false, false);
    }

    @Override
    protected OpenListener createOpenListener() {
        if(listenerOptions.get(UndertowOptions.ENABLE_HTTP2, false) || listenerOptions.get(UndertowOptions.ENABLE_SPDY, false)) {
            try {
                getClass().getClassLoader().loadClass("org.eclipse.jetty.alpn.ALPN");
                return createAlpnOpenListener();
            } catch (ClassNotFoundException e) {
                UndertowLogger.ROOT_LOGGER.alpnNotFound();
                return super.createOpenListener();
            }
        } else {
            return super.createOpenListener();
        }
    }

    private OpenListener createAlpnOpenListener() {
        List<OpenListener> listeners = new ArrayList<>();
        OptionMap undertowOptions = OptionMap.builder().addAll(commonOptions).addAll(listenerOptions).set(UndertowOptions.ENABLE_CONNECTOR_STATISTICS, getUndertowService().isStatisticsEnabled()).getMap();
        Pool bufferPool = getBufferPool().getValue();
        HttpOpenListener http =  new HttpOpenListener(bufferPool, undertowOptions);
        listeners.add(http);
        AlpnOpenListener alpn = new AlpnOpenListener(bufferPool, http);
        if(listenerOptions.get(UndertowOptions.ENABLE_HTTP2, false)) {
            Http2OpenListener http2 = new Http2OpenListener(bufferPool, undertowOptions, "h2");
            alpn.addProtocol("h2", http2, 10);
            Http2OpenListener http2_14 = new Http2OpenListener(bufferPool, undertowOptions, "h2-14");
            alpn.addProtocol("h2-14", http2_14, 9);
            listeners.add(http2);
            listeners.add(http2_14);
        }
        if(listenerOptions.get(UndertowOptions.ENABLE_SPDY, false)) {
            //if you want to use spdy you need to configure heap buffers
            //we may fix this in future, but spdy is going away anyway
            SpdyOpenListener spdyOpenListener = new SpdyOpenListener(bufferPool, bufferPool, undertowOptions);
            alpn.addProtocol(SpdyOpenListener.SPDY_3_1, spdyOpenListener, 5);
            listeners.add(spdyOpenListener);
        }
        return new TempAlpnOpenListener(alpn, bufferPool, listeners);
    }

    @Override
    protected void startListening(XnioWorker worker, InetSocketAddress socketAddress, ChannelListener<AcceptingChannel<StreamConnection>> acceptListener) throws IOException {

        SSLContext sslContext = securityRealm.getValue().getSSLContext();
        Builder builder = OptionMap.builder().addAll(commonOptions);
        builder.addAll(socketOptions);
        builder.set(Options.USE_DIRECT_BUFFERS, true);
        OptionMap combined = builder.getMap();

        XnioSsl xnioSsl = new JsseXnioSsl(worker.getXnio(), combined, sslContext);
        sslServer = xnioSsl.createSslConnectionServer(worker, socketAddress, (ChannelListener) acceptListener, combined);
        sslServer.resumeAccepts();

        UndertowLogger.ROOT_LOGGER.listenerStarted("HTTPS", getName(), socketAddress);
    }

    @Override
    public boolean isSecure() {
        return true;
    }

    @Override
    protected void stopListening() {
        sslServer.suspendAccepts();
        UndertowLogger.ROOT_LOGGER.listenerSuspend("HTTPS", getName());
        IoUtils.safeClose(sslServer);
        sslServer = null;
        UndertowLogger.ROOT_LOGGER.listenerStopped("HTTPS", getName(), getBinding().getValue().getSocketAddress());
        httpListenerRegistry.getValue().removeListener(getName());
    }

    public InjectedValue<SecurityRealm> getSecurityRealm() {
        return securityRealm;
    }

    @Override
    protected String getProtocol() {
        return PROTOCOL;
    }

    //temporary class that can go away once we have Undertow Beta11
    private static final class TempAlpnOpenListener implements OpenListener {

        private final AlpnOpenListener alpnOpenListener;
        private final Pool<ByteBuffer> bufferPool;

        private final List<OpenListener> listeners;

        private volatile HttpHandler rootHandler;
        private volatile OptionMap undertowOptions;
        private volatile boolean statisticsEnabled;

        private TempAlpnOpenListener(AlpnOpenListener alpnOpenListener, Pool<ByteBuffer> bufferPool, List<OpenListener> listeners) {
            this.alpnOpenListener = alpnOpenListener;
            this.bufferPool = bufferPool;
            this.listeners = listeners;
        }

        @Override
        public HttpHandler getRootHandler() {
            return rootHandler;
        }

        @Override
        public void setRootHandler(HttpHandler rootHandler) {
            this.rootHandler = rootHandler;
            for(OpenListener delegate : listeners) {
                delegate.setRootHandler(rootHandler);
            }
        }

        @Override
        public OptionMap getUndertowOptions() {
            return undertowOptions;
        }

        @Override
        public void setUndertowOptions(OptionMap undertowOptions) {
            if (undertowOptions == null) {
                throw UndertowMessages.MESSAGES.argumentCannotBeNull("undertowOptions");
            }
            this.undertowOptions = undertowOptions;
            for(OpenListener delegate : listeners) {
                delegate.setRootHandler(rootHandler);
            }
            statisticsEnabled = undertowOptions.get(UndertowOptions.ENABLE_CONNECTOR_STATISTICS, false);
        }

        @Override
        public Pool<ByteBuffer> getBufferPool() {
            return bufferPool;
        }

        @Override
        public ConnectorStatistics getConnectorStatistics() {
            if(statisticsEnabled) {
                List<ConnectorStatistics> stats = new ArrayList<>();
                for(OpenListener l : listeners) {
                    ConnectorStatistics c = l.getConnectorStatistics();
                    if(c != null) {
                        stats.add(c);
                    }
                }
                return new AggregateConnectorStatistics(stats.toArray(new ConnectorStatistics[stats.size()]));
            }
            return null;
        }

        @Override
        public void handleEvent(StreamConnection channel) {
            alpnOpenListener.handleEvent(channel);
        }
    }

    //todo: remove this
    private static class AggregateConnectorStatistics implements ConnectorStatistics {

        private final ConnectorStatistics[] connectorStatistics;

        public AggregateConnectorStatistics(ConnectorStatistics[] connectorStatistics) {
            this.connectorStatistics = connectorStatistics;
        }

        @Override
        public long getRequestCount() {
            long count = 0;
            for(ConnectorStatistics c : connectorStatistics) {
                count += c.getRequestCount();
            }
            return count;
        }

        @Override
        public long getBytesSent() {
            long count = 0;
            for(ConnectorStatistics c : connectorStatistics) {
                count += c.getBytesSent();
            }
            return count;
        }

        @Override
        public long getBytesReceived() {
            long count = 0;
            for(ConnectorStatistics c : connectorStatistics) {
                count += c.getBytesReceived();
            }
            return count;
        }

        @Override
        public long getErrorCount() {
            long count = 0;
            for(ConnectorStatistics c : connectorStatistics) {
                count += c.getErrorCount();
            }
            return count;
        }

        @Override
        public long getProcessingTime() {
            long count = 0;
            for(ConnectorStatistics c : connectorStatistics) {
                count += c.getProcessingTime();
            }
            return count;
        }

        @Override
        public long getMaxProcessingTime() {
            long count = 0;
            for(ConnectorStatistics c : connectorStatistics) {
                count += c.getMaxProcessingTime();
            }
            return count;
        }

        @Override
        public void reset() {
            for(ConnectorStatistics c : connectorStatistics) {
                c.reset();
            }
        }
    }

}
