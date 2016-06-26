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

import javax.net.ssl.SSLContext;

import io.undertow.UndertowOptions;
import io.undertow.protocols.ssl.UndertowXnioSsl;
import io.undertow.server.OpenListener;
import io.undertow.server.protocol.http.AlpnOpenListener;
import io.undertow.server.protocol.http.HttpOpenListener;
import io.undertow.server.protocol.http2.Http2OpenListener;

import org.jboss.as.domain.management.SecurityRealm;
import org.jboss.as.network.NetworkUtils;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.extension.undertow.logging.UndertowLogger;
import org.wildfly.security.ssl.CipherSuiteSelector;
import org.xnio.ChannelListener;
import org.xnio.IoUtils;
import org.xnio.OptionMap;
import org.xnio.OptionMap.Builder;
import org.xnio.Option;
import org.xnio.Options;
import org.xnio.Pool;
import org.xnio.Sequence;
import org.xnio.StreamConnection;
import org.xnio.XnioWorker;
import org.xnio.channels.AcceptingChannel;
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
    private final String cipherSuites;

    public HttpsListenerService(final String name, String serverName, OptionMap listenerOptions, String cipherSuites, OptionMap socketOptions) {
        super(name, serverName, listenerOptions, socketOptions, false, false);
        this.cipherSuites = cipherSuites;
    }

    @Override
    protected OpenListener createOpenListener() {
        if(listenerOptions.get(UndertowOptions.ENABLE_HTTP2, false)) {
                return createAlpnOpenListener();
        } else {
            return super.createOpenListener();
        }
    }

    private OpenListener createAlpnOpenListener() {
        OptionMap undertowOptions = OptionMap.builder().addAll(commonOptions).addAll(listenerOptions).set(UndertowOptions.ENABLE_CONNECTOR_STATISTICS, getUndertowService().isStatisticsEnabled()).getMap();
        Pool<ByteBuffer> bufferPool = getBufferPool().getValue();
        HttpOpenListener http =  new HttpOpenListener(bufferPool, undertowOptions);
        AlpnOpenListener alpn = new AlpnOpenListener(bufferPool, undertowOptions, http);
        if(listenerOptions.get(UndertowOptions.ENABLE_HTTP2, false)) {
            Http2OpenListener http2 = new Http2OpenListener(bufferPool, undertowOptions, "h2");
            alpn.addProtocol(Http2OpenListener.HTTP2, http2, 10);
            Http2OpenListener http2_14 = new Http2OpenListener(bufferPool, undertowOptions, "h2-14");
            alpn.addProtocol(Http2OpenListener.HTTP2_14, http2_14, 9);
        }
        return alpn;
    }

    @Override
    protected void startListening(XnioWorker worker, InetSocketAddress socketAddress, ChannelListener<AcceptingChannel<StreamConnection>> acceptListener) throws IOException {

        SSLContext sslContext = securityRealm.getValue().getSSLContext();

        if(sslContext == null) {
            throw UndertowLogger.ROOT_LOGGER.noSslContextInSecurityRealm();
        }

        Builder builder = OptionMap.builder().addAll(commonOptions);
        builder.addAll(socketOptions);
        builder.set(Options.USE_DIRECT_BUFFERS, true);

        if (cipherSuites != null) {
            String[] cipherList = CipherSuiteSelector.fromString(cipherSuites).evaluate(sslContext.getSupportedSSLParameters().getCipherSuites());
            builder.setSequence((Option<Sequence<String>>) HttpsListenerResourceDefinition.ENABLED_CIPHER_SUITES.getOption(), cipherList);
        }

        OptionMap combined = builder.getMap();

        XnioSsl xnioSsl = new UndertowXnioSsl(worker.getXnio(), combined, sslContext);
        sslServer = xnioSsl.createSslConnectionServer(worker, socketAddress, (ChannelListener) acceptListener, combined);
        sslServer.resumeAccepts();

        UndertowLogger.ROOT_LOGGER.listenerStarted("HTTPS", getName(), NetworkUtils.formatIPAddressForURI(socketAddress.getAddress()), socketAddress.getPort());
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
        UndertowLogger.ROOT_LOGGER.listenerStopped("HTTPS", getName(), NetworkUtils.formatIPAddressForURI(getBinding().getValue().getSocketAddress().getAddress()), getBinding().getValue().getSocketAddress().getPort());
        httpListenerRegistry.getValue().removeListener(getName());
    }

    public InjectedValue<SecurityRealm> getSecurityRealm() {
        return securityRealm;
    }

    @Override
    protected String getProtocol() {
        return PROTOCOL;
    }
}
