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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import javax.net.SocketFactory;
import org.jboss.as.protocol.Connection;
import org.jboss.as.protocol.MessageHandler;
import org.jboss.as.protocol.ProtocolClient;
import static org.jboss.as.protocol.StreamUtils.safeClose;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * Service used to connect to the host controller.  Will maintain the connection for the length of the service life.
 *
 * @author John Bailey
 */
public class HostControllerConnectionService implements Service<Connection> {
    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("host", "controller", "connection");
    private final InjectedValue<InetSocketAddress> smAddress = new InjectedValue<InetSocketAddress>();

    private Connection connection;

    /** {@inheritDoc} */
    public synchronized void start(StartContext context) throws StartException {
        final ProtocolClient.Configuration configuration = new ProtocolClient.Configuration();
        configuration.setServerAddress(smAddress.getValue());
        configuration.setMessageHandler(MessageHandler.NULL);
        configuration.setSocketFactory(SocketFactory.getDefault());
        configuration.setThreadFactory(Executors.defaultThreadFactory());
        configuration.setReadExecutor(Executors.newCachedThreadPool());

        final ProtocolClient protocolClient = new ProtocolClient(configuration);
        try {
            connection = protocolClient.connect();
        } catch (IOException e) {
            throw new StartException("Failed to start remote Host Controller connection", e);
        }
    }

    /** {@inheritDoc} */
    public synchronized void stop(StopContext context) {
        safeClose(connection);
        connection = null;
    }

    /** {@inheritDoc} */
    public synchronized Connection getValue() throws IllegalStateException {
        return connection;
    }

    /**
     * Get the host controller address injector.
     *
     * @return The injector
     */
    public Injector<InetSocketAddress> getSmAddressInjector() {
        return smAddress;
    }
}
