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
import java.util.Map;
import java.util.concurrent.TimeUnit;

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

/**
 * A service which sets up the {@link EJBClientContext} with appropriate remoting receivers and local receivers.
 * The receivers and the client context are configured in a jboss-ejb-client.xml.
 *
 * @author Jaikiran Pai
 * @author <a href="mailto:tadamski@redhat.com">Tomasz Adamski</a>
 */
public class DescriptorBasedEJBClientContextService implements Service<EJBClientContext> {

    public static final ServiceName BASE_SERVICE_NAME = ServiceName.JBOSS.append("ejb3", "dd-based-ejb-client-context");

    private static final Logger logger = Logger.getLogger(DescriptorBasedEJBClientContextService.class);

    private static final long DEFAULT_CONNECTION_TIMEOUT = 5000L;

    private final InjectedValue<RemotingProfileService> profileServiceValue=new InjectedValue<RemotingProfileService>();

    private final EJBClientConfiguration ejbClientConfiguration;
    private final ClassLoader clientContextClassloader;

    /**
     * The client context
     */
    private volatile EJBClientContext ejbClientContext;

    /**
     *
     * @param ejbClientConfiguration
     * @deprecated Use {@link #DescriptorBasedEJBClientContextService(org.jboss.ejb.client.EJBClientConfiguration, ClassLoader)} instead
     */
    @Deprecated
    public DescriptorBasedEJBClientContextService(final EJBClientConfiguration ejbClientConfiguration) {
        this.ejbClientConfiguration = ejbClientConfiguration;
        this.clientContextClassloader = null;
    }

    public DescriptorBasedEJBClientContextService(final EJBClientConfiguration ejbClientConfiguration, final ClassLoader clientContextClassloader) {
        this.ejbClientConfiguration = ejbClientConfiguration;
        this.clientContextClassloader = clientContextClassloader;
    }

    @Override
    public synchronized void start(StartContext startContext) throws StartException {
        // setup the context with the receivers
        final EJBClientContext context;
        if (this.clientContextClassloader != null) {
            context = EJBClientContext.create(this.ejbClientConfiguration, this.clientContextClassloader);
        } else {
            context = EJBClientContext.create(this.ejbClientConfiguration);
        }
        final RemotingProfileService profileService=profileServiceValue.getValue();
        // add the (optional) local EJB receiver
        final LocalEjbReceiver localEjbReceiver = profileService.getLocalEjbReceiverInjector().getOptionalValue();
        if (localEjbReceiver != null) {
            context.registerEJBReceiver(localEjbReceiver);
            logger.debugf("Added a local EJB receiver to descriptor based EJB client context named %s", startContext
                    .getController().getName());
        }
        // now process the remoting receivers
        this.registerRemotingEJBReceivers(startContext, context,profileService);
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

    public Injector<RemotingProfileService> getProfileServiceInjector() {
        return this.profileServiceValue;
    }

    private void registerRemotingEJBReceivers(final StartContext startContext, final EJBClientContext context,
            final RemotingProfileService profileService) {
        final Map<ServiceName, InjectedValue<AbstractOutboundConnectionService>> remotingOutboundConnections = profileService
                .getRemotingConnections();
        final Map<String, Long> connectionTimeouts = profileService.getConnectionTimeouts();
        final Map<String,OptionMap> channelCreationOpts=profileService.getChannelCreationOpts();
        final ServiceRegistry serviceRegistry = startContext.getController().getServiceContainer();
        int numRemotingReceivers = 0;
        for (final Map.Entry<ServiceName, InjectedValue<AbstractOutboundConnectionService>> entry : remotingOutboundConnections
                .entrySet()) {
            final InjectedValue<AbstractOutboundConnectionService> injectedValue = entry.getValue();
            final AbstractOutboundConnectionService outboundConnectionService = injectedValue.getValue();
            final String connectionName = outboundConnectionService.getConnectionName();
            logger.debugf("Creating remoting EJB receiver for connection %s", connectionName);
            final long connectionTimeout = connectionTimeouts.get(connectionName) <= 0 ? DEFAULT_CONNECTION_TIMEOUT
                    : connectionTimeouts.get(connectionName);
            final OptionMap options = channelCreationOpts.get(connectionName) == null ? OptionMap.EMPTY
                    : channelCreationOpts.get(connectionName);

            Connection connection = null;
            final ReconnectHandler reconnectHandler = new OutboundConnectionReconnectHandler(serviceRegistry, entry.getKey(),
                    context, connectionTimeout, options);
            try {
                final IoFuture<Connection> futureConnection = outboundConnectionService.connect();
                connection = IoFutureHelper.get(futureConnection, connectionTimeout, TimeUnit.MILLISECONDS);

            } catch (Exception e) {
                // just log a message and register a reconnect handler
                logger.debugf(e,
                        "Failed to create a connection for %s. A reconnect handler will be added to the client context",
                        connectionName, e);
                context.registerReconnectHandler(reconnectHandler);
                continue;
            }
            final RemotingConnectionEJBReceiver ejbReceiver = new RemotingConnectionEJBReceiver(connection, reconnectHandler,
                    options, outboundConnectionService.getProtocol());
            context.registerEJBReceiver(ejbReceiver);
            numRemotingReceivers++;
        }
        logger.debugf("Added %s remoting EJB receivers to descriptor based EJB client context %s", numRemotingReceivers,
                startContext.getController().getName());
    }

    /**
     * A {@link ReconnectHandler} which attempts a reconnection to an outbound connection using the
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
                logger.debugf("Unregistering %s since %s is no longer available", this, this.outboundConnectionServiceName);
                this.clientContext.unregisterReconnectHandler(this);
                return;
            }
            final AbstractOutboundConnectionService outboundConnectionService = (AbstractOutboundConnectionService) serviceController.getValue();
            try {
                final IoFuture<Connection> futureConnection = outboundConnectionService.connect();
                final Connection connection = IoFutureHelper.get(futureConnection, connectionTimeout, TimeUnit.MILLISECONDS);
                logger.debugf("Successful reconnect attempt#%s to outbound connection %s", this.reconnectAttemptCount,
                        this.outboundConnectionServiceName);
                // successfully reconnected so unregister this reconnect handler
                this.clientContext.unregisterReconnectHandler(this);
                // register the newly reconnected connection
                final EJBReceiver receiver = new RemotingConnectionEJBReceiver(connection, this, channelCreationOpts, outboundConnectionService.getProtocol()); //TODO: FIXME
                this.clientContext.registerEJBReceiver(receiver);
            } catch (Exception e) {
                logger.debugf(e, "Reconnect attempt#%s failed for outbound connection %s", this.reconnectAttemptCount,
                        this.outboundConnectionServiceName);
            }

        }
    }
}
