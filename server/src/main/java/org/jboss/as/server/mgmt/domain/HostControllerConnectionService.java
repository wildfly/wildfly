/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

import static org.jboss.as.protocol.StreamUtils.safeClose;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.sasl.RealmCallback;
import javax.security.sasl.RealmChoiceCallback;

import org.jboss.as.protocol.ProtocolChannelClient;
import org.jboss.as.remoting.management.ManagementRemotingServices;
import org.jboss.as.server.ServerMessages;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.remoting3.Channel;
import org.jboss.remoting3.Connection;
import org.jboss.remoting3.Endpoint;
import org.xnio.OptionMap;

/**
 * Service used to connect to the host controller.  Will maintain the connection for the length of the service life.
 *
 * @author John Bailey
 */
public class HostControllerConnectionService implements Service<Channel> {
    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("host", "controller", "channel");
    private final InjectedValue<InetSocketAddress> hcAddressInjector = new InjectedValue<InetSocketAddress>();
    private final InjectedValue<Endpoint> endpointInjector = new InjectedValue<Endpoint>();

    private volatile Channel channel;
    private volatile ProtocolChannelClient client;
    private volatile Connection connection;

    private final String serverName;
    private final byte[] authKey;

    private HostControllerConnectionService(final String serverName, final byte[] authKey) {
        this.serverName = serverName;
        this.authKey = authKey;
    }

    public static void install(ServiceTarget serviceTarget, final ServiceName endpointName, final InetSocketAddress managementSocket, final String serverName, final byte[] authKey) {
        final HostControllerConnectionService hcConnection = new HostControllerConnectionService(serverName, authKey);
        serviceTarget.addService(HostControllerConnectionService.SERVICE_NAME, hcConnection)
                .addInjection(hcConnection.hcAddressInjector, managementSocket)
                .addDependency(endpointName, Endpoint.class, hcConnection.endpointInjector)
                .setInitialMode(ServiceController.Mode.ACTIVE)
                .install();
    }

    /** {@inheritDoc} */
    public synchronized void start(StartContext context) throws StartException {
        ProtocolChannelClient client;
        try {
            ProtocolChannelClient.Configuration configuration = new ProtocolChannelClient.Configuration();
            configuration.setEndpoint(endpointInjector.getValue());
            configuration.setUri(new URI("remote://" + hcAddressInjector.getValue().getHostName() + ":" + hcAddressInjector.getValue().getPort()));
            client = ProtocolChannelClient.create(configuration);
        } catch (Exception e) {
            throw new StartException(e);
        }

        try {
            connection = client.connectSync(new ClientCallbackHandler());
            channel = connection.openChannel(ManagementRemotingServices.SERVER_CHANNEL, OptionMap.EMPTY).get();
        } catch (IOException e) {
            throw ServerMessages.MESSAGES.failedToConnectToHC(e);
        }
    }

    /** {@inheritDoc} */
    public synchronized void stop(StopContext context) {
        safeClose(client);
        client = null;
        channel = null;
    }

    /** {@inheritDoc} */
    public synchronized Channel getValue() throws IllegalStateException {
        return channel;
    }

    private class ClientCallbackHandler implements CallbackHandler {

        public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
            for (Callback current : callbacks) {
                if (current instanceof RealmCallback) {
                    RealmCallback rcb = (RealmCallback) current;
                    String defaultText = rcb.getDefaultText();
                    rcb.setText(defaultText); // For now just use the realm suggested.
                } else if (current instanceof RealmChoiceCallback) {
                    throw new UnsupportedCallbackException(current, "Realm choice not currently supported.");
                } else if (current instanceof NameCallback) {
                    NameCallback ncb = (NameCallback) current;
                    ncb.setName(serverName);
                } else if (current instanceof PasswordCallback) {
                    PasswordCallback pcb = (PasswordCallback) current;
                    pcb.setPassword(new String(authKey).toCharArray());
                } else {
                    throw new UnsupportedCallbackException(current);
                }
            }
        }
    }

}
