/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ejb3.remote;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.jboss.as.remoting.AbstractOutboundConnectionService;
import org.jboss.ejb.client.EJBClientContext;
import org.jboss.ejb.client.remoting.IoFutureHelper;
import org.jboss.logging.Logger;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.remoting3.Connection;
import org.xnio.IoFuture;

/**
 * @author Jaikiran Pai
 */
public class DescriptorBasedEJBClientContextService implements Service<EJBClientContext> {

    public static final ServiceName BASE_SERVICE_NAME = ServiceName.JBOSS.append("ejb3", "dd-based-ejb-client-context");

    private static final Logger logger = Logger.getLogger(DescriptorBasedEJBClientContextService.class);

    private static final long DEFAULT_CONNECTION_TIMEOUT = 5000L;

    /**
     * The outbound connection references from which the remoting EJB receivers will be created
     */
    private final List<InjectedValue<AbstractOutboundConnectionService>> remotingOutboundConnections = new ArrayList<InjectedValue<AbstractOutboundConnectionService>>();

    /**
     * The client context
     */
    private volatile EJBClientContext ejbClientContext;

    @Override
    public synchronized void start(StartContext startContext) throws StartException {
        final Collection<Connection> connections = this.createRemotingConnections();
        // setup the context with the receivers
        EJBClientContext context = EJBClientContext.create();
        for (final Connection conection : connections) {
            context.registerConnection(conection);
        }
        logger.debug("Descriptor based EJB client context created with " + connections.size() + " remoting EJB receivers");
        this.ejbClientContext = context;
    }

    @Override
    public synchronized void stop(StopContext context) {
        this.ejbClientContext = null;
    }

    @Override
    public EJBClientContext getValue() throws IllegalStateException, IllegalArgumentException {
        return this.ejbClientContext;
    }

    public void addRemotingConnectionDependency(final ServiceBuilder<EJBClientContext> serviceBuilder, final ServiceName serviceName) {
        final InjectedValue<AbstractOutboundConnectionService> value = new InjectedValue<AbstractOutboundConnectionService>();
        serviceBuilder.addDependency(serviceName, AbstractOutboundConnectionService.class, value);
        remotingOutboundConnections.add(value);
    }

    private Collection<Connection> createRemotingConnections() {
        final Collection<Connection> connections = new ArrayList<Connection>();

        for (final InjectedValue<AbstractOutboundConnectionService> injectedValue : this.remotingOutboundConnections) {
            final AbstractOutboundConnectionService outboundConnectionService = injectedValue.getValue();
            final String connectionName = outboundConnectionService.getConnectionName();
            logger.debug("Creating remoting EJB receiver for connection " + connectionName);
            try {
                final IoFuture<Connection> futureConnection = outboundConnectionService.connect();
                // TODO: Make the timeout configurable
                final Connection connection = IoFutureHelper.get(futureConnection, DEFAULT_CONNECTION_TIMEOUT, TimeUnit.MILLISECONDS);
                // add it to the successful connection list to be returned
                connections.add(connection);

            } catch (IOException ioe) {
                // just log a WARN and move on to the next
                logger.warn(connectionName + " connection will not be used for EJB client context due " +
                        "to error during connection creation", ioe);
            }
        }
        return connections;
    }
}
