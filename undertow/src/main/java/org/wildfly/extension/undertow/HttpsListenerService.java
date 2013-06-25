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

import static org.xnio.Options.SSL_CLIENT_AUTH_MODE;
import static org.xnio.SslClientAuthMode.REQUESTED;

import java.io.IOException;
import java.net.InetSocketAddress;
import javax.net.ssl.SSLContext;

import org.jboss.as.domain.management.AuthMechanism;
import org.jboss.as.domain.management.SecurityRealm;
import org.jboss.msc.value.InjectedValue;
import org.xnio.ChannelListener;
import org.xnio.IoUtils;
import org.xnio.OptionMap;
import org.xnio.OptionMap.Builder;
import org.xnio.Options;
import org.xnio.StreamConnection;
import org.xnio.XnioWorker;
import org.xnio.channels.AcceptingChannel;
import org.xnio.channels.SslConnection;
import org.xnio.ssl.JsseXnioSsl;
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

    public HttpsListenerService(final String name) {
        super(name);
    }

    @Override
    protected void startListening(XnioWorker worker, InetSocketAddress socketAddress, ChannelListener<AcceptingChannel<StreamConnection>> acceptListener) throws IOException {

        SSLContext sslContext = securityRealm.getValue().getSSLContext();
        Builder builder = OptionMap.builder().addAll(SERVER_OPTIONS);
        if (securityRealm.getValue().getSupportedAuthenticationMechanisms().contains(AuthMechanism.CLIENT_CERT)) {
            builder.set(SSL_CLIENT_AUTH_MODE, REQUESTED);
        }
        builder.set(Options.USE_DIRECT_BUFFERS,true);
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
    }

    public InjectedValue<SecurityRealm> getSecurityRealm() {
        return securityRealm;
    }

}
