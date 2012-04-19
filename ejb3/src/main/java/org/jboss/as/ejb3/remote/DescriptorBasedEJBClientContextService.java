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

import org.jboss.as.remoting.AbstractOutboundConnectionService;
import org.jboss.ejb.client.EJBClientConfiguration;
import org.jboss.ejb.client.EJBClientContext;
import org.jboss.ejb.client.EJBReceiver;
import org.jboss.ejb.client.remoting.IoFutureHelper;
import org.jboss.ejb.client.remoting.ReconnectHandler;
import org.jboss.ejb.client.remoting.RemotingConnectionEJBReceiver;
import org.jboss.logging.Logger;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.remoting3.Connection;
import org.xnio.IoFuture;
import org.xnio.OptionMap;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * A service which sets up the {@link EJBClientContext} with appropriate remoting receivers and local receivers.
 * The receivers and the client context are configured in a jboss-ejb-client.xml.
 *
 * @author Jaikiran Pai
 */
public class DescriptorBasedEJBClientContextService implements Service<EJBClientContext> {

    public static final ServiceName BASE_SERVICE_NAME = ServiceName.JBOSS.append("ejb3", "dd-based-ejb-client-context");

    private static final Logger logger = Logger.getLogger(DescriptorBasedEJBClientContextService.class);

    private static final long DEFAULT_CONNECTION_TIMEOUT = 5000L;

    /**
     * The outbound connection references from which the remoting EJB receivers will be created
     */
    private final Map<ServiceName, InjectedValue<AbstractOutboundConnectionService>> remotingOutboundConnections = new HashMap<ServiceName, InjectedValue<AbstractOutboundConnectionService>>();

    /**
     * (optional) local EJB receiver for the EJB client context
     */
    private final InjectedValue<LocalEjbReceiver> localEjbReceiverInjectedValue = new InjectedValue<LocalEjbReceiver>();

    private final EJBClientConfiguration ejbClientConfiguration;

    private final Map<String, OptionMap> channelCreationOpts = Collections.synchronizedMap(new HashMap<String, OptionMap>());
    private final Map<String, Long> connectionTimeouts = Collections.synchronizedMap(new HashMap<String, Long>());

    /**
     * The client context
     */
    private volatile EJBClientContext ejbClientContext;

    public DescriptorBasedEJBClientContextService(final EJBClientConfiguration ejbClientConfiguration) {
        this.ejbClientConfiguration = ejbClientConfiguration;
    }

    @Override
    public synchronized void start(StartContext startContext) throws StartException {
        // setup the context with the receivers
        final EJBClientContext context = EJBClientContext.create(this.ejbClientConfiguration);
        // add the (optional) local EJB receiver
        final LocalEjbReceiver localEjbReceiver = this.localEjbReceiverInjectedValue.getOptionalValue();
        if (localEjbReceiver != null) {
            context.registerEJBReceiver(localEjbReceiver);
            logger.debug("Added a local EJB receiver to descriptor based EJB client context named " + startContext.getController().getName());
        }
        // now process the remoting receivers
        this.registerRemotingEJBReceivers(startContext, context);
        // we now have a fully configured EJB client context for use
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
        remotingOutboundConnections.put(serviceName, value);
    }

    public void setChannelCreationOptions(final String outboundConnectionName, final OptionMap options) {
        this.channelCreationOpts.put(outboundConnectionName, options);
    }

    public void setConnectionCreationTimeout(final String outboundConnectionName, final long timeoutInMillis) {
        this.connectionTimeouts.put(outboundConnectionName, timeoutInMillis);
    }

    public Injector<LocalEjbReceiver> getLocalEjbReceiverInjector() {
        return this.localEjbReceiverInjectedValue;
    }

    private void registerRemotingEJBReceivers(final StartContext startContext, final EJBClientContext context) {
        final ServiceRegistry serviceRegistry = startContext.getController().getServiceContainer();
        int numRemotingReceivers = 0;
        for (final Map.Entry<ServiceName, InjectedValue<AbstractOutboundConnectionService>> entry : this.remotingOutboundConnections.entrySet()) {
            final InjectedValue<AbstractOutboundConnectionService> injectedValue = entry.getValue();
            final AbstractOutboundConnectionService outboundConnectionService = injectedValue.getValue();
            final String connectionName = outboundConnectionService.getConnectionName();
            logger.debug("Creating remoting EJB receiver for connection " + connectionName);
            final long connectionTimeout = this.connectionTimeouts.get(connectionName) <= 0 ? DEFAULT_CONNECTION_TIMEOUT : this.connectionTimeouts.get(connectionName);
            final OptionMap options = this.channelCreationOpts.get(connectionName) == null ? OptionMap.EMPTY : this.channelCreationOpts.get(connectionName);

            Connection connection = null;
            final ReconnectHandler reconnectHandler = new OutboundConnectionReconnectHandler(serviceRegistry, entry.getKey(), context, connectionTimeout, options);
            try {
                final IoFuture<Connection> futureConnection = outboundConnectionService.connect();
                connection = IoFutureHelper.get(futureConnection, connectionTimeout, TimeUnit.MILLISECONDS);

            } catch (Exception e) {
                // just log a message and register a reconnect handler
                logger.debug("Failed to create a connection for " + connectionName + ". A reconnect handler will be added to the client context", e);
                context.registerReconnectHandler(reconnectHandler);
                continue;
            }
            final RemotingConnectionEJBReceiver ejbReceiver = new RemotingConnectionEJBReceiver(connection, reconnectHandler, options);
            context.registerEJBReceiver(ejbReceiver);
            numRemotingReceivers++;
        }
        logger.debug("Added " + numRemotingReceivers + " remoting EJB receivers to descriptor based EJB client context " + startContext.getController().getName());
    }

    /**
     * A {@link ReconnectHandler} which attempts a reconnection to a outbound connection using the
     * {@link AbstractOutboundConnectionService}
     */
    private class OutboundConnectionReconnectHandler implements ReconnectHandler {

        private final ServiceRegistry serviceRegistry;
        private final ServiceName outboundConnectionServiceName;
        private final EJBClientContext clientContext;
        private volatile int reconnectAttemptCount;
        private final long connectionTimeout;
        private final OptionMap channelCreationOpts;

        OutboundConnectionReconnectHandler(final ServiceRegistry serviceRegistry, final ServiceName outboundConnectionServiceName,
                                           final EJBClientContext clientContext, final long connectionTimeout, final OptionMap channelCreationOpts) {
            this.outboundConnectionServiceName = outboundConnectionServiceName;
            this.serviceRegistry = serviceRegistry;
            this.clientContext = clientContext;
            this.connectionTimeout = connectionTimeout;
            this.channelCreationOpts = channelCreationOpts;
        }

        @Override
        public void reconnect() throws IOException {
            this.reconnectAttemptCount++;
            final ServiceController serviceController = this.serviceRegistry.getService(this.outboundConnectionServiceName);
            if (serviceController == null) {
                // the outbound connection service is no longer available, so unregister this
                // reconnect handler from the EJB client context
                logger.debug("Unregistering " + this + " since " + this.outboundConnectionServiceName + " is no longer available");
                this.clientContext.unregisterReconnectHandler(this);
                return;
            }
            final AbstractOutboundConnectionService outboundConnectionService = (AbstractOutboundConnectionService) serviceController.getValue();
            try {
                final IoFuture<Connection> futureConnection = outboundConnectionService.connect();
                final Connection connection = IoFutureHelper.get(futureConnection, connectionTimeout, TimeUnit.MILLISECONDS);
                logger.debug("Successful reconnect attempt#" + this.reconnectAttemptCount + " to outbound connection " + this.outboundConnectionServiceName);
                // successfully reconnected so unregister this reconnect handler
                this.clientContext.unregisterReconnectHandler(this);
                // register the newly reconnected connection
                final EJBReceiver receiver = new RemotingConnectionEJBReceiver(connection, this, channelCreationOpts);
                this.clientContext.registerEJBReceiver(receiver);
            } catch (Exception e) {
                logger.debug("Reconnect attempt#" + this.reconnectAttemptCount + " failed for outbound connection " + this.outboundConnectionServiceName, e);
            }

        }
    }
}
