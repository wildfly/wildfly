/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.server.mgmt.domain;

import org.jboss.as.controller.ControlledProcessState;
import org.jboss.as.controller.ControlledProcessStateService;
import org.jboss.as.network.NetworkUtils;
import org.jboss.as.protocol.ProtocolConnectionConfiguration;
import org.jboss.as.protocol.StreamUtils;
import org.jboss.as.server.ServerMessages;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.remoting3.Endpoint;
import org.jboss.threads.JBossThreadFactory;
import org.xnio.OptionMap;
import org.xnio.Options;
import org.xnio.Sequence;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.net.URI;
import java.security.AccessController;
import java.security.GeneralSecurityException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Service setting up the connection to the local host controller.
 *
 * @author Emanuel Muckenhuber
 */
public class HostControllerConnectionService implements Service<HostControllerClient> {

    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("host", "controller", "client");

    private static final String JBOSS_LOCAL_USER = "JBOSS-LOCAL-USER";
    private static final long SERVER_CONNECTION_TIMEOUT = 60000;

    private final InjectedValue<Endpoint> endpointInjector = new InjectedValue<Endpoint>();
    private final InjectedValue<ControlledProcessStateService> processStateServiceInjectedValue = new InjectedValue<ControlledProcessStateService>();

    private final int port;
    private final String hostName;
    private final String serverName;
    private final String userName;
    private final String serverProcessName;
    private final byte[] initialAuthKey;
    private final ThreadFactory threadFactory = new JBossThreadFactory(new ThreadGroup("host-controller-connection-threads"), Boolean.FALSE, null, "%G - %t", null, null, AccessController.getContext());
    private final ExecutorService executor = Executors.newCachedThreadPool(threadFactory);
    private final boolean managementSubsystemEndpoint;

    private HostControllerClient client;

    public HostControllerConnectionService(final String hostName, final int port, final String serverName, final String serverProcessName, final byte[] authKey, final boolean managementSubsystemEndpoint) {
        this.port = port;
        this.hostName = hostName;
        this.serverName = serverName;
        this.userName = "=" + serverName;
        this.serverProcessName = serverProcessName;
        this.initialAuthKey = authKey;
        this.managementSubsystemEndpoint = managementSubsystemEndpoint;
    }

    @Override
    @SuppressWarnings("deprecation")
    public synchronized void start(final StartContext context) throws StartException {
        final Endpoint endpoint = endpointInjector.getValue();
        try {
            final URI connectionURI = new URI("remote://" + NetworkUtils.formatPossibleIpv6Address(hostName) + ":" + port);
            final OptionMap options = OptionMap.create(Options.SASL_DISALLOWED_MECHANISMS, Sequence.of(JBOSS_LOCAL_USER));
            // Create the connection configuration
            final ProtocolConnectionConfiguration configuration = ProtocolConnectionConfiguration.create(endpoint, connectionURI, options);
            configuration.setCallbackHandler(HostControllerConnection.createClientCallbackHandler(userName, initialAuthKey));
            configuration.setConnectionTimeout(SERVER_CONNECTION_TIMEOUT);
            configuration.setSslContext(getAcceptingSSLContext());
            // Create the connection
            final HostControllerConnection connection = new HostControllerConnection(serverProcessName, userName, configuration, executor);
            // Trigger the started notification based on the process state listener
            final ControlledProcessStateService processService = processStateServiceInjectedValue.getValue();
            processService.addPropertyChangeListener(new PropertyChangeListener() {
                @Override
                public void propertyChange(final PropertyChangeEvent evt) {
                    final ControlledProcessState.State current = (ControlledProcessState.State) evt.getNewValue();
                    if(current == ControlledProcessState.State.RUNNING) {
                        // Send the started notification
                        connection.started();
                    } else {
                        // TODO send a stopping message
                    }
                }
            });
            this.client = new HostControllerClient(serverName, connection.getChannelHandler(), connection, managementSubsystemEndpoint);
        } catch (Exception e) {
            throw new StartException(e);
        }
    }

    @Override
    public synchronized void stop(final StopContext context) {
        StreamUtils.safeClose(client);
        client = null;
    }

    @Override
    public synchronized HostControllerClient getValue() throws IllegalStateException, IllegalArgumentException {
        final HostControllerClient client = this.client;
        if(client == null) {
            throw new IllegalStateException();
        }
        return client;
    }

    public InjectedValue<ControlledProcessStateService> getProcessStateServiceInjectedValue() {
        return processStateServiceInjectedValue;
    }

    public InjectedValue<Endpoint> getEndpointInjector() {
        return endpointInjector;
    }

    private static SSLContext getAcceptingSSLContext() throws IOException {
        /*
         * This connection is only a connection back to the local host controller.
         *
         * The HostController that started this process will have already provided the
         * required information regarding the connection so quietly allow the SSL connection
         * to be established.
         */
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            TrustManager[] trustManagers = new TrustManager[] { new X509TrustManager() {
                public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
                }

                public void checkServerTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
                }

                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
            } };

            sslContext.init(null, trustManagers, null);

            return sslContext;
        } catch (GeneralSecurityException e) {
            throw ServerMessages.MESSAGES.unableToInitialiseSSLContext(e.getMessage());
        }
    }

}
